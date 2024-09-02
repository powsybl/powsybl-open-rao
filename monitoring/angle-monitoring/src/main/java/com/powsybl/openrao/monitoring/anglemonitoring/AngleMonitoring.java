/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.anglemonitoring;

import com.powsybl.action.*;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.AngleCnec;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.cim.CimGlskPoint;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

/**
 * Monitors AngleCnecs' angles.
 * To remedy AngleCnecs with angles past their thresholds, corresponding remedial actions are applied.
 * These remedial actions are then compensated via redispatching.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoring {
    private final Crac crac;
    private final Network inputNetwork;
    private final RaoResult raoResult;
    private final CimGlskDocument cimGlskDocument;
    private final OffsetDateTime glskOffsetDateTime;

    private List<AngleMonitoringResult> stateSpecificResults;
    private final Set<Country> glskCountries;

    /**
     * Use this constructor if you want to run angle monitoring with your specific CimGlskDocument
     */
    public AngleMonitoring(Crac crac, Network inputNetwork, RaoResult raoResult, CimGlskDocument cimGlskDocument, OffsetDateTime glskOffsetDateTime) {
        this.crac = Objects.requireNonNull(crac);
        this.inputNetwork = Objects.requireNonNull(inputNetwork);
        this.raoResult = Objects.requireNonNull(raoResult);
        this.cimGlskDocument = Objects.requireNonNull(cimGlskDocument);
        this.glskOffsetDateTime = Objects.requireNonNull(glskOffsetDateTime);
        this.glskCountries = loadGlskCountries(cimGlskDocument);
    }

    /**
     * Use this constructor if you want to run angle monitoring with automatically-generated proportional GLSK
     */
    public AngleMonitoring(Crac crac, Network inputNetwork, RaoResult raoResult, Set<Country> glskCountries) {
        this.crac = Objects.requireNonNull(crac);
        this.inputNetwork = Objects.requireNonNull(inputNetwork);
        this.raoResult = Objects.requireNonNull(raoResult);
        this.cimGlskDocument = null;
        this.glskOffsetDateTime = null;
        this.glskCountries = Objects.requireNonNull(glskCountries);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with AngleMonitoringResult
     */
    public RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel) throws OpenRaoException {
        return new RaoResultWithAngleMonitoring(raoResult, run(loadFlowProvider, loadFlowParameters, numberOfLoadFlowsInParallel));
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an AngleMonitoringResult
     */

    @Deprecated
    public AngleMonitoringResult run(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel) throws OpenRaoException {
        BUSINESS_LOGS.info("----- Angle monitoring [start]");
        stateSpecificResults = new ArrayList<>();

        if (crac.getAngleCnecs().isEmpty()) {
            BUSINESS_WARNS.warn("No AngleCnecs defined.");
            stateSpecificResults.add(new AngleMonitoringResult(Collections.emptySet(), Collections.emptyMap(), AngleMonitoringResult.Status.SECURE));
            return assembleAngleMonitoringResults();
        }

        // I) Preventive state
        if (Objects.nonNull(crac.getPreventiveState())) {
            applyOptimalRemedialActions(crac.getPreventiveState(), inputNetwork);
            stateSpecificResults.add(monitorAngleCnecsAndLog(loadFlowProvider, loadFlowParameters, crac.getPreventiveState(), inputNetwork));
        }
        // II) Curative states
        Set<State> contingencyStates = crac.getAngleCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        if (contingencyStates.isEmpty()) {
            return assembleAngleMonitoringResults();
        }

        try {
            try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), Math.min(numberOfLoadFlowsInParallel, contingencyStates.size()), true)) {
                List<ForkJoinTask<Object>> tasks = contingencyStates.stream().map(state ->
                    networkPool.submit(() -> {
                        Network networkClone = networkPool.getAvailableNetwork();
                        try {
                            Contingency contingency = state.getContingency().orElseThrow();
                            if (!contingency.isValid(networkClone)) {
                                throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
                            }
                            contingency.toModification().apply(networkClone, (ComputationManager) null);
                            applyOptimalRemedialActionsOnContingencyState(state, networkClone);
                            stateSpecificResults.add(monitorAngleCnecsAndLog(loadFlowProvider, loadFlowParameters, state, networkClone));
                        } catch (Exception e) {
                            BUSINESS_WARNS.warn(e.getMessage());
                            stateSpecificResults.add(catchAngleMonitoringResult(state, AngleMonitoringResult.Status.UNKNOWN));
                        }
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
        return assembleAngleMonitoringResults();
    }

    /**
     * Gathers optimal remedial actions retrieved from raoResult for a given state on network.
     * For curative states, consider auto (when they exist) and curative states.
     */
    private void applyOptimalRemedialActionsOnContingencyState(State state, Network networkClone) {
        if (state.getInstant().isCurative()) {
            Optional<Contingency> contingency = state.getContingency();
            crac.getStates(contingency.orElseThrow()).forEach(contingencyState ->
                applyOptimalRemedialActions(state, networkClone));
        } else {
            applyOptimalRemedialActions(state, networkClone);
        }
    }

    /**
     * Applies optimal remedial actions retrieved from raoResult for a given state on network.
     */
    private void applyOptimalRemedialActions(State state, Network networkClone) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(na -> na.apply(networkClone));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(ra -> ra.apply(networkClone, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

    private AngleMonitoringResult monitorAngleCnecsAndLog(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, Network networkClone) {
        BUSINESS_LOGS.info("-- Monitoring angles at state \"{}\" [start]", state);
        AngleMonitoringResult result = monitorAngleCnecs(loadFlowProvider, loadFlowParameters, state, networkClone);
        result.printConstraints().forEach(BUSINESS_LOGS::info);
        BUSINESS_LOGS.info("-- Monitoring angles at state \"{}\" [end]", state);
        return result;
    }

    /**
     * AngleMonitoring computation on all AngleCnecs in the CRAC for a given state.
     * Returns an AngleMonitoringResult.
     */
    private AngleMonitoringResult monitorAngleCnecs(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, Network networkClone) {
        Set<RemedialAction<?>> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        Set<String> networkElementsToBeExcluded = new HashSet<>();
        EnumMap<Country, Double> powerToBeRedispatched = new EnumMap<>(Country.class);
        // 1) Compute angles for all AngleCnecs
        boolean loadFlowIsOk = computeLoadFlow(loadFlowProvider, loadFlowParameters, networkClone);
        if (!loadFlowIsOk) {
            return catchAngleMonitoringResult(state, AngleMonitoringResult.Status.DIVERGENT);
        }
        Map<AngleCnec, Double> angleValues = computeAngles(crac.getAngleCnecs(state), networkClone);
        for (Map.Entry<AngleCnec, Double> angleCnecWithAngle : angleValues.entrySet()) {
            AngleCnec angleCnec = angleCnecWithAngle.getKey();
            if (thresholdOvershoot(angleCnec, angleCnecWithAngle.getValue())) {
                // 2) For AngleCnecs with angle overshoot, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getAngleCnecNetworkActions(state, angleCnec);
                // and apply them
                appliedNetworkActions.addAll(applyNetworkActions(networkClone, angleCnec.getId(), availableNetworkActions, networkElementsToBeExcluded, powerToBeRedispatched));
            }
        }
        // 3) Redispatch to compensate the loss of generation/ load
        redispatchNetworkActions(networkClone, powerToBeRedispatched, networkElementsToBeExcluded);
        // Recompute LoadFlow
        if (!appliedNetworkActions.isEmpty()) {
            loadFlowIsOk = computeLoadFlow(loadFlowProvider, loadFlowParameters, networkClone);
            if (!loadFlowIsOk) {
                BUSINESS_WARNS.warn("Load-flow computation failed at state {} after applying RAs. Skipping this state.", state);
                Set<AngleMonitoringResult.AngleResult> result = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
                angleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, angleResult)));
                return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), AngleMonitoringResult.Status.DIVERGENT);
            }
        }
        // 4) Re-compute all angle values
        Map<AngleCnec, Double> newAngleValues = computeAngles(crac.getAngleCnecs(state), networkClone);
        AngleMonitoringResult.Status status = AngleMonitoringResult.Status.SECURE;
        if (newAngleValues.entrySet().stream().anyMatch(entrySet -> thresholdOvershoot(entrySet.getKey(), entrySet.getValue()))) {
            status = AngleMonitoringResult.Status.UNSECURE;
        }
        Set<AngleMonitoringResult.AngleResult> result = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
        newAngleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, angleResult)));
        return new AngleMonitoringResult(result, Map.of(state, appliedNetworkActions), status);
    }

    /**
     * Get the voltage level of an element in the network
     *
     * @return a VoltageLevel or null if the element wasn't found
     */
    private VoltageLevel getVoltageLevelOfElement(String elementId, Network network) {
        if (network.getBusBreakerView().getBus(elementId) != null) {
            return network.getBusBreakerView().getBus(elementId).getVoltageLevel();
        }
        return network.getVoltageLevel(elementId);
    }

    // ------- 1) Compute angles for all AngleCnecs -----

    /**
     * Angle computation on angleCnecs (parameter).
     * An angle is defined as the maximum phase difference between the exporting and the importing network elements' voltageLevels.
     * Returns a map linking angleCnecs to the computed angle in DEGREES.
     */
    private Map<AngleCnec, Double> computeAngles(Set<AngleCnec> angleCnecs, Network network) {
        Map<AngleCnec, Double> anglePerCnec = new HashMap<>();
        angleCnecs.forEach(ac -> {
            VoltageLevel exportingVoltageLevel = getVoltageLevelOfElement(ac.getExportingNetworkElement().getId(), network);
            VoltageLevel importingVoltageLevel = getVoltageLevelOfElement(ac.getImportingNetworkElement().getId(), network);
            Double angle = exportingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).max().getAsDouble()
                - importingVoltageLevel.getBusView().getBusStream().mapToDouble(Bus::getAngle).min().getAsDouble();
            anglePerCnec.put(ac, angle);
        });
        return anglePerCnec;
    }

    /**
     * Compares an angleCnec's thresholds to an angle (parameter).
     * Returns true if a threshold is breached.
     */
    public static boolean thresholdOvershoot(AngleCnec angleCnec, Double angle) {
        return angleCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMax() && angle != null && angle > threshold.max().orElseThrow())
            || angleCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMin() && angle != null && angle < threshold.min().orElseThrow());
    }

    // -------  2) For AngleCnecs with angle overshoot, get associated remedial actions and apply them -----

    /**
     * Retrieves the network actions that were defined for an angleCnec (parameter) in a given state (parameter).
     * Preventive network actions are filtered.
     */
    private Set<NetworkAction> getAngleCnecNetworkActions(State state, AngleCnec angleCnec) {
        Set<RemedialAction<?>> availableRemedialActions =
            crac.getRemedialActions().stream()
                .filter(remedialAction ->
                    remedialAction.getUsageRules().stream().filter(OnConstraint.class::isInstance)
                        .map(OnConstraint.class::cast)
                        .anyMatch(onAngleConstraint -> onAngleConstraint.getCnec().equals(angleCnec)))
                .collect(Collectors.toSet());
        if (availableRemedialActions.isEmpty()) {
            BUSINESS_WARNS.warn("AngleCnec {} in state {} has no associated RA. Angle constraint cannot be secured.", angleCnec.getId(), state.getId());
            return Collections.emptySet();
        }
        if (state.isPreventive()) {
            BUSINESS_WARNS.warn("AngleCnec {} is constrained in preventive state, it cannot be secured.", angleCnec.getId());
            return Collections.emptySet();
        }
        // Convert remedial actions to network actions
        return availableRemedialActions.stream().filter(remedialAction -> {
            if (remedialAction instanceof NetworkAction) {
                return true;
            } else {
                BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} in state {} is ignored : it's not a network action.", remedialAction.getId(), angleCnec.getId(), state.getId());
                return false;
            }
        }).map(NetworkAction.class::cast).collect(Collectors.toSet());
    }

    /**
     * Applies network actions not filtered by checkElementaryActionAndStoreInjection to network
     * Returns applied network actions.
     */
    private Set<NetworkAction> applyNetworkActions(Network networkClone, String angleCnecId, Set<NetworkAction> availableNetworkActions, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched) {
        Set<NetworkAction> appliedNetworkActions = new TreeSet<>(Comparator.comparing(NetworkAction::getId));
        boolean networkActionOk = false;
        for (NetworkAction na : availableNetworkActions) {
            EnumMap<Country, Double> tempPowerToBeRedispatched = new EnumMap<>(powerToBeRedispatched);
            for (Action ea : na.getElementaryActions()) {
                networkActionOk = checkElementaryActionAndStoreInjection(ea, networkClone, angleCnecId, na.getId(), networkElementsToBeExcluded, tempPowerToBeRedispatched);
                if (!networkActionOk) {
                    break;
                }
            }
            if (networkActionOk) {
                na.apply(networkClone);
                appliedNetworkActions.add(na);
                powerToBeRedispatched.putAll(tempPowerToBeRedispatched);
            }
        }
        BUSINESS_LOGS.info("Applying the following remedial action(s) in order to reduce constraints on CNEC \"{}\": {}", angleCnecId, appliedNetworkActions.stream().map(com.powsybl.openrao.data.cracapi.Identifiable::getId).collect(Collectors.joining(", ")));
        return appliedNetworkActions;
    }

    /**
     * 1) Checks a network action's elementary action : it must be a Generator or a Load injection setpoint,
     * with a defined country.
     * 2) Stores applied injections on network
     * Returns false if network action must be filtered.
     */
    private boolean checkElementaryActionAndStoreInjection(Action ea, Network networkClone, String angleCnecId, String naId, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched) {
        Identifiable<?> ne = getInjectionSetpointIdentifiable(ea, networkClone);
        if (ne == null) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        // Elementary actions are either generators or loads
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
                checkGlsks(country.get(), naId, angleCnecId);
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
    private void checkGlsks(Country country, String naId, String angleCnecId) {
        if (!glskCountries.contains(country)) {
            throw new OpenRaoException(String.format("INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.", country.getName(), naId, angleCnecId));
        }
    }

    // ---------------  3) Redispatch to compensate the loss of generation/ load --------

    /**
     * Redispatches the net sum (generation - load) of power generations & loads, according to merit order glsks.
     */
    private void redispatchNetworkActions(Network networkClone, Map<Country, Double> powerToBeRedispatched, Set<String> networkElementsToBeExcluded) {
        // Apply one redispatch action per country
        for (Map.Entry<Country, Double> redispatchPower : powerToBeRedispatched.entrySet()) {
            BUSINESS_LOGS.info("Redispatching {} MW in {} [start]", redispatchPower.getValue(), redispatchPower.getKey());
            RedispatchAction redispatchAction;
            if (cimGlskDocument != null) {
                Set<CimGlskPoint> countryGlskPoints = cimGlskDocument.getGlskPoints().stream()
                    .filter(glskPoint -> redispatchPower.getKey().equals(new CountryEICode(glskPoint.getSubjectDomainmRID()).getCountry())
                        && isInTimeInterval(glskOffsetDateTime, glskPoint.getPointInterval().getStart().toString(), glskPoint.getPointInterval().getEnd().toString()))
                    .map(CimGlskPoint.class::cast)
                    .collect(Collectors.toSet());
                if (countryGlskPoints.size() > 1) {
                    throw new OpenRaoException(String.format("> 1 (%s) glskPoints defined for country %s", countryGlskPoints.size(), redispatchPower.getKey().getName()));
                }
                redispatchAction = new RedispatchActionWithGlskPoint(networkElementsToBeExcluded, countryGlskPoints.iterator().next());
            } else {
                redispatchAction = new RedispatchActionWithAutoGlsk(networkElementsToBeExcluded, redispatchPower.getKey());
            }
            redispatchAction.apply(networkClone, redispatchPower.getValue());
            BUSINESS_LOGS.info("Redispatching {} MW in {} [end]", redispatchPower.getValue(), redispatchPower.getKey());
        }
    }

    // --------------- LoadFlow ------------

    /**
     * Runs a LoadFlow computation
     * Returns false if loadFlow has not converged.
     */
    private boolean computeLoadFlow(String loadFlowProvider, LoadFlowParameters loadFlowParameters, Network networkClone) {
        TECHNICAL_LOGS.info("Load-flow computation [start]");
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
            .run(networkClone, loadFlowParameters);
        if (!loadFlowResult.isFullyConverged()) {
            BUSINESS_WARNS.warn("LoadFlow error.");
        }
        TECHNICAL_LOGS.info("Load-flow computation [end]");
        return loadFlowResult.isFullyConverged();
    }

    // --------------- Merge results ------------

    /**
     * Assembles all AngleMonitoringResults computed.
     * Individual AngleResults and appliedCras maps are concatenated.
     * Global status :
     * - SECURE if all AngleMonitoringResults are SECURE.
     * - DIVERGENT if any AngleMonitoringResult is DIVERGENT.
     * - UNSECURE if any AngleMonitoringResult is UNSECURE.
     * - UNKNOWN if any AngleMonitoringResult is UNKNOWN and no AngleMonitoringResult is UNSECURE.
     */
    private AngleMonitoringResult assembleAngleMonitoringResults() {
        Set<AngleMonitoringResult.AngleResult> assembledAngleCnecsWithAngle = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
        Map<State, Set<RemedialAction<?>>> assembledAppliedCras = new HashMap<>();
        AngleMonitoringResult.Status assembledStatus = AngleMonitoringResult.Status.SECURE;

        stateSpecificResults.forEach(individualResult -> {
            assembledAngleCnecsWithAngle.addAll(individualResult.getAngleCnecsWithAngle());
            assembledAppliedCras.putAll(individualResult.getAppliedCras());
        });
        // Status
        if (stateSpecificResults.isEmpty()) {
            assembledStatus = AngleMonitoringResult.Status.UNKNOWN;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isDivergent)) {
            assembledStatus = AngleMonitoringResult.Status.DIVERGENT;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnsecure)) {
            assembledStatus = AngleMonitoringResult.Status.UNSECURE;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnknown)) {
            assembledStatus = AngleMonitoringResult.Status.UNKNOWN;
        }
        AngleMonitoringResult result = new AngleMonitoringResult(assembledAngleCnecsWithAngle, assembledAppliedCras, assembledStatus);
        result.printConstraints().forEach(BUSINESS_LOGS::info);
        BUSINESS_LOGS.info("----- Angle monitoring [end]");
        return result;
    }

    private AngleMonitoringResult catchAngleMonitoringResult(State state, AngleMonitoringResult.Status status) {
        BUSINESS_WARNS.warn("Load-flow computation failed at state {}. Skipping this state.", state);
        TreeSet<AngleMonitoringResult.AngleResult> result = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
        crac.getAngleCnecs(state).forEach(ac -> result.add(new AngleMonitoringResult.AngleResult(ac, Double.NaN)));
        return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), status);
    }

    private static Set<Country> loadGlskCountries(CimGlskDocument cimGlskDocument) {
        TreeSet<Country> countries = new TreeSet<>(Comparator.comparing(Country::getName));
        for (GlskPoint glskPoint : cimGlskDocument.getGlskPoints()) {
            countries.add(new CountryEICode(glskPoint.getSubjectDomainmRID()).getCountry());
        }
        return countries;
    }

    private boolean isInTimeInterval(OffsetDateTime offsetDateTime, String startTime, String endTime) {
        OffsetDateTime startTimeGlskPoint = OffsetDateTime.parse(startTime);
        OffsetDateTime endTimeGlskPoint = OffsetDateTime.parse(endTime);
        return !offsetDateTime.isBefore(startTimeGlskPoint) && offsetDateTime.isBefore(endTimeGlskPoint);
    }
}

