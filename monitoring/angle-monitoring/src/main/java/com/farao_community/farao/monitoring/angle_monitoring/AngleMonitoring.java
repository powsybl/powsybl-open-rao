/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.monitoring.angle_monitoring;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.InjectionSetpoint;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.usage_rule.OnAngleConstraint;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final Network network;
    private final RaoResult raoResult;
    private final Map<Country, Set<ScalableNetworkElement>> glsks;
    private final String loadFlowProvider;
    private final LoadFlowParameters loadFlowParameters;

    private CopyOnWriteArrayList<AngleMonitoringResult> stateSpecificResults;

    public AngleMonitoring(Crac crac, Network network, RaoResult raoResult, Map<Country, Set<ScalableNetworkElement>> glsks, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.crac = Objects.requireNonNull(crac);
        this.network = Objects.requireNonNull(network);
        this.raoResult = Objects.requireNonNull(raoResult);
        this.glsks = Objects.requireNonNull(glsks);
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowParameters = loadFlowParameters;
    }

    /**
     * Main function : runs AngleMonitoring computation on all AngleCnecs defined in the CRAC.
     * Returns an AngleMonitoringResult
     */
    public AngleMonitoringResult run(int numberOfLoadFlowsInParallel) throws FaraoException {
        stateSpecificResults = new CopyOnWriteArrayList<>();

        if (crac.getAngleCnecs().isEmpty()) {
            BUSINESS_WARNS.warn("No AngleCnecs defined.");
            return assembleAngleMonitoringResults();
        }

        // I) Preventive state
        if (Objects.nonNull(crac.getPreventiveState())) {
            applyOptimalRemedialActions(crac.getPreventiveState(), network);
            stateSpecificResults.add(monitorAngleCnecs(crac.getPreventiveState(), network));
        }
        // II) Curative states
        Set<State> contingencyStates = crac.getAngleCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        if (contingencyStates.isEmpty()) {
            return assembleAngleMonitoringResults();
        }

        try {
            try (AbstractNetworkPool networkPool =
                         AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), Math.min(numberOfLoadFlowsInParallel, contingencyStates.size()))
            ) {
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
                                applyOptimalRemedialActions(state, networkClone);
                                stateSpecificResults.add(monitorAngleCnecs(state, networkClone));
                            } catch (Exception e) {
                                BUSINESS_WARNS.warn(e.getMessage());
                            }
                            stateCountDownLatch.countDown();
                            try {
                                networkPool.releaseUsedNetwork(networkClone);
                            } catch (InterruptedException ex) {
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
     * Applies optimal remedial actions retrieved from raoResult for a given state on network
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
    private AngleMonitoringResult monitorAngleCnecs(State state, Network networkClone) {
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();
        Set<String> networkElementsToBeExcluded = new HashSet<>();
        Map<Country, Double> powerToBeRedispatched = new HashMap<>();
        // 1) Compute angles for all AngleCnecs
        boolean loadFlowIsOk = computeLoadFlow(networkClone);
        if (!loadFlowIsOk) {
            Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
            crac.getAngleCnecs(state).forEach(ac -> result.add(new AngleMonitoringResult.AngleResult(ac, Double.NaN)));
            return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), AngleMonitoringResult.Status.UNKNOWN);
        }
        Map<AngleCnec, Double> angleValues = new HashMap<>(computeAngles(crac.getAngleCnecs(state), networkClone));
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
            loadFlowIsOk = computeLoadFlow(networkClone);
            if (!loadFlowIsOk) {
                Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
                angleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, angleResult)));
                return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), AngleMonitoringResult.Status.UNSECURE);
            }
        }
        // 4) Re-compute all angle values
        Map<AngleCnec, Double> newAngleValues = new HashMap<>(computeAngles(crac.getAngleCnecs(state), networkClone));
        AngleMonitoringResult.Status status = AngleMonitoringResult.Status.SECURE;
        if (newAngleValues.keySet().stream().anyMatch(angleCnec -> thresholdOvershoot(angleCnec, newAngleValues.get(angleCnec)))) {
            status = AngleMonitoringResult.Status.UNSECURE;
        }
        Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
        newAngleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, angleResult)));
        return new AngleMonitoringResult(result, Map.of(state, appliedNetworkActions), status);
    }

    // ------- 1) Compute angles for all AngleCnecs -----
    /**
     * Angle computation on angleCnecs (parameter).
     * An angle is defined as the maximum phase difference between the exporting and the importing network elements' voltageLevels.
     * Returns a map linking angleCnecs to the computed angle in DEGREES.
     */
    private Map<AngleCnec, Double> computeAngles(Set<AngleCnec> angleCnecs, Network networkClone) {
        Map<AngleCnec, Double> anglePerCnec = new HashMap<>();
        angleCnecs.forEach(ac -> {
            VoltageLevel exportingVoltageLevel = (VoltageLevel) networkClone.getIdentifiable(ac.getExportingNetworkElement().getId());
            VoltageLevel importingVoltageLevel = (VoltageLevel) networkClone.getIdentifiable(ac.getImportingNetworkElement().getId());
            Double angle = 180 / Math.PI * (Collections.max(exportingVoltageLevel.getBusView().getBusStream().map(Bus::getAngle).collect(Collectors.toSet()))
                    - Collections.min(importingVoltageLevel.getBusView().getBusStream().map(Bus::getAngle).collect(Collectors.toSet())));
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
            if (!(remedialAction instanceof NetworkAction)) {
                BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} in state {} is ignored : it's not a network action.", remedialAction.getId(), angleCnec.getId(), state.getId());
                return false;
            } else {
                return true;
            }
        }).map(NetworkAction.class::cast).collect(Collectors.toSet());
    }

    /**
     * Applies network actions not filtered by checkElementaryActionAndStoreInjection to network
     * Returns applied network actions.
     */
    private Set<NetworkAction> applyNetworkActions(Network networkClone, String angleCnecId, Set<NetworkAction> availableNetworkActions, Set<String> networkElementsToBeExcluded, Map<Country, Double> powerToBeRedispatched) {
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();
        boolean networkActionOk = true;
        for (NetworkAction na : availableNetworkActions) {
            for (ElementaryAction ea : na.getElementaryActions()) {
                networkActionOk = networkActionOk && checkElementaryActionAndStoreInjection(ea, networkClone, angleCnecId, na.getId(), networkElementsToBeExcluded, powerToBeRedispatched);
                if (!networkActionOk) {
                    break;
                }
            }
            if (networkActionOk) {
                na.apply(networkClone);
                appliedNetworkActions.add(na);
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
        Map<Country, Double> temporaryPowerToBeRedispatched = powerToBeRedispatched;
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
                    temporaryPowerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP(), Double::sum);
                } else if (ne instanceof Load) {
                    temporaryPowerToBeRedispatched.merge(country.get(), -((Load) ne).getP0(), Double::sum);
                } else {
                    BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an injection setpoint that's neither a generator nor a load.", naId, angleCnecId);
                    return false;
                }
                networkElementsToBeExcluded.add(ne.getId());
            }
        }
        powerToBeRedispatched.putAll(temporaryPowerToBeRedispatched);
        return true;
    }

    /**
     * Checks glsks are correctly defined on country
     */
    private void checkGlsks(Country country, String naId, String angleCnecId) {
        if (!glsks.containsKey(country)) {
            throw new FaraoException(String.format("INFEASIBLE Angle Monitoring : Glsks were not defined for country %s. Remedial action %s of AngleCnec %s is ignored.", country.getName(), naId, angleCnecId));
        }
        if (glsks.get(country).stream().mapToDouble(ScalableNetworkElement::getPercentage).sum() != 100.) {
            throw new FaraoException(String.format("INFEASIBLE Angle Monitoring : Glsks were ill defined for country %s : %,.2f%% were defined.", country.getName(), glsks.get(country).stream().mapToDouble(ScalableNetworkElement::getPercentage).sum()));
        }
    }

    // ---------------  3) Redispatch to compensate the loss of generation/ load --------
    /**
     * Redispatches the net sum (generation - load) of power generations & loads, according to proportional glsks.
     */
    private void redispatchNetworkActions(Network networkClone, Map<Country, Double> powerToBeRedispatched, Set<String> networkElementsToBeExcluded) {
        // Apply one redispatch action per country
        for (Map.Entry<Country, Double> redispatchPower : powerToBeRedispatched.entrySet()) {
            new RedispatchAction(redispatchPower.getKey().name(), redispatchPower.getValue(), networkElementsToBeExcluded, glsks.get(redispatchPower.getKey())).apply(networkClone);
        }
    }

    // --------------- LoadFlow ------------
    /**
     * Runs a LoadFlow computation
     * Returns false if loadFlow has not converged.
     */
    private boolean computeLoadFlow(Network networkClone) {
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
     * - UNSECURE if any AngleMonitoringResult is UNSECURE.
     * - UNKNOWN if any AngleMonitoringResult is UNKNOWN and no AngleMonitoringResult is UNSECURE.
     */
    private AngleMonitoringResult assembleAngleMonitoringResults() {
        Set<AngleMonitoringResult.AngleResult> assembledAngleCnecsWithAngle = new HashSet<>();
        Map<State, Set<NetworkAction>> assembledAppliedCras = new HashMap<>();
        AngleMonitoringResult.Status assembledStatus = AngleMonitoringResult.Status.SECURE;

        stateSpecificResults.forEach(individualResult -> {
            assembledAngleCnecsWithAngle.addAll(individualResult.getAngleCnecsWithAngle());
            assembledAppliedCras.putAll(individualResult.getAppliedCras());
        });
        // Status
        if (stateSpecificResults.isEmpty()) {
            assembledStatus = AngleMonitoringResult.Status.UNKNOWN;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnsecure)) {
            assembledStatus = AngleMonitoringResult.Status.UNSECURE;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnknown)) {
            assembledStatus = AngleMonitoringResult.Status.UNKNOWN;
        }
        return new AngleMonitoringResult(assembledAngleCnecsWithAngle, assembledAppliedCras, assembledStatus);
    }
}

