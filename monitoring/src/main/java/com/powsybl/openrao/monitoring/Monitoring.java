/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring;

import com.powsybl.action.Action;
import com.powsybl.action.DanglingLineAction;
import com.powsybl.action.GeneratorAction;
import com.powsybl.action.LoadAction;
import com.powsybl.action.ShuntCompensatorPositionAction;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
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
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;
import com.powsybl.openrao.data.crac.impl.AngleCnecValue;
import com.powsybl.openrao.data.crac.impl.VoltageCnecValue;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.RaoResultWithAngleMonitoring;
import com.powsybl.openrao.monitoring.results.RaoResultWithVoltageMonitoring;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.ArrayList;
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
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

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

    public MonitoringResult runMonitoring(MonitoringInput monitoringInput, int numberOfLoadFlowsInParallel) {
        PhysicalParameter physicalParameter = monitoringInput.getPhysicalParameter();
        Network inputNetwork = monitoringInput.getNetwork();
        Crac crac = monitoringInput.getCrac();
        RaoResult raoResult = monitoringInput.getRaoResult();

        MonitoringResult monitoringResult = new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), Cnec.SecurityStatus.SECURE);

        BUSINESS_LOGS.info("----- {} monitoring [start]", physicalParameter);
        Set<Cnec> cnecs = crac.getCnecs(physicalParameter);
        if (cnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No Cnecs of type '{}' defined.", physicalParameter);
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        // I) Preventive state
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
            Set<Cnec> preventiveStateCnecs = crac.getCnecs(physicalParameter, preventiveState);
            MonitoringResult preventiveStateMonitoringResult = monitorCnecs(preventiveState, preventiveStateCnecs, inputNetwork, monitoringInput);
            preventiveStateMonitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
            monitoringResult.combine(preventiveStateMonitoringResult);
        }

        // II) Curative states
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
            List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                networkPool.submit(() -> {
                    Network networkClone = networkPool.getAvailableNetwork();

                    Contingency contingency = state.getContingency().orElseThrow();
                    if (!contingency.isValid(networkClone)) {
                        monitoringResult.combine(makeFailedMonitoringResultForStateWithNaNCnecRsults(monitoringInput, physicalParameter, state, "Unable to apply contingency " + contingency.getId()));
                        networkPool.releaseUsedNetwork(networkClone);
                        return null;
                    }
                    contingency.toModification().apply(networkClone, (ComputationManager) null);
                    applyOptimalRemedialActionsOnContingencyState(state, networkClone, crac, raoResult);
                    Set<Cnec> currentStateCnecs = crac.getCnecs(physicalParameter, state);
                    MonitoringResult currentStateMonitoringResult = monitorCnecs(state, currentStateCnecs, networkClone, monitoringInput);
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
        } catch (InterruptedException | OpenRaoException e) {
            Thread.currentThread().interrupt();
            monitoringResult.setStatusToFailure();
        }

        BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
        monitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
        return monitoringResult;
    }

    private MonitoringResult monitorCnecs(State state, Set<Cnec> cnecs, Network network, MonitoringInput monitoringInput) {
        PhysicalParameter physicalParameter = monitoringInput.getPhysicalParameter();
        Unit unit = parameterToUnitMap.get(physicalParameter);
        Set<CnecResult> cnecResults = new HashSet<>();
        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", physicalParameter, state);
        boolean lfSuccess = computeLoadFlow(network);
        if (!lfSuccess) {
            String failureReason = String.format("Load-flow computation failed at state %s. Skipping this state.", state);
            return makeFailedMonitoringResultForStateWithNaNCnecRsults(monitoringInput, physicalParameter, state, failureReason);
        }
        List<AppliedNetworkActionsResult> appliedNetworkActionsResultList = new ArrayList<>();
        cnecs.forEach(cnec -> {
            if (cnec.computeMargin(network, unit) < 0) {
                // For Cnecs with overshoot, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getNetworkActionsAssociatedToCnec(state, monitoringInput.getCrac(), cnec, physicalParameter);
                // if there is any RA(s) available apply it/them
                if (!availableNetworkActions.isEmpty()) {
                    AppliedNetworkActionsResult appliedNetworkActionsResult = applyNetworkActions(network, availableNetworkActions, cnec.getId(), monitoringInput);
                    if (!appliedNetworkActionsResult.getAppliedNetworkActions().isEmpty()) {
                        appliedNetworkActionsResultList.add(appliedNetworkActionsResult);
                    }
                }
            }
            CnecResult cnecResult = new CnecResult(cnec, unit, cnec.computeValue(network, unit), cnec.computeMargin(network, unit), cnec.computeSecurityStatus(network, unit));
            cnecResults.add(cnecResult);
        });

        redispatchNetworkActions(network, appliedNetworkActionsResultList, monitoringInput.getScalableZonalData());

        // If some action were applied, recompute a loadflow
        if (appliedNetworkActionsResultList.stream().map(AppliedNetworkActionsResult::getAppliedNetworkActions).findAny().isPresent()) {
            lfSuccess = computeLoadFlow(network);
            if (!lfSuccess) {
                String failureReason = String.format("Load-flow computation failed at state %s after applying RAs. Skipping this state.", state);
                return makeFailedMonitoringResultForState(physicalParameter, state, failureReason, cnecResults);
            }
            // Re-compute all voltage/angle values
            cnecResults.clear();
            cnecs.forEach(cnec -> {
                CnecResult cnecResult = new CnecResult(cnec,
                    unit,
                    cnec.computeValue(network, unit),
                    cnec.computeMargin(network, unit),
                    cnec.computeSecurityStatus(network, unit));
                cnecResults.add(cnecResult);
            });
        }

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
            Map.of(state, appliedNetworkActionsResultList.stream().flatMap(r -> r.getAppliedNetworkActions().stream()).collect(Collectors.toSet())),
            monitoringResultStatus);
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
            .run(network, loadFlowRunParameters);
        if (loadFlowResult.isFailed()) {
            BUSINESS_WARNS.warn("LoadFlow error.");
        }
        TECHNICAL_LOGS.info("Load-flow computation [end]");
        return loadFlowResult.isFullyConverged();
    }

    private Set<NetworkAction> getNetworkActionsAssociatedToCnec(State state, Crac crac, Cnec cnec, PhysicalParameter physicalParameter) {
        Set<RemedialAction<?>> availableRemedialActions =
            crac.getRemedialActions().stream()
                .filter(remedialAction ->
                    remedialAction.getUsageRules().stream().filter(OnConstraint.class::isInstance)
                        .map(OnConstraint.class::cast)
                        .anyMatch(onConstraint -> onConstraint.getCnec().equals(cnec)))
                .collect(Collectors.toSet());
        if (availableRemedialActions.isEmpty()) {
            BUSINESS_WARNS.warn("{} Cnec {} in state {} has no associated RA. {} constraint cannot be secured.", physicalParameter, cnec.getId(), state.getId(), physicalParameter);
            return Collections.emptySet();
        }
        if (state.isPreventive()) {
            BUSINESS_WARNS.warn("{} Cnec {} is constrained in preventive state, it cannot be secured.", physicalParameter, cnec.getId());
            return Collections.emptySet();
        }
        // Convert remedial actions to network actions
        return availableRemedialActions.stream().filter(remedialAction -> {
            if (remedialAction instanceof NetworkAction) {
                return true;
            } else {
                BUSINESS_WARNS.warn("Remedial action {} of Cnec {} in state {} is ignored : it's not a network action.", remedialAction.getId(), cnec.getId(), state.getId());
                return false;
            }
        }).map(NetworkAction.class::cast).collect(Collectors.toSet());
    }

    private AppliedNetworkActionsResult applyNetworkActions(Network network, Set<NetworkAction> availableNetworkActions, String cnecId, MonitoringInput monitoringInput) {
        AppliedNetworkActionsResult appliedNetworkActionsResult;
        Set<RemedialAction> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        if (monitoringInput.getPhysicalParameter().equals(PhysicalParameter.VOLTAGE)) {
            for (NetworkAction na : availableNetworkActions) {
                na.apply(network);
                appliedNetworkActions.add(na);
            }
            appliedNetworkActionsResult = new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions)
                .withNetworkElementsToBeExcluded(new HashSet<>()).withPowerToBeRedispatched(new EnumMap<>(Country.class)).build();
        } else {
            boolean networkActionOk = false;
            EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
            Set<String> networkElementsToBeExcluded = new HashSet<>();
            for (NetworkAction na : availableNetworkActions) {
                EnumMap<Country, Double> tempPowerToBeRedispatched = new EnumMap<>(powerToBeRedispatched);
                for (Action ea : na.getElementaryActions()) {
                    networkActionOk = checkElementaryActionAndStoreInjection(
                        ea,
                        network,
                        cnecId,
                        na.getId(),
                        networkElementsToBeExcluded,
                        tempPowerToBeRedispatched,
                        monitoringInput.getScalableZonalData()
                    );
                    if (!networkActionOk) {
                        break;
                    }
                }
                if (networkActionOk) {
                    na.apply(network);
                    appliedNetworkActions.add(na);
                    powerToBeRedispatched.putAll(tempPowerToBeRedispatched);
                }
            }
            appliedNetworkActionsResult = new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions)
                .withNetworkElementsToBeExcluded(networkElementsToBeExcluded).withPowerToBeRedispatched(powerToBeRedispatched).build();
        }
        BUSINESS_LOGS.info(
            "Applied the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}",
            cnecId,
            appliedNetworkActions.stream().map(com.powsybl.openrao.data.crac.api.Identifiable::getId).collect(Collectors.joining(", "))
        );
        return appliedNetworkActionsResult;
    }

    /**
     * 1) Checks a network action's elementary action : it must be a Generator or a Load injection setpoint,
     * with a defined country.
     * 2) Stores applied injections on network
     * Returns false if network action must be filtered.
     */
    private boolean checkElementaryActionAndStoreInjection(Action ea,
                                                           Network network,
                                                           String angleCnecId,
                                                           String naId,
                                                           Set<String> networkElementsToBeExcluded,
                                                           Map<Country, Double> powerToBeRedispatched,
                                                           ZonalData<Scalable> scalableZonalData) {
        if (!(ea instanceof LoadAction) && !(ea instanceof GeneratorAction)) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);

        if (ne == null) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has no elementary actions.", naId, angleCnecId);
            return false;
        }

        Optional<Substation> substation = ((Injection<?>) ne).getTerminal().getVoltageLevel().getSubstation();
        if (substation.isEmpty()) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that doesn't have a substation.", naId, angleCnecId);
            return false;
        } else {
            Optional<Country> country = substation.get().getCountry();
            if (country.isEmpty()) {
                BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that doesn't have a country.", naId, angleCnecId);
                return false;
            } else {
                checkGlsks(country.get(), naId, angleCnecId, scalableZonalData);
                if (ne.getType().equals(IdentifiableType.GENERATOR)) {
                    powerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP() - ((GeneratorAction) ea).getActivePowerValue().getAsDouble(), Double::sum);
                } else if (ne.getType().equals(IdentifiableType.LOAD)) {
                    powerToBeRedispatched.merge(country.get(), -((Load) ne).getP0() + ((LoadAction) ea).getActivePowerValue().getAsDouble(), Double::sum);
                } else {
                    BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an injection setpoint that's neither a generator nor a load.", naId, angleCnecId);
                    return false;
                }
                networkElementsToBeExcluded.add(ne.getId());
            }
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
        if (ea instanceof DanglingLineAction danglingLineAction) {
            return network.getIdentifiable(danglingLineAction.getDanglingLineId());
        }
        if (ea instanceof ShuntCompensatorPositionAction shuntCompensatorPositionAction) {
            return network.getIdentifiable(shuntCompensatorPositionAction.getShuntCompensatorId());
        }
        return null;
    }

    /**
     * Checks glsks are correctly defined on country
     */
    private void checkGlsks(Country country, String naId, String angleCnecId, ZonalData<Scalable> scalableZonalData) {
        Set<Country> glskCountries = new TreeSet<>(Comparator.comparing(Country::getName));
        if (Objects.isNull(scalableZonalData)) {
            String error = "ScalableZonalData undefined (no GLSK given)";
            BUSINESS_LOGS.error(error);
            throw new OpenRaoException(error);
        }
        for (String zone : scalableZonalData.getDataPerZone().keySet()) {
            glskCountries.add(new CountryEICode(zone).getCountry());
        }
        if (!glskCountries.contains(country)) {
            throw new OpenRaoException(String.format(
                "INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.",
                country.getName(), naId, angleCnecId
            ));
        }
    }

    private MonitoringResult makeFailedMonitoringResultForStateWithNaNCnecRsults(MonitoringInput monitoringInput, PhysicalParameter physicalParameter, State state, String failureReason) {
        Set<CnecResult> cnecResults = new HashSet<>();
        CnecValue cnecValue = physicalParameter.equals(PhysicalParameter.ANGLE) ? new AngleCnecValue(Double.NaN) : new VoltageCnecValue(Double.NaN, Double.NaN);
        monitoringInput.getCrac().getCnecs(state).forEach(cnec -> cnecResults.add(new CnecResult(cnec, parameterToUnitMap.get(physicalParameter), cnecValue, Double.NaN, Cnec.SecurityStatus.FAILURE)));
        return makeFailedMonitoringResultForState(physicalParameter, state, failureReason, cnecResults);
    }

    private MonitoringResult makeFailedMonitoringResultForState(PhysicalParameter physicalParameter, State state, String failureReason, Set<CnecResult> cnecResults) {
        BUSINESS_WARNS.warn(failureReason);
        return new MonitoringResult(physicalParameter, cnecResults, Map.of(state, Collections.emptySet()), Cnec.SecurityStatus.FAILURE);
    }
}
