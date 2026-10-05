/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.CnecValue;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyContingency;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 *
 * This class contains everything that is common between voltage and angle monitoring.
 */
public abstract class AbstractMonitoring implements Monitoring {

    private final String loadFlowProvider;
    private final LoadFlowRunParameters loadFlowRunParameters;
    Map<PhysicalParameter, Unit> parameterToUnitMap = new EnumMap<>(PhysicalParameter.class);

    public AbstractMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowRunParameters = new LoadFlowRunParameters().setParameters(loadFlowParameters);
        parameterToUnitMap.put(PhysicalParameter.ANGLE, Unit.DEGREE);
        parameterToUnitMap.put(PhysicalParameter.VOLTAGE, Unit.KILOVOLT);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public AbstractMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, ComputationManager computationManager) {
        this(loadFlowProvider, loadFlowParameters);
        this.loadFlowRunParameters.setComputationManager(computationManager);
    }

    /**
     * Executes the monitoring process for a given {@link MonitoringInput} to evaluate the security status
     * of a power network for preventive and curative states. It monitors the network constraints and
     * applies remedial actions to optimize system performance under various conditions.
     *
     * @param monitoringInput The input data required for monitoring, including network, physical parameters,
     *                        results, and zonal data.
     * @param numberOfLoadFlowsInParallel The maximum number of load flow computations to be performed in parallel
     *                                    for curative state evaluations.
     * @return The results of the monitoring process encapsulated in a {@link MonitoringResult} object
     */
    public MonitoringResult runMonitoring(MonitoringInput monitoringInput, int numberOfLoadFlowsInParallel) {
        // Initialize variables
        PhysicalParameter physicalParameter = monitoringInput.getPhysicalParameter();
        Network inputNetwork = monitoringInput.getNetwork();
        Crac crac = monitoringInput.getCrac();
        RaoResult raoResult = monitoringInput.getRaoResult();
        ZonalData<Scalable> scalableZonalData = monitoringInput.getScalableZonalData();

        // Create an empty monitoring result
        MonitoringResult monitoringResult = new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);

        BUSINESS_LOGS.info("----- {} monitoring [start]", physicalParameter);

        Set<Cnec> cnecs = crac.getCnecs(physicalParameter);
        if (cnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No Cnecs of type '{}' defined.", physicalParameter);
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        // I) Monitor preventive state
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
            MonitoringResult preventiveStateMonitoringResult = monitorState(preventiveState, crac, inputNetwork, physicalParameter, scalableZonalData);
            preventiveStateMonitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
            monitoringResult.combine(preventiveStateMonitoringResult);
        }

        // II) Monitor curative states
        Set<State> contingencyStates = crac.getCnecs(physicalParameter).stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());

        if (contingencyStates.isEmpty()) {
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(
            inputNetwork,
            inputNetwork.getVariantManager().getWorkingVariantId(),
            Math.min(numberOfLoadFlowsInParallel, contingencyStates.size()),
            true
        )) {
            contingencyStates.stream()
                .map(state -> networkPool.submit(() ->
                    optimizeOneContingencyState(networkPool, state, crac, physicalParameter, scalableZonalData, raoResult)))
                .forEach(task -> {
                    try {
                        monitoringResult.combine(task.get());
                    } catch (InterruptedException | ExecutionException e) {
                        Thread.currentThread().interrupt();
                        throw new OpenRaoException(e);
                    }
                });

            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException | OpenRaoException e) {
            Thread.currentThread().interrupt();
            BUSINESS_LOGS.error(e.getMessage());
            monitoringResult.setStatusToFailure();
        }

        BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
        monitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
        return monitoringResult;
    }

    /**
     * Optimizes one contingency state by applying the contingency to a cloned network,
     * applying optimal remedial actions, and monitoring the state to return the monitoring results.
     * The cloned network is sourced from a pool of available networks and is released back to the
     * pool once processing is complete.
     */
    private MonitoringResult optimizeOneContingencyState(AbstractNetworkPool networkPool,
                                                         State state,
                                                         Crac crac,
                                                         PhysicalParameter physicalParameter,
                                                         ZonalData<Scalable> scalableZonalData,
                                                         RaoResult raoResult) throws InterruptedException {

        Network networkClone = networkPool.getAvailableNetwork();
        try {
            // Apply contingency
            if (!applyContingency(networkClone, state, false)) {
                return makeFailedMonitoringResultForStateWithNaNCnecRsults(
                    crac,
                    physicalParameter,
                    state,
                    "Unable to apply contingency " + state.getContingency().orElseThrow().getId()
                );
            }

            applyOptimalRemedialActions(state, networkClone, raoResult);
            MonitoringResult currentStateMonitoringResult = monitorState(
                state, crac, networkClone, physicalParameter, scalableZonalData
            );

            currentStateMonitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
            return currentStateMonitoringResult;
        } finally {
            networkPool.releaseUsedNetwork(networkClone);
        }
    }

    /**
     * Monitors the given state. This involves evaluating the load flow, analyzing the CNECs, detecting overloads,
     * applying network actions if necessary (if the state is curative), and generating a monitoring result that summarizes the
     * findings.
     */
    private MonitoringResult monitorState(State state, Crac crac, Network network, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        Unit unit = parameterToUnitMap.get(physicalParameter);
        Set<CnecResult> cnecResults = new HashSet<>();

        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", physicalParameter, state);

        // Check state validity
        if (!(state.isPreventive() || state.getInstant().equals(crac.getLastInstant()))) {
            TECHNICAL_LOGS.warn(String.format(
                "State %s is not valid. Monitoring is only allowed on preventive state or curative states defined on the last curative instant %s.",
                state.getId(), crac.getLastInstant()
            ));
            return new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);
        }

        Set<Cnec> consideredCnecs = crac.getCnecs(physicalParameter, state);
        if (consideredCnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No {} CNECs in state '{}' defined.", physicalParameter, state);
            return new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);
        }

        // Compute loadflow
        boolean lfSuccess = computeLoadFlow(network, loadFlowProvider, loadFlowRunParameters);
        if (!lfSuccess) {
            String failureReason = String.format("Load flow computation failed at state %s. Skipping this state.", state);
            return makeFailedMonitoringResultForStateWithNaNCnecRsults(crac, physicalParameter, state, failureReason);
        }

        //

        // Get overloaded CNECs
        Set<Cnec> overloadedCnecs = consideredCnecs.stream().filter(cnec -> cnec.computeMargin(network, unit) < 0).collect(Collectors.toSet());

        Set<NetworkAction> networkActionsToApply = new HashSet<>();

        if (state.isPreventive()) {
            if (!overloadedCnecs.isEmpty()) {
                BUSINESS_WARNS.warn("{} {} CNEC(s) are constrained in preventive state but it cannot be secured.", overloadedCnecs.size(), physicalParameter);
            }
        } else {
            MonitoringResult curativeResult = handleCurativeState(
                state, crac, network, physicalParameter, scalableZonalData,
                overloadedCnecs, networkActionsToApply, cnecResults
            );
            if (curativeResult != null) {
                return curativeResult;
            }
        }

        // Evaluate all the voltage/angle CNECs
        consideredCnecs.forEach(cnec ->
            cnecResults.add(new CnecResult(cnec, unit, cnec.computeValue(network, unit), cnec.computeMargin(network, unit), cnec.computeSecurityStatus(network, unit)))
        );

        // Combine all CnecResult into a MonitoringResult
        Cnec.SecurityStatus monitoringResultStatus = computeMonitoringResultStatus(cnecResults);

        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [end]", physicalParameter, state);
        return new MonitoringResult(physicalParameter,
            cnecResults,
            Map.of(
                state,
                networkActionsToApply.stream()
                    .map(RemedialAction.class::cast)
                    .collect(Collectors.toSet())
            ),
            monitoringResultStatus);
    }

    private MonitoringResult handleCurativeState(State state, Crac crac, Network network,
                                                 PhysicalParameter physicalParameter,
                                                 ZonalData<Scalable> scalableZonalData,
                                                 Set<Cnec> overloadedCnecs,
                                                 Set<NetworkAction> networkActionsToApply,
                                                 Set<CnecResult> cnecResults) {
        // Get all network actions associated with overloaded CNECs that can be used to solve the overload
        overloadedCnecs.forEach(cnec -> {
            networkActionsToApply.addAll(getValidNetworkActionsAssociatedToCnec(network, crac, cnec, physicalParameter, scalableZonalData));
        });

        if (!networkActionsToApply.isEmpty()) {
            // Re-balance the network if injection actions are going to be applied
            // TODO: keep this condition to match old code but it seems problematic why wouldn't we rebalance the network after an injection network action in voltage monitoring ?
            rebalanceNetwork(network, networkActionsToApply, scalableZonalData);

            // Apply all the actions on the network
            networkActionsToApply.forEach(networkAction -> networkAction.apply(network));

            // recompute load flow
            boolean lfSuccess = computeLoadFlow(network, loadFlowProvider, loadFlowRunParameters);
            if (!lfSuccess) {
                String failureReason = String.format("Load-flow computation failed at state %s after applying RAs. Skipping this state.", state);
                return makeFailedMonitoringResultForState(physicalParameter, state, failureReason, cnecResults);
            }
            return null;
        }
        return null;
    }

    private Cnec.SecurityStatus computeMonitoringResultStatus(Set<CnecResult> cnecResults) {
        if (cnecResults.stream().anyMatch(cnecResult -> cnecResult.getMargin() < 0)) {
            return MonitoringResult.combineStatuses(
                cnecResults.stream()
                    .map(CnecResult::getCnecSecurityStatus)
                    .toArray(Cnec.SecurityStatus[]::new));
        }
        return Cnec.SecurityStatus.SECURE;
    }

    //TODO: put in common with applyRemedialActions function in raoUtil after result refactoring
    private void applyOptimalRemedialActions(State state, Network network, RaoResult raoResult) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(networkAction -> networkAction.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(rangeAction -> rangeAction.apply(network, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
    }

    // Need loadFlowRunParameters specifically to be able to change load flow computationManager
    public static boolean computeLoadFlow(Network network, String loadFlowProvider, LoadFlowRunParameters loadFlowRunParameters) {
        TECHNICAL_LOGS.info("Load flow computation [start]");
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider).run(network, loadFlowRunParameters);
        if (loadFlowResult.isFailed()) {
            BUSINESS_WARNS.warn("Load flow error.");
        }
        TECHNICAL_LOGS.info("Load flow computation [end]");
        return loadFlowResult.isFullyConverged();
    }

    // Functions to override

    protected abstract Set<NetworkAction> getValidNetworkActionsAssociatedToCnec(Network network, Crac crac, Cnec cnec, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData);

    protected abstract void rebalanceNetwork(Network network,
                                             Set<NetworkAction> networkActionsToApply,
                                             ZonalData<Scalable> scalableZonalData);

    private MonitoringResult makeFailedMonitoringResultForStateWithNaNCnecRsults(Crac crac, PhysicalParameter physicalParameter, State state, String failureReason) {
        Set<CnecResult> cnecResults = new HashSet<>();
        CnecValue cnecValue = physicalParameter.equals(PhysicalParameter.ANGLE) ? new AngleCnecValue(Double.NaN) : new VoltageCnecValue(Double.NaN, Double.NaN);
        crac.getCnecs(state).stream()
            .filter(cnec -> cnec.getPhysicalParameter() == physicalParameter)
            .forEach(cnec -> cnecResults.add(new CnecResult(cnec, parameterToUnitMap.get(physicalParameter), cnecValue, Double.NaN, Cnec.SecurityStatus.FAILURE)));
        return makeFailedMonitoringResultForState(physicalParameter, state, failureReason, cnecResults);
    }

    private MonitoringResult makeFailedMonitoringResultForState(PhysicalParameter physicalParameter, State state, String failureReason, Set<CnecResult> cnecResults) {
        BUSINESS_WARNS.warn(failureReason);
        return new MonitoringResult(physicalParameter, cnecResults, Map.of(state, Collections.emptySet()), Cnec.SecurityStatus.FAILURE);
    }

}
