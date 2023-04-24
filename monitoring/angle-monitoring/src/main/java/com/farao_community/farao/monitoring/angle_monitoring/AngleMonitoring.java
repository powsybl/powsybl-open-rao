/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.cim.CimGlskPoint;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_LOGS;
import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * Monitors AngleCnecs' angles.
 * To remedy AngleCnecs with angles past their thresholds, corresponding remedial actions are applied.
 * These remedial actions are then compensated via redispatching.
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoring {
    public static final String CONTINGENCY_ERROR = "At least one contingency could not be monitored within the given time (24 hours). This should not happen.";

    private final Crac crac;
    private final Network inputNetwork;
    private final RaoResult raoResult;
    private final CimGlskDocument cimGlskDocument;

    private List<AngleMonitoringResult> stateSpecificResults;
    private Set<Country> glskCountries;
    private OffsetDateTime glskOffsetDateTime;

    public AngleMonitoring(Crac crac, Network inputNetwork, RaoResult raoResult, CimGlskDocument cimGlskDocument) {
        this.crac = Objects.requireNonNull(crac);
        this.inputNetwork = Objects.requireNonNull(inputNetwork);
        this.raoResult = Objects.requireNonNull(raoResult);
        this.cimGlskDocument = Objects.requireNonNull(cimGlskDocument);
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an AngleMonitoringResult
     */
    public AngleMonitoringResult run(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, OffsetDateTime glskOffsetDateTime) throws FaraoException {
        this.glskOffsetDateTime = glskOffsetDateTime;
        stateSpecificResults = new ArrayList<>();
        loadGlskCountries();

        if (crac.getAngleCnecs().isEmpty()) {
            BUSINESS_WARNS.warn("No AngleCnecs defined.");
            stateSpecificResults.add(new AngleMonitoringResult(Collections.emptySet(), Collections.emptyMap(), AngleMonitoringResult.Status.SECURE));
            return assembleAngleMonitoringResults();
        }

        // I) Preventive state
        if (Objects.nonNull(crac.getPreventiveState())) {
            applyOptimalRemedialActions(crac.getPreventiveState(), inputNetwork);
            stateSpecificResults.add(monitorAngleCnecs(loadFlowProvider, loadFlowParameters, crac.getPreventiveState(), inputNetwork));
        }
        // II) Curative states
        Set<State> contingencyStates = crac.getAngleCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        if (contingencyStates.isEmpty()) {
            return assembleAngleMonitoringResults();
        }

        try {
            int numberOfClones = Math.min(numberOfLoadFlowsInParallel, contingencyStates.size());

            try (AbstractNetworkPool networkPool =
                         AbstractNetworkPool.create(inputNetwork, inputNetwork.getVariantManager().getWorkingVariantId(), numberOfClones)
            ) {

                if (numberOfClones != 1) {
                    networkPool.addNetworkClones(numberOfClones);
                }

                CountDownLatch stateCountDownLatch = new CountDownLatch(contingencyStates.size());
                contingencyStates.forEach(state ->
                        networkPool.submit(() -> {
                            Network networkClone = null;
                            try {
                                networkClone = networkPool.getAvailableNetwork();
                            } catch (Exception e) {
                                stateCountDownLatch.countDown();
                                Thread.currentThread().interrupt();
                                throw new FaraoException(CONTINGENCY_ERROR, e);
                            }
                            try {
                                state.getContingency().orElseThrow().apply(networkClone, null);
                                applyOptimalRemedialActionsOnContingencyState(state, networkClone);
                                stateSpecificResults.add(monitorAngleCnecs(loadFlowProvider, loadFlowParameters, state, networkClone));
                            } catch (Exception e) {
                                BUSINESS_WARNS.warn(e.getMessage());
                                stateSpecificResults.add(catchAngleMonitoringResult(state, AngleMonitoringResult.Status.UNKNOWN));
                            }
                            try {
                                networkPool.releaseUsedNetwork(networkClone);
                                stateCountDownLatch.countDown();
                            } catch (InterruptedException ex) {
                                stateCountDownLatch.countDown();
                                Thread.currentThread().interrupt();
                                throw new FaraoException(ex);
                            }
                        }));
                boolean success = stateCountDownLatch.await(24, TimeUnit.HOURS);
                if (!success) {
                    throw new FaraoException(CONTINGENCY_ERROR);
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
        if (state.getInstant().equals(Instant.CURATIVE)) {
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

    /**
     * AngleMonitoring computation on all AngleCnecs in the CRAC for a given state.
     * Returns an AngleMonitoringResult.
     */
    private AngleMonitoringResult monitorAngleCnecs(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, Network networkClone) {
        Set<NetworkAction> appliedNetworkActions = new TreeSet<>(Comparator.comparing(NetworkAction::getId));
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

    // ------- 1) Compute angles for all AngleCnecs -----
    /**
     * Angle computation on angleCnecs (parameter).
     * An angle is defined as the maximum phase difference between the exporting and the importing network elements' voltageLevels.
     * Returns a map linking angleCnecs to the computed angle in DEGREES.
     */
    private Map<AngleCnec, Double> computeAngles(Set<AngleCnec> angleCnecs, Network network) {
        Map<AngleCnec, Double> anglePerCnec = new HashMap<>();
        angleCnecs.forEach(ac -> {
            VoltageLevel exportingVoltageLevel = network.getVoltageLevel(ac.getExportingNetworkElement().getId());
            VoltageLevel importingVoltageLevel = network.getVoltageLevel(ac.getImportingNetworkElement().getId());
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
                                remedialAction.getUsageRules().stream().filter(OnAngleConstraint.class::isInstance)
                                        .map(OnAngleConstraint.class::cast)
                                        .anyMatch(onAngleConstraint -> onAngleConstraint.getAngleCnec().equals(angleCnec)))
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
            for (ElementaryAction ea : na.getElementaryActions()) {
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
        return appliedNetworkActions;
    }

    /**
     * 1) Checks a network action's elementary action : it must be a Generator or a Load injection setpoint,
     * with a defined country.
     * 2) Stores applied injections on network
     * Returns false if network action must be filtered.
     */
    private boolean checkElementaryActionAndStoreInjection(ElementaryAction ea, Network networkClone, String angleCnecId, String naId, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched) {
        if (!(ea instanceof InjectionSetpoint)) {
            BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an elementary action that's not an injection setpoint.", naId, angleCnecId);
            return false;
        }
        // Elementary actions are either generators or loads
        Identifiable<?> ne = networkClone.getIdentifiable(((InjectionSetpoint) ea).getNetworkElement().getId());
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
                if (ne instanceof Generator) {
                    powerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP() - ((InjectionSetpoint) ea).getSetpoint(), Double::sum);
                } else if (ne instanceof Load) {
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
    private void checkGlsks(Country country, String naId, String angleCnecId) {
        if (!glskCountries.contains(country)) {
            throw new FaraoException(String.format("INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.", country.getName(), naId, angleCnecId));
        }
    }

    // ---------------  3) Redispatch to compensate the loss of generation/ load --------
    /**
     * Redispatches the net sum (generation - load) of power generations & loads, according to merit order glsks.
     */
    private void redispatchNetworkActions(Network networkClone, Map<Country, Double> powerToBeRedispatched, Set<String> networkElementsToBeExcluded) {
        // Apply one redispatch action per country
        for (Map.Entry<Country, Double> redispatchPower : powerToBeRedispatched.entrySet()) {
            Set<CimGlskPoint> countryGlskPoints = cimGlskDocument.getGlskPoints().stream()
                    .filter(glskPoint -> redispatchPower.getKey().equals(new CountryEICode(glskPoint.getSubjectDomainmRID()).getCountry())
                    && isInTimeInterval(glskOffsetDateTime, glskPoint.getPointInterval().getStart().toString(), glskPoint.getPointInterval().getEnd().toString()))
                    .map(CimGlskPoint.class::cast)
                    .collect(Collectors.toSet());
            if (countryGlskPoints.size() > 1) {
                throw new FaraoException(String.format("> 1 (%s) glskPoints defined for country %s", countryGlskPoints.size(), redispatchPower.getKey().getName()));
            }
            new RedispatchAction(redispatchPower.getValue(), networkElementsToBeExcluded, countryGlskPoints.iterator().next()).apply(networkClone);
            BUSINESS_LOGS.info("Redispatching done for country %s", redispatchPower.getKey().name());
        }
    }

    // --------------- LoadFlow ------------
    /**
     * Runs a LoadFlow computation
     * Returns false if loadFlow has not converged.
     */
    private boolean computeLoadFlow(String loadFlowProvider, LoadFlowParameters loadFlowParameters, Network networkClone) {
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
                .run(networkClone, loadFlowParameters);
        if (!loadFlowResult.isOk()) {
            BUSINESS_WARNS.warn("LoadFlow error.");
        }
        return loadFlowResult.isOk();
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
        Map<State, Set<NetworkAction>> assembledAppliedCras = new HashMap<>();
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
        return new AngleMonitoringResult(assembledAngleCnecsWithAngle, assembledAppliedCras, assembledStatus);
    }

    private AngleMonitoringResult catchAngleMonitoringResult(State state, AngleMonitoringResult.Status status) {
        TreeSet<AngleMonitoringResult.AngleResult> result = new TreeSet<>(Comparator.comparing(AngleMonitoringResult.AngleResult::getId));
        crac.getAngleCnecs(state).forEach(ac -> result.add(new AngleMonitoringResult.AngleResult(ac, Double.NaN)));
        return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), status);
    }

    private void loadGlskCountries() {
        glskCountries = new TreeSet<>(Comparator.comparing(Country::getName));
        for (GlskPoint glskPoint : cimGlskDocument.getGlskPoints()) {
            glskCountries.add(new CountryEICode(glskPoint.getSubjectDomainmRID()).getCountry());
        }
    }

    private boolean isInTimeInterval(OffsetDateTime offsetDateTime, String startTime, String endTime) {
        OffsetDateTime startTimeGlskPoint = OffsetDateTime.parse(startTime);
        OffsetDateTime endTimeGlskPoint = OffsetDateTime.parse(endTime);
        return !offsetDateTime.isBefore(startTimeGlskPoint) && offsetDateTime.isBefore(endTimeGlskPoint);
    }
}

