/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.remedialaction.AppliedNetworkActionsResult;
import com.powsybl.openrao.monitoring.remedialaction.RedispatchAction;
import com.powsybl.openrao.monitoring.results.*;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public abstract class AbstractMonitoring<I extends Cnec<?>> implements Monitoring<I> {

    private final String loadFlowProvider;
    private final LoadFlowParameters loadFlowParameters;
    private final PhysicalParameter physicalParameter;
    private final Unit unit;

    protected AbstractMonitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, PhysicalParameter physicalParameter, Unit unit) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowParameters = loadFlowParameters;
        this.physicalParameter = physicalParameter;
        this.unit = unit;
    }

    public MonitoringResult<I> runMonitoring(MonitoringInput monitoringInput, int numberOfLoadFlowsInParallel) {
        checkInputs(monitoringInput);
        Network inputNetwork = monitoringInput.network();
        Crac crac = monitoringInput.crac();
        RaoResult raoResult = monitoringInput.raoResult();

        MonitoringResult<I> monitoringResult = makeEmptySecureResult();

        BUSINESS_LOGS.info("----- {} monitoring [start]", physicalParameter);
        Set<I> cnecs = getCnecs(crac);
        if (cnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No Cnecs of type '{}' defined.", physicalParameter);
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        // I) Preventive state
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
            Set<I> preventiveStateCnecs = getCnecs(crac, crac.getPreventiveState());
            MonitoringResult<I> preventiveStateMonitoringResult = monitorCnecs(preventiveState, preventiveStateCnecs, inputNetwork, monitoringInput);
            preventiveStateMonitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
            monitoringResult.combine(preventiveStateMonitoringResult);
        }

        // II) Curative states
        Set<State> contingencyStates = crac.getCnecs(physicalParameter).stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        if (contingencyStates.isEmpty()) {
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), Math.min(numberOfLoadFlowsInParallel, contingencyStates.size()), true)) {
            List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                networkPool.submit(() -> {
                    Network networkClone = networkPool.getAvailableNetwork();

                    Contingency contingency = state.getContingency().orElseThrow();
                    if (!contingency.isValid(networkClone)) {
                        monitoringResult.combine(makeFailedMonitoringResultForStateWithNaNCnecResults(monitoringInput, state, "Unable to apply contingency " + contingency.getId()));
                        networkPool.releaseUsedNetwork(networkClone);
                        return null;
                    }
                    contingency.toModification().apply(networkClone, (ComputationManager) null);
                    applyOptimalRemedialActionsOnContingencyState(state, networkClone, crac, raoResult);
                    Set<I> currentStateCnecs = getCnecs(crac, state);
                    MonitoringResult<I> currentStateMonitoringResult = monitorCnecs(state, currentStateCnecs, networkClone, monitoringInput);
                    currentStateMonitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
                    monitoringResult.combine(currentStateMonitoringResult);
                    networkPool.releaseUsedNetwork(networkClone);
                    return null;
                })).toList();

            for (ForkJoinTask<Object> task : tasks) {
                try {
                    task.get();
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
            networkPool.shutdownAndAwaitTermination(24, TimeUnit.HOURS);
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            monitoringResult.setStatusToFailure();
        }

        BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
        monitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
        return monitoringResult;
    }

    protected abstract void checkInputs(MonitoringInput monitoringInput);

    protected abstract MonitoringResult<I> makeEmptySecureResult();

    protected abstract Set<I> getCnecs(Crac crac);

    protected Set<I> getCnecs(Crac crac, State state) {
        return getCnecs(crac).stream().filter(cnec -> state.equals(cnec.getState())).collect(Collectors.toSet());
    }

    private MonitoringResult<I> monitorCnecs(State state, Set<I> cnecs, Network network, MonitoringInput monitoringInput) {
        Set<CnecResult<I>> cnecResults = new HashSet<>();
        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", physicalParameter, state);
        boolean lfSuccess = computeLoadFlow(network);
        if (!lfSuccess) {
            String failureReason = String.format("Load-flow computation failed at state %s. Skipping this state.", state);
            return makeFailedMonitoringResultForStateWithNaNCnecResults(monitoringInput, state, failureReason);
        }
        List<AppliedNetworkActionsResult> appliedNetworkActionsResultList = new ArrayList<>();
        cnecs.forEach(cnec -> {
            CnecResult<I> cnecResult = computeCnecResult(cnec, network, unit);
            if (cnecResult.getMargin() < 0) {
                // For CNEC with violation, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getNetworkActionsAssociatedToCnec(state, monitoringInput.crac(), cnec, physicalParameter);
                // if there are any available remedial actions, apply them
                if (!availableNetworkActions.isEmpty()) {
                    AppliedNetworkActionsResult appliedNetworkActionsResult = applyNetworkActions(network, availableNetworkActions, cnec.getId(), monitoringInput);
                    if (!appliedNetworkActionsResult.getAppliedNetworkActions().isEmpty()) {
                        appliedNetworkActionsResultList.add(appliedNetworkActionsResult);
                    }
                }
            }
            cnecResults.add(cnecResult);
        });

        redispatchNetworkActions(network, appliedNetworkActionsResultList, monitoringInput.scalableZonalData());

        // If some action were applied, recompute a loadflow
        if (appliedNetworkActionsResultList.stream().map(AppliedNetworkActionsResult::getAppliedNetworkActions).findAny().isPresent()) {
            lfSuccess = computeLoadFlow(network);
            if (!lfSuccess) {
                String failureReason = String.format("Load-flow computation failed at state %s after applying RAs. Skipping this state.", state);
                return makeFailedMonitoringResultForState(state, failureReason, cnecResults);
            }
            // Re-compute all voltage/angle values
            cnecResults.clear();
            cnecs.forEach(cnec -> cnecResults.add(computeCnecResult(cnec, network, unit)));
        }

        SecurityStatus monitoringResultStatus = SecurityStatus.SECURE;
        if (cnecResults.stream().anyMatch(cnecResult -> cnecResult.getMargin() < 0)) {
            monitoringResultStatus = MonitoringResult.combineStatuses(
                cnecResults.stream()
                    .map(CnecResult::getCnecSecurityStatus)
                    .toArray(SecurityStatus[]::new));
        }

        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [end]", physicalParameter, state);
        return makeMonitoringResult(cnecResults, Map.of(state, appliedNetworkActionsResultList.stream().flatMap(r -> r.getAppliedNetworkActions().stream()).collect(Collectors.toSet())), monitoringResultStatus);
    }

    private void redispatchNetworkActions(Network network, List<AppliedNetworkActionsResult> appliedNetworkActionsResults, ZonalData<Scalable> scalableZonalData) {
        // Apply one redispatch action per country
        appliedNetworkActionsResults.forEach(appliedNetworkActionsResult ->
            appliedNetworkActionsResult.getPowerToBeRedispatched().forEach((key, value) -> {
                BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", value, key);
                List<Scalable> countryScalables = scalableZonalData.getDataPerZone().entrySet().stream().filter(entry -> key.equals(new CountryEICode(entry.getKey()).getCountry()))
                    .map(Map.Entry::getValue).toList();
                if (countryScalables.size() > 1) {
                    throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryScalables.size(), key.getName()));
                }
                new RedispatchAction(value, appliedNetworkActionsResult.getNetworkElementsToBeExcluded(), countryScalables.get(0)).apply(network);
                BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", value, key);
            }));
    }

    /**
     * Gathers optimal remedial actions retrieved from raoResult for a given state on network.
     * For curative states, consider auto (when they exist) and curative states.
     */
    private void applyOptimalRemedialActionsOnContingencyState(State state, Network network, Crac crac, RaoResult raoResult) {
        if (state.getInstant().isCurative()) {
            Optional<Contingency> contingency = state.getContingency();
            crac.getStates(contingency.orElseThrow()).forEach(contingencyState ->
                applyOptimalRemedialActions(contingencyState, network, raoResult));
        } else {
            applyOptimalRemedialActions(state, network, raoResult);
        }
    }

    /**
     * Applies optimal remedial actions retrieved from raoResult for a given state on network.
     */
    private void applyOptimalRemedialActions(State state, Network network, RaoResult raoResult) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(na -> na.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(ra -> ra.apply(network, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

    /**
     * Runs a LoadFlow computation
     * Returns false if loadFlow has not converged.
     */
    private boolean computeLoadFlow(Network network) {
        TECHNICAL_LOGS.info("Load-flow computation [start]");
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
            .run(network, loadFlowParameters);
        if (loadFlowResult.isFailed()) {
            BUSINESS_WARNS.warn("LoadFlow error.");
        }
        TECHNICAL_LOGS.info("Load-flow computation [end]");
        return loadFlowResult.isFullyConverged();
    }

    private Set<NetworkAction> getNetworkActionsAssociatedToCnec(State state, Crac crac, I cnec, PhysicalParameter physicalParameter) {
        Set<NetworkAction> availableNetworkActions = crac.getNetworkActions()
            .stream()
            .filter(networkAction -> networkAction.getUsageRules()
                .stream()
                .filter(OnConstraint.class::isInstance)
                .map(OnConstraint.class::cast)
                .anyMatch(onConstraint -> onConstraint.getCnec().equals(cnec))
            )
            .collect(Collectors.toSet());
        if (availableNetworkActions.isEmpty()) {
            BUSINESS_WARNS.warn("{} Cnec {} in state {} has no associated RA. {} constraint cannot be secured.", physicalParameter, cnec.getId(), state.getId(), physicalParameter);
            return Collections.emptySet();
        }
        if (state.isPreventive()) {
            BUSINESS_WARNS.warn("{} Cnec {} is constrained in preventive state, it cannot be secured.", physicalParameter, cnec.getId());
            return Collections.emptySet();
        }
        return availableNetworkActions;
    }

    protected abstract AppliedNetworkActionsResult applyNetworkActions(Network network, Set<NetworkAction> availableNetworkActions, String cnecId, MonitoringInput monitoringInput);

    private MonitoringResult<I> makeFailedMonitoringResultForStateWithNaNCnecResults(MonitoringInput monitoringInput, State state, String failureReason) {
        Set<CnecResult<I>> cnecResults = new HashSet<>();
        getCnecs(monitoringInput.crac(), state).forEach(cnec -> cnecResults.add(makeFailedCnecResult(cnec)));
        return makeFailedMonitoringResultForState(state, failureReason, cnecResults);
    }

    protected abstract MonitoringResult<I> makeMonitoringResult(Set<CnecResult<I>> cnecResults, Map<State, Set<NetworkAction>> appliedRemedialActions, SecurityStatus monitoringResultStatus);

    protected abstract MonitoringResult<I> makeFailedMonitoringResultForState(State state, String failureReason, Set<CnecResult<I>> cnecResults);

    protected abstract CnecResult<I> computeCnecResult(I cnec, Network network, Unit unit);

    protected abstract CnecResult<I> makeFailedCnecResult(I cnec);
}
