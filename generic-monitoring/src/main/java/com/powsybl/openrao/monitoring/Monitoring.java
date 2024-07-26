package com.powsybl.openrao.monitoring;

import com.powsybl.action.*;
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
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.*;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public class Monitoring {

    private final String loadFlowProvider;
    private final LoadFlowParameters loadFlowParameters;

    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowParameters = loadFlowParameters;
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public RaoResult runAngleAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput monitoringInput) throws OpenRaoException {
        // TODO add min(numberOfLoadFlowsInParallel, contingencyStates.size())
        return new RaoResultWithAngleMonitoring(monitoringInput.getRaoResult(), new Monitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput));
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public RaoResult runVoltageAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, MonitoringInput monitoringInput) {
        // TODO add min(numberOfLoadFlowsInParallel, contingencyStates.size())
        return new RaoResultWithVoltageMonitoring(monitoringInput.getRaoResult(), new Monitoring(loadFlowProvider, loadFlowParameters).runMonitoring(monitoringInput));
    }

    public MonitoringResult runMonitoring(MonitoringInput monitoringInput) {
        PhysicalParameter physicalParameter = monitoringInput.getPhysicalParameter();

        MonitoringResult monitoringResult = new MonitoringResult(physicalParameter, Collections.emptySet(), Collections.emptyMap(), MonitoringResult.Status.SECURE);
        Network inputNetwork = monitoringInput.getNetwork();
        Crac crac = monitoringInput.getCrac();
        RaoResult raoResult = monitoringInput.getRaoResult();

        BUSINESS_LOGS.info("----- {} monitoring [start]", physicalParameter);
        Set<? extends Cnec> cnecs = physicalParameter.equals(PhysicalParameter.ANGLE) ? crac.getAngleCnecs() : crac.getVoltageCnecs();
        if (cnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No Cnecs of type '{}' defined.", physicalParameter);
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        // I) Preventive state
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
            Set<? extends Cnec> preventiveStateCnecs = physicalParameter.equals(PhysicalParameter.ANGLE) ? crac.getAngleCnecs(preventiveState) : crac.getVoltageCnecs(preventiveState);
            monitoringResult.combine(monitorCnecs(preventiveState, preventiveStateCnecs, inputNetwork, monitoringInput));
        }

        // II) Curative states
        Set<State> contingencyStates;
        if (physicalParameter.equals(PhysicalParameter.VOLTAGE)) {
            contingencyStates = crac.getVoltageCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        } else {
            contingencyStates = crac.getAngleCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        }
        if (contingencyStates.isEmpty()) {
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return monitoringResult;
        }

        try {
            try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), contingencyStates.size(), true)) {
                List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                    networkPool.submit(() -> {
                        Network networkClone = networkPool.getAvailableNetwork();

                        Contingency contingency = state.getContingency().orElseThrow();
                        if (!contingency.isValid(networkClone)) {
                            throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
                        }
                        contingency.toModification().apply(networkClone, (ComputationManager) null);
                        applyOptimalRemedialActionsOnContingencyState(state, networkClone, crac, raoResult);
                        Set<? extends Cnec> currentStateCnecs = physicalParameter.equals(PhysicalParameter.ANGLE) ? crac.getAngleCnecs(state) : crac.getVoltageCnecs(state);
                        monitoringResult.combine(monitorCnecs(state, currentStateCnecs, networkClone, monitoringInput));
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
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
        monitoringResult.printConstraints().forEach(BUSINESS_LOGS::info);
        return monitoringResult;
    }

    private MonitoringResult monitorCnecs(State state, Set<? extends Cnec> cnecs, Network network, MonitoringInput monitoringInput) {
        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", monitoringInput.getPhysicalParameter(), state);
        boolean lfSuccess = computeLoadFlow(network);
        if (!lfSuccess) {
            return makeResultWhenLoadFlowFails(monitoringInput.getPhysicalParameter(), state, monitoringInput.getCrac());
        }

        // compute cnec results
        Set<? extends CnecResult> cnecResults = monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE) ? computeAngles((Set<AngleCnec>) cnecs, network) : computeVoltages((Set<VoltageCnec>) cnecs, network);

        // Check for threshold overshoot for the voltages/angles of each cnec
        List<AppliedNetworkActionsResult> appliedNetworkActionsResultList = new ArrayList<>();
        cnecResults.forEach(cnecResult -> {
            if (cnecResult.thresholdOvershoot()) {
                // For Cnecs with overshoot, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getNetworkActionsAssociatedToCnec(state, monitoringInput.getCrac(), cnecResult.getCnec(), monitoringInput.getPhysicalParameter());
                // and apply them
                AppliedNetworkActionsResult appliedNetworkActionsResult = applyNetworkActions(network, availableNetworkActions, cnecResult.getCnec().getId(), monitoringInput);
                appliedNetworkActionsResultList.add(appliedNetworkActionsResult);
            }
        });

        // if AngleMonitoring : Redispatch to compensate the loss of generation/ load
        if (monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE)) {
            redispatchNetworkActions(network, appliedNetworkActionsResultList, monitoringInput.getScalableZonalData());
        }

        // If some action were applied, recompute a loadflow
        if (appliedNetworkActionsResultList.stream().map(AppliedNetworkActionsResult::getAppliedNetworkActions).findAny().isPresent()) {
            boolean loadFlowIsOk = computeLoadFlow(network);
            if (!loadFlowIsOk) {
                BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", state);
                return new MonitoringResult(monitoringInput.getPhysicalParameter(), cnecResults, Map.of(state, Collections.emptySet()), MonitoringResult.Status.FAILURE);
            }
        }

        // Re-compute all voltage/angle values
        Set<? extends CnecResult> newCnecResults = monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE) ? computeAngles((Set<AngleCnec>) cnecs, network) : computeVoltages((Set<VoltageCnec>) cnecs, network);
        MonitoringResult.Status status = MonitoringResult.Status.SECURE;
        if (newCnecResults.stream().anyMatch(CnecResult::thresholdOvershoot)) {
            status = MonitoringResult.combineStatuses(
                newCnecResults.stream()
                    .map(CnecResult::getStatus)
                    .toArray(MonitoringResult.Status[]::new));
        }

        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [end]", monitoringInput.getPhysicalParameter(), state);
        return new MonitoringResult(monitoringInput.getPhysicalParameter(), newCnecResults, Map.of(state, appliedNetworkActionsResultList.stream().flatMap(r -> r.getAppliedNetworkActions().stream()).collect(Collectors.toSet())), status);
    }

    private void redispatchNetworkActions(Network network, List<AppliedNetworkActionsResult> appliedNetworkActionsResults, ZonalData<Scalable> scalableZonalData) {
        // Apply one redispatch action per country
        appliedNetworkActionsResults.forEach(appliedNetworkActionsResult -> {
            for (Map.Entry<Country, Double> redispatchPower : appliedNetworkActionsResult.getPowerToBeRedispatched().entrySet()) {
                BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", redispatchPower.getValue(), redispatchPower.getKey());
                List<Scalable> countryScalables = scalableZonalData.getDataPerZone().entrySet().stream().filter(entry -> redispatchPower.getKey().equals(new CountryEICode(entry.getKey()).getCountry()))
                    .map(Map.Entry::getValue).toList();
                if (countryScalables.size() > 1) {
                    throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryScalables.size(), redispatchPower.getKey().getName()));
                }
                new RedispatchAction(redispatchPower.getValue(), appliedNetworkActionsResult.getNetworkElementsToBeExcluded(), countryScalables.get(0)).apply(network);
                BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", redispatchPower.getValue(), redispatchPower.getKey());
            }
        });

    }

    /**
     * Gathers optimal remedial actions retrieved from raoResult for a given state on network.
     * For curative states, consider auto (when they exist) and curative states.
     */
    private void applyOptimalRemedialActionsOnContingencyState(State state, Network network, Crac crac, RaoResult raoResult) {
        if (state.getInstant().isCurative()) {
            Optional<Contingency> contingency = state.getContingency();
            crac.getStates(contingency.orElseThrow()).forEach(contingencyState ->
                applyOptimalRemedialActions(state, network, raoResult));
        } else {
            applyOptimalRemedialActions(state, network, raoResult);
        }
    }

    private static MonitoringResult makeResultWhenLoadFlowFails(PhysicalParameter physicalParameter, State state, Crac crac) {
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", state);
        if (physicalParameter.equals(PhysicalParameter.VOLTAGE)) {
            Set<VoltageCnecResult> voltageCnecResults = new HashSet<>();
            crac.getVoltageCnecs(state).forEach(vc ->
                voltageCnecResults.add(new VoltageCnecResult(vc, new VoltageCnecResult.ExtremeVoltageValues(new HashSet<>(List.of(Double.NaN))))
                ));
            return new MonitoringResult(physicalParameter, voltageCnecResults, new HashMap<>(), MonitoringResult.Status.FAILURE);
        } else {
            // ANGLE
            Set<AngleCnecResult> angleCnecResults = new HashSet<>();
            crac.getAngleCnecs(state).forEach(ac -> angleCnecResults.add(new AngleCnecResult(ac, Double.NaN)));
            return new MonitoringResult(physicalParameter, angleCnecResults, new HashMap<>(), MonitoringResult.Status.FAILURE);
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

    private Set<AngleCnecResult> computeAngles(Set<AngleCnec> angleCnecs, Network network) {
        Set<AngleCnecResult> angleCnecResults = new HashSet<>();
        angleCnecs.forEach(ac -> {
            VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(ac.getExportingNetworkElement().getId(), network);
            VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(ac.getImportingNetworkElement().getId(), network);
            Double angle = exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
                - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble();
            angleCnecResults.add(new AngleCnecResult(ac, angle));
        });
        return angleCnecResults;
    }

    private VoltageLevel getVoltageLevelOfElement(String elementId, Network network) {
        if (network.getBusBreakerView().getBus(elementId) != null) {
            return network.getBusBreakerView().getBus(elementId).getVoltageLevel();
        }
        return network.getVoltageLevel(elementId);
    }

    private Set<VoltageCnecResult> computeVoltages(Set<VoltageCnec> voltageCnecs, Network network) {
        Set<VoltageCnecResult> voltageCnecResults = new HashSet<>();
        voltageCnecs.forEach(vc -> {
            VoltageLevel voltageLevel = network.getVoltageLevel(vc.getNetworkElement().getId());
            Set<Double> voltages = null;
            if (voltageLevel != null) {
                voltages = voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet());
            }
            BusbarSection busbarSection = network.getBusbarSection(vc.getNetworkElement().getId());
            if (busbarSection != null) {
                Double busBarVoltages = busbarSection.getV();
                voltages = new HashSet<>();
                voltages.add(busBarVoltages);
            }
            if (voltageLevel != null) {
                voltageCnecResults.add(new VoltageCnecResult(vc, new VoltageCnecResult.ExtremeVoltageValues(voltages)));
            }
        });
        return voltageCnecResults;
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
        Set<RemedialAction> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        if (monitoringInput.getPhysicalParameter().equals(PhysicalParameter.VOLTAGE)) {
            for (NetworkAction na : availableNetworkActions) {
                na.apply(network);
                appliedNetworkActions.add(na);
            }
            return new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions).build();
        } else {
            boolean networkActionOk = false;

            EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
            Set<String> networkElementsToBeExcluded = new HashSet<>();
            for (NetworkAction na : availableNetworkActions) {
                EnumMap<Country, Double> tempPowerToBeRedispatched = new EnumMap<>(powerToBeRedispatched);
                for (Action ea : na.getElementaryActions()) {
                    networkActionOk = checkElementaryActionAndStoreInjection(ea, network, cnecId, na.getId(), networkElementsToBeExcluded, tempPowerToBeRedispatched, monitoringInput.getScalableZonalData());
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
            return new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(appliedNetworkActions)
                .withNetworkElementsToBeExcluded(networkElementsToBeExcluded).withPowerToBeRedispatched(powerToBeRedispatched).build();
        }
    }

    /**
     * 1) Checks a network action's elementary action : it must be a Generator or a Load injection setpoint,
     * with a defined country.
     * 2) Stores applied injections on network
     * Returns false if network action must be filtered.
     */
    private boolean checkElementaryActionAndStoreInjection(Action ea, Network network, String angleCnecId, String naId, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched, ZonalData<Scalable> scalableZonalData) {
        if (!(ea instanceof LoadAction) && !(ea instanceof GeneratorAction)) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, network);

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
        for (String zone : scalableZonalData.getDataPerZone().keySet()) {
            glskCountries.add(new CountryEICode(zone).getCountry());
        }
        if (!glskCountries.contains(country)) {
            throw new OpenRaoException(String.format("INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.", country.getName(), naId, angleCnecId));
        }
    }

}
