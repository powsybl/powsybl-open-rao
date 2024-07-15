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
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.ElementaryAction;
import com.powsybl.openrao.data.cracapi.networkaction.InjectionSetpoint;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.redispatching.RedispatchAction;
import com.powsybl.openrao.monitoring.results.AngleCnecResult;
import com.powsybl.openrao.monitoring.results.CnecResult;
import com.powsybl.openrao.monitoring.results.MonitoringResult;
import com.powsybl.openrao.monitoring.results.VoltageCnecResult;
import com.powsybl.openrao.util.AbstractNetworkPool;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public class Monitoring {

    private List<MonitoringResult> stateSpecificResults;
    private final String loadFlowProvider;
    private final LoadFlowParameters loadFlowParameters;

    public Monitoring(String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowParameters = loadFlowParameters;
    }

    public MonitoringResult runMonitoring(MonitoringInput monitoringInput) {
        PhysicalParameter physicalParameter = monitoringInput.getPhysicalParameter();
        Network inputNetwork = monitoringInput.getNetwork();
        Crac crac = monitoringInput.getCrac();
        RaoResult raoResult = monitoringInput.getRaoResult();

        BUSINESS_LOGS.info("----- {} monitoring [start]", physicalParameter);
        Set<? extends Cnec> cnecs = physicalParameter.equals(PhysicalParameter.ANGLE) ? crac.getAngleCnecs() : crac.getVoltageCnecs();
        if (cnecs.isEmpty()) {
            BUSINESS_WARNS.warn("No Cnecs of type '{}' defined.", physicalParameter);
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return new MonitoringResult(Collections.emptySet(), Collections.emptyMap(), MonitoringResult.Status.SECURE);
        }

        // I) Preventive state
        State preventiveState = crac.getPreventiveState();
        if (Objects.nonNull(preventiveState)) {
            applyOptimalRemedialActions(preventiveState, inputNetwork, raoResult);
            MonitoringResult monitoringResult = monitorCnecs(preventiveState, cnecs, inputNetwork, monitoringInput);
            stateSpecificResults.add(monitoringResult);
        }

        // II) Curative states
        Set<State> contingencyStates;
        if (physicalParameter.equals(PhysicalParameter.VOLTAGE)) {
            contingencyStates = monitoringInput.getCrac().getVoltageCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        } else {
            contingencyStates = monitoringInput.getCrac().getAngleCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        }
        if (contingencyStates.isEmpty()) {
            BUSINESS_LOGS.info("----- {} monitoring [end]", physicalParameter);
            return assembleMonitoringResults();
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
                        stateSpecificResults.add(monitorCnecs(state, cnecs, networkClone, monitoringInput));
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
        return assembleMonitoringResults();
    }

    private MonitoringResult monitorCnecs(State state, Set<? extends Cnec> cnecs, Network network, MonitoringInput monitoringInput) {
        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [start]", monitoringInput.getPhysicalParameter(), state);
        boolean lfSuccess = computeLoadFlow(network);
        if (!lfSuccess) {
            return makeResultWhenLoadFlowFails(monitoringInput.getPhysicalParameter(), state, monitoringInput.getCrac());
        }
        // compute cnec results
        Set<? extends CnecResult> cnecResults = monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE) ? computeAngles((Set<AngleCnec>) cnecs, network) : computeVoltages((Set<VoltageCnec>) cnecs, network);
        // / Check for threshold overshoot for the voltages/angles of each cnec

        AppliedNetworkActionsResult overallAppliedNetworkActionsResult = new AppliedNetworkActionsResult.AppliedNetworkActionsResultBuilder().withAppliedNetworkActions(new HashSet<>()).withNetworkElementsToBeExcluded(new HashSet<>()).withPowerToBeRedispatched(new HashMap<>()).build();
        cnecResults.forEach(cnecResult -> {
            if (cnecResult.thresholdOvershoot()) {
                // 2) For Cnecs with overshoot, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getNetworkActionsAssociatedToCnec(state, monitoringInput.getCrac(), cnecResult.getCnec(), monitoringInput.getPhysicalParameter());
                // and apply them
                overallAppliedNetworkActionsResult.addAll(applyNetworkActions(network, availableNetworkActions, cnecResult.getCnec().getId(), monitoringInput));
            }
        });

        // if AngleMonitoring : Redispatch to compensate the loss of generation/ load
        if (monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE)) {
            redispatchNetworkActions(network, overallAppliedNetworkActionsResult, monitoringInput.getScalableZonalData());
        }

        // If some action were applied, recompute a loadflow
        if (!overallAppliedNetworkActionsResult.getAppliedNetworkActions().isEmpty()) {
            boolean loadFlowIsOk = computeLoadFlow(network);
            if (!loadFlowIsOk) {
                BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", state);
                return new MonitoringResult(cnecResults, Map.of(state, Collections.emptySet()), MonitoringResult.Status.FAILURE);
            }
        }

        // Re-compute all voltage/angle values
        Set<? extends CnecResult> newCnecResults = monitoringInput.getPhysicalParameter().equals(PhysicalParameter.ANGLE) ? computeAngles((Set<AngleCnec>) cnecs, network) : computeVoltages((Set<VoltageCnec>) cnecs, network);
        MonitoringResult.Status status = MonitoringResult.Status.SECURE;
        if (newCnecResults.stream().anyMatch(CnecResult::thresholdOvershoot)) {
            status = concatenateStatuses(newCnecResults.stream().map(CnecResult::getStatus).collect(Collectors.toSet()));
        }

        //result.printConstraints().forEach(BUSINESS_LOGS::info);
        BUSINESS_LOGS.info("-- '{}' Monitoring at state '{}' [end]", monitoringInput.getPhysicalParameter(), state);
        return new MonitoringResult(newCnecResults, Map.of(state, overallAppliedNetworkActionsResult.getAppliedNetworkActions()), status);
    }

    private void redispatchNetworkActions(Network network, AppliedNetworkActionsResult overallAppliedNetworkActionsResult, ZonalData<Scalable> scalableZonalData) {
        // Apply one redispatch action per country
        for (Map.Entry<Country, Double> redispatchPower : overallAppliedNetworkActionsResult.getPowerToBeRedispatched().entrySet()) {
            BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", redispatchPower.getValue(), redispatchPower.getKey());
            List<Scalable> countryScalables = scalableZonalData.getDataPerZone().entrySet().stream().filter(entry -> redispatchPower.getKey().equals(new CountryEICode(entry.getKey()).getCountry()))
                .map(Map.Entry::getValue).toList();
            if (countryScalables.size() > 1) {
                throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryScalables.size(), redispatchPower.getKey().getName()));
            }
            new RedispatchAction(redispatchPower.getValue(), overallAppliedNetworkActionsResult.getNetworkElementsToBeExcluded(), countryScalables.get(0)).apply(network);
            BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", redispatchPower.getValue(), redispatchPower.getKey());
        }
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

    private static MonitoringResult makeResultWhenLoadFlowFails(PhysicalParameter physicalParameter, State
        state, Crac crac) {
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", state);
        if (physicalParameter.equals(PhysicalParameter.VOLTAGE)) {
            Set<VoltageCnecResult> voltageCnecResults = new HashSet<>();
            crac.getVoltageCnecs(state).forEach(vc ->
                voltageCnecResults.add(new VoltageCnecResult(vc, new VoltageCnecResult.ExtremeVoltageValues(new HashSet<>(List.of(Double.NaN))))
                ));
            return new MonitoringResult(voltageCnecResults, new HashMap<>(), MonitoringResult.Status.FAILURE);
        } else {
            // ANGLE
            Set<AngleCnecResult> angleCnecResults = new HashSet<>();
            crac.getAngleCnecs(state).forEach(ac -> angleCnecResults.add(new AngleCnecResult(ac, Double.NaN)));
            return new MonitoringResult(angleCnecResults, new HashMap<>(), MonitoringResult.Status.FAILURE);
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
        return loadFlowResult.isPartiallyConverged();
    }

    private MonitoringResult assembleMonitoringResults() {
        Set<CnecResult> cnecResults = new HashSet<>();
        Map<State, Set<RemedialAction>> appliedRas = new HashMap<>();
        Set<MonitoringResult.Status> statuses = new HashSet<>();
        stateSpecificResults.forEach(s -> {
            cnecResults.addAll(s.getCnecResults());
            appliedRas.putAll(s.getAppliedRas());
            statuses.add(s.getStatus());
        });

        if (cnecResults.stream().noneMatch(CnecResult::thresholdOvershoot)) {
            BUSINESS_LOGS.info("All voltage CNECs are secure.");
        } else {
            BUSINESS_LOGS.info("Some voltage CNECs are not secure:");
            cnecResults.forEach(cnecRes -> BUSINESS_LOGS.info(cnecRes.print()));
        }
        return new MonitoringResult(cnecResults, appliedRas, concatenateStatuses(statuses));
    }

    private MonitoringResult.Status concatenateStatuses(Set<MonitoringResult.Status> statusList) {
        boolean atLeastOneHigh = statusList.stream()
            .anyMatch(result -> result.equals(MonitoringResult.Status.HIGH_CONSTRAINT)
                || result.equals(MonitoringResult.Status.HIGH_AND_LOW_CONSTRAINTS));

        boolean atLeastOneLow = statusList.stream()
            .anyMatch(result -> result.equals(MonitoringResult.Status.LOW_CONSTRAINT)
                || result.equals(MonitoringResult.Status.HIGH_AND_LOW_CONSTRAINTS));

        boolean atLeastOneFailed = statusList.stream()
            .anyMatch(result -> result.equals(MonitoringResult.Status.FAILURE));

        if (statusList.isEmpty() || atLeastOneFailed) {
            return MonitoringResult.Status.FAILURE;
        }

        if (atLeastOneHigh && atLeastOneLow) {
            return MonitoringResult.Status.HIGH_AND_LOW_CONSTRAINTS;
        }
        if (atLeastOneHigh) {
            return MonitoringResult.Status.HIGH_CONSTRAINT;
        }
        if (atLeastOneLow) {
            return MonitoringResult.Status.LOW_CONSTRAINT;
        }
        return MonitoringResult.Status.SECURE;
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
                for (ElementaryAction ea : na.getElementaryActions()) {
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
    private boolean checkElementaryActionAndStoreInjection(ElementaryAction ea, Network network, String angleCnecId, String naId, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched, ZonalData<Scalable> scalableZonalData) {
        if (!(ea instanceof InjectionSetpoint)) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        // Elementary actions are either generators or loads
        Identifiable<?> ne = network.getIdentifiable(((InjectionSetpoint) ea).getNetworkElement().getId());
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
                    powerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP() - ((InjectionSetpoint) ea).getSetpoint(), Double::sum);
                } else if (ne.getType().equals(IdentifiableType.LOAD)) {
                    powerToBeRedispatched.merge(country.get(), -((Load) ne).getP0() + ((InjectionSetpoint) ea).getSetpoint(), Double::sum);
                } else {
                    BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an injection setpoint that's neither a generator nor a load.", naId, angleCnecId);
                    return false;
                }
                networkElementsToBeExcluded.add(ne.getId());
            }
        }
        return true;
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
