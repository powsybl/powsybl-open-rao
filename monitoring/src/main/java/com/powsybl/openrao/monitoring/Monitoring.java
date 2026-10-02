/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.action.Action;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.loadflow.LoadFlowParameters;
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
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.searchtreerao.networkpool.AbstractNetworkPool;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.applyContingency;
import static com.powsybl.openrao.searchtreerao.commons.RaoUtil.computeLoadFlow;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
public class Monitoring {

    private final String loadFlowProvider;
    private final LoadFlowRunParameters loadFlowRunParameters;
    Map<PhysicalParameter, Unit> parameterToUnitMap = new HashMap<>();

    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowRunParameters = new LoadFlowRunParameters().setParameters(loadFlowParameters);
        parameterToUnitMap.put(PhysicalParameter.ANGLE, Unit.DEGREE);
        parameterToUnitMap.put(PhysicalParameter.VOLTAGE, Unit.KILOVOLT);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters, ComputationManager computationManager) {
        this(loadFlowProvider, loadFlowParameters);
        this.loadFlowRunParameters.setComputationManager(computationManager);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new Monitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public static RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider,
                                                       LoadFlowParameters loadFlowParameters,
                                                       ComputationManager computationManager,
                                                       int numberOfLoadFlowsInParallel,
                                                       MonitoringInput monitoringInput) throws OpenRaoException {
        final MonitoringResult angleMonitoringResult = new Monitoring(loadFlowProvider, loadFlowParameters, computationManager).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), angleMonitoringResult);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         LoadFlowParameters loadFlowParameters,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new Monitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
    }

    /**
     * The computation manager can be used by the caller to execute actions before and/or after running the loadflow.
     * In particular, GridCapa relies on it to inject task-id in the MDC in order to bind logs with tasks.
     */
    public static RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider,
                                                         LoadFlowParameters loadFlowParameters,
                                                         ComputationManager computationManager,
                                                         int numberOfLoadFlowsInParallel,
                                                         MonitoringInput monitoringInput) {
        final MonitoringResult voltageMonitoringResult = new Monitoring(
            loadFlowProvider,
            loadFlowParameters,
            computationManager
        ).runMonitoring(monitoringInput, numberOfLoadFlowsInParallel);
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), voltageMonitoringResult);
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
            monitoringResult.setStatusToFailure();
        }

        BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
        monitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
        return monitoringResult;
    }

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

    private MonitoringResult monitorState(State state, Crac crac, Network network, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        Unit unit = parameterToUnitMap.get(physicalParameter);
        Set<CnecResult> cnecResults = new HashSet<>();

        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", physicalParameter, state);

        if (!state.isPreventive() && !state.getInstant().equals(crac.getLastInstant())) {
            TECHNICAL_LOGS.warn(String.format(
                "State %s is not valid. Monitoring is only allowed on preventive state or curative states defined on the last curative instant %s.",
                state.getId(), crac.getLastInstant()
            ));
            return new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);
        }

        Set<Cnec> consideredCnecs = crac.getCnecs(physicalParameter, state);
        if (consideredCnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No {} Cnecs in state '{}' defined.", physicalParameter, state);
            return new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);
        }

        // Compute loadflow
        boolean lfSuccess = computeLoadFlow(network, loadFlowProvider, loadFlowRunParameters.getLoadFlowParameters());
        if (!lfSuccess) {
            String failureReason = String.format("Load-flow computation failed at state %s. Skipping this state.", state);
            return makeFailedMonitoringResultForStateWithNaNCnecRsults(crac, physicalParameter, state, failureReason);
        }

        // Get overloaded CNECs
        Set<Cnec> overloadedCnecs = consideredCnecs.stream().filter(cnec -> cnec.computeMargin(network, unit) >= 0).collect(Collectors.toSet());

        Set<NetworkAction> networkActionsToApply = new HashSet<>();

        if (state.isPreventive()) {
            // CNECs in preventive can only be reported as overloaded, overload cannot be solved.
            if (!overloadedCnecs.isEmpty()) {
                BUSINESS_WARNS.warn("{} {} Cnec are constrained in preventive state but it cannot be secured.", overloadedCnecs.size(), physicalParameter);
            }
        } else {
            // Get all network actions associated with overloaded CNECs that can be used to solve the overload
            overloadedCnecs.forEach(cnec -> {
                networkActionsToApply.addAll(getValidNetworkActionsAssociatedToCnec(network, crac, cnec, physicalParameter, scalableZonalData));
            });

            if (!networkActionsToApply.isEmpty()) {
                // Apply all the actions on the network
                networkActionsToApply.forEach(networkAction -> networkAction.apply(network));

                // Re-balance the network after if injection actions were applied
                // TODO: keep this condition to match old code but it seems problematic why wouldn't we rebalance the network after an injection network action in voltage monitoring ?
                if (physicalParameter.equals(PhysicalParameter.ANGLE)) {
                    redispatchNetworkActions(network, networkActionsToApply, scalableZonalData);
                }

                // recompute load flow
                lfSuccess = computeLoadFlow(network, loadFlowProvider, loadFlowRunParameters.getLoadFlowParameters());
                if (!lfSuccess) {
                    String failureReason = String.format("Load-flow computation failed at state %s after applying RAs. Skipping this state.", state);
                    return makeFailedMonitoringResultForState(physicalParameter, state, failureReason, cnecResults);
                }
            }
        }

        // Evaluate all the voltage/angle CNECs
        consideredCnecs.forEach(cnec ->
            cnecResults.add(new CnecResult(cnec, unit, cnec.computeValue(network, unit), cnec.computeMargin(network, unit), cnec.computeSecurityStatus(network, unit)))
        );

        // Combine all CnecResult into a MonitoringResult
        Cnec.SecurityStatus monitoringResultStatus = Cnec.SecurityStatus.SECURE;
        if (cnecResults.stream().anyMatch(cnecResult -> cnecResult.getMargin() < 0)) {
            monitoringResultStatus = MonitoringResult.combineStatuses(
                cnecResults.stream()
                    .map(CnecResult::getCnecSecurityStatus)
                    .toArray(Cnec.SecurityStatus[]::new));
        }

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

    private void redispatchNetworkActions(Network network, Set<NetworkAction> networkActionsToApply, ZonalData<Scalable> scalableZonalData) {
        // Get power to be redispatched and network elements to be excluded
        EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
        Set<String> networkElementsToBeExcluded = new HashSet<>();
        networkActionsToApply.forEach(networkAction ->
            networkAction.getElementaryActions().forEach(ea -> storeEnergyToRedispatchAndNetworkElementsToExclude(ea, network, networkElementsToBeExcluded, powerToBeRedispatched)
            ));

        // Apply redispatch actions per country
        powerToBeRedispatched.forEach((country, powerToRedispatch) -> {
            BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", powerToRedispatch, country);
            // Necessarily exist because we check beforehand if the country is in the GLSK
            List<Scalable> countryScalables = scalableZonalData.getDataPerZone().entrySet().stream().filter(entry -> country.equals(new CountryEICode(entry.getKey()).getCountry()))
                    .map(Map.Entry::getValue).toList();
            if (countryScalables.size() > 1) {
                throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryScalables.size(), country));
            }
            new RedispatchAction(powerToRedispatch, networkElementsToBeExcluded, countryScalables.get(0)).apply(network);
            BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", powerToRedispatch, country);
        });
    }

    //TODO: put in common in raoUtil after raoResult refactoring
    private void applyOptimalRemedialActions(State state, Network network, RaoResult raoResult) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(networkAction -> networkAction.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(rangeAction -> rangeAction.apply(network, raoResult.getOptimizedSetPointOnState(state, rangeAction)));
    }

    private Set<NetworkAction> getValidNetworkActionsAssociatedToCnec(Network network, Crac crac, Cnec cnec, PhysicalParameter physicalParameter, ZonalData<Scalable> scalableZonalData) {
        Set<NetworkAction> availableNetworkActions =
            crac.getNetworkActions().stream()
                .filter(networkAction ->
                    networkAction.getUsageRules().stream()
                        .filter(OnConstraint.class::isInstance)
                        .map(OnConstraint.class::cast)
                        .anyMatch(onConstraint -> onConstraint.getCnec().equals(cnec)))
                .collect(Collectors.toSet());

        if (physicalParameter.equals(PhysicalParameter.ANGLE)) {
            if (!availableNetworkActions.isEmpty()) {
                Set<Country> glskCountries = getCountriesFromGlsk(scalableZonalData);
                // only keep network actions that only have elementary actions that are injection action
                availableNetworkActions.stream().filter(networkAction -> networkAction.getElementaryActions().stream()
                    .allMatch(ea -> isValidInjectionAction(ea, network, networkAction.getId(), glskCountries)));
            }
        }

        return availableNetworkActions;
    }

    private void storeEnergyToRedispatchAndNetworkElementsToExclude(Action ea,
                                         Network network,
                                         Set<String> networkElementsToBeExcluded,
                                         Map<Country, Double> powerToBeRedispatched) {

        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);
        Country country = ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation().get().getCountry().get();

        if (ne.getType().equals(IdentifiableType.GENERATOR)) {
            powerToBeRedispatched.merge(
                country,
                ((Generator) ne).getTargetP() - ((GeneratorAction) ea).getActivePowerValue().getAsDouble(),
                Double::sum
            );
            networkElementsToBeExcluded.add(ne.getId());
        } else if (ne.getType().equals(IdentifiableType.LOAD)) {
            powerToBeRedispatched.merge(
                country,
                -((Load) ne).getP0() + ((LoadAction) ea).getActivePowerValue().getAsDouble(),
                Double::sum
            );
            networkElementsToBeExcluded.add(ne.getId());
        }
    }

    private boolean isValidInjectionAction(Action ea,
                                           Network network,
                                           String naId,
                                           Set<Country> glskCountries) {

        if (!(ea instanceof LoadAction) && !(ea instanceof GeneratorAction)) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that's not an injection setpoint.", naId);
            return false;
        }

        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);
        Optional<Substation> substation = ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation();

        if (substation.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that doesn't have a substation.", naId);
            return false;
        }

        Optional<Country> country = substation.get().getCountry();
        if (country.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action that doesn't have a country.", naId);
            return false;
        }

        if (!glskCountries.contains(country.get())) {
            BUSINESS_WARNS.warn("Remedial action {} is ignored : it has an elementary action on a country that's not defined in the GLSK.", naId);
            return false;
        }

        return true;
    }

    private Identifiable<?> getInjectionSetpointIdentifiable(Action ea, Network network) {
        if (ea instanceof GeneratorAction generatorAction) {
            return network.getIdentifiable(generatorAction.getGeneratorId());
        }
        if (ea instanceof LoadAction loadAction) {
            return network.getIdentifiable(loadAction.getLoadId());
        }
        return null;
    }

    private static Set<Country> getCountriesFromGlsk(ZonalData<Scalable> scalableZonalData) {
        Set<Country> glskCountries = new TreeSet<>(Comparator.comparing(Country::getName));
        if (Objects.isNull(scalableZonalData)) {
            String error = "ScalableZonalData undefined (no GLSK given)";
            BUSINESS_LOGS.error(error);
            throw new OpenRaoException(error);
        }
        for (String zone : scalableZonalData.getDataPerZone().keySet()) {
            glskCountries.add(new CountryEICode(zone).getCountry());
        }
        return glskCountries;
    }

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
