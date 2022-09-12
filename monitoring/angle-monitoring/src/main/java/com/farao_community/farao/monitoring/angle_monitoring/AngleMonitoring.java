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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * Monitors angle of AngleCnecs
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class AngleMonitoring {
    private final Crac crac;
    private final Network network;
    private final RaoResult raoResult;
    private final Map<Country, Set<ScalableNetworkElement>> glsks;
    private final String loadFlowProvider;
    private final LoadFlowParameters loadFlowParameters;
    private CopyOnWriteArrayList<AngleMonitoringResult> stateSpecificResults;
    private ConcurrentHashMap<Country, Double> powerToBeRedispatched;
    private Set<String> networkElementsToBeExcluded;

    public static final String CONTINGENCY_ERROR = "At least one contingency could not be monitored within the given time (24 hours). This should not happen.";

    public AngleMonitoring(Crac crac, Network network, RaoResult raoResult, Map<Country, Set<ScalableNetworkElement>> glsks, String loadFlowProvider, LoadFlowParameters loadFlowParameters) {
        this.crac = Objects.requireNonNull(crac);
        this.network = Objects.requireNonNull(network);
        this.raoResult = Objects.requireNonNull(raoResult);
        this.glsks = Objects.requireNonNull(glsks);
        this.loadFlowProvider = loadFlowProvider;
        this.loadFlowParameters = loadFlowParameters;
    }

    public AngleMonitoringResult run(int numberOfLoadFlowsInParallel) {
        stateSpecificResults = new CopyOnWriteArrayList<>();
        powerToBeRedispatched = new ConcurrentHashMap<>();
        networkElementsToBeExcluded = new HashSet<>();

        // I) Preventive state
        applyOptimalRemedialActions(crac.getPreventiveState(), network);
        stateSpecificResults.add(monitorAngleCnecs(crac.getPreventiveState(), network));

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
                            Network networkClone;
                            try {
                                networkClone = networkPool.getAvailableNetwork();
                            } catch (InterruptedException e) {
                                stateCountDownLatch.countDown();
                                Thread.currentThread().interrupt();
                                throw new FaraoException(CONTINGENCY_ERROR, e);
                            }
                            try {
                                state.getContingency().orElseThrow().apply(networkClone, null);
                                applyOptimalRemedialActions(state, networkClone);
                                stateSpecificResults.add(monitorAngleCnecs(state, networkClone));
                            } catch (Exception e) {
                                throw new FaraoException(CONTINGENCY_ERROR, e);
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return assembleAngleMonitoringResults();
    }

    private AngleMonitoringResult assembleAngleMonitoringResults() {
        Set<AngleMonitoringResult.AngleResult> assembledAngleCnecsWithAngle = new HashSet<>();
        Map<State, Set<NetworkAction>> assembledAppliedCras = new HashMap<>();
        AngleMonitoringResult.Status assembledStatus = AngleMonitoringResult.Status.SECURE;

        stateSpecificResults.forEach(individualResult -> {
            assembledAngleCnecsWithAngle.addAll(individualResult.getAngleCnecsWithAngle());
            assembledAppliedCras.putAll(individualResult.getAppliedCras());
        });
        // Status
        if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnsecure)) {
            assembledStatus = AngleMonitoringResult.Status.UNSECURE;
        } else if (stateSpecificResults.stream().anyMatch(AngleMonitoringResult::isUnknown)) {
            assembledStatus = AngleMonitoringResult.Status.UNKNOWN;
        }
        return new AngleMonitoringResult(assembledAngleCnecsWithAngle, assembledAppliedCras, assembledStatus);
    }

    private AngleMonitoringResult monitorAngleCnecs(State state, Network networkClone) {
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();
        // 1) Compute angles for all AngleCnecs
        boolean loadFlow1IsOk = computeLoadFlow(networkClone);
        if (!loadFlow1IsOk) {
            Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
            crac.getAngleCnecs(state).forEach(ac -> result.add(new AngleMonitoringResult.AngleResult(ac, state, Double.NaN)));
            return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), AngleMonitoringResult.Status.UNKNOWN);
        }
        Map<AngleCnec, Double> angleValues = new ConcurrentHashMap<>(computeAngles(crac.getAngleCnecs(state), networkClone));
        for (Map.Entry<AngleCnec, Double> angleCnecWithAngle : angleValues.entrySet()) {
            AngleCnec angleCnec = angleCnecWithAngle.getKey();
            if (checkThresholds(angleCnec, angleCnecWithAngle.getValue())) {
                // 2) For AngleCnecs with angle overshoot, get associated remedial actions
                Set<NetworkAction> availableNetworkActions = getAngleCnecNetworkActions(state, angleCnec);
                // and apply them
                appliedNetworkActions.addAll(applyNetworkActions(networkClone, angleCnec.getId(), availableNetworkActions));
            }
        }
        // 3) Redispatch to compensate the loss of generation/ load
        redispatchNetworkActions(networkClone);
        // Recompute LoadFlow
        if (!appliedNetworkActions.isEmpty()) {
            boolean loadFlow2IsOk = computeLoadFlow(networkClone);
            if (!loadFlow2IsOk) {
                Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
                angleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, state, angleResult)));
                return new AngleMonitoringResult(result, Map.of(state, Collections.emptySet()), AngleMonitoringResult.Status.UNSECURE);
            }
        }
        // 4) Re-compute all angle values
        Map<AngleCnec, Double> newAngleValues = new ConcurrentHashMap<>(computeAngles(crac.getAngleCnecs(state), networkClone));
        AngleMonitoringResult.Status status = AngleMonitoringResult.Status.SECURE;
        if (newAngleValues.keySet().stream().anyMatch(angleCnec -> checkThresholds(angleCnec, newAngleValues.get(angleCnec)))) {
            status = AngleMonitoringResult.Status.UNSECURE;
        }
        Set<AngleMonitoringResult.AngleResult> result = new HashSet<>();
        newAngleValues.forEach((angleCnecResult, angleResult) -> result.add(new AngleMonitoringResult.AngleResult(angleCnecResult, state, angleResult)));
        return new AngleMonitoringResult(result, Map.of(state, appliedNetworkActions), status);
    }

    public static boolean checkThresholds(AngleCnec angleCnec, Double angle) {
        return angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMax() && angle != null && angle > threshold.max().orElseThrow())
                || angleCnec.getThresholds().stream()
                .anyMatch(threshold -> threshold.limitsByMin() && angle != null && angle < threshold.min().orElseThrow());
    }

    private Set<NetworkAction> applyNetworkActions(Network networkClone, String angleCnecId, Set<NetworkAction> availableNetworkActions) {
        Set<NetworkAction> appliedNetworkActions = new HashSet<>();

        for (NetworkAction na : availableNetworkActions) {
            for (ElementaryAction ea : na.getElementaryActions()) {
                if (!storeElementaryActionsPower(ea, networkClone, angleCnecId, na.getId())) {
                    break;
                }
            }
            na.apply(networkClone);
            appliedNetworkActions.add(na);
        }

        return appliedNetworkActions;
    }

    private boolean storeElementaryActionsPower(ElementaryAction ea, Network networkClone, String angleCnecId, String naId) {
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
                if (ne instanceof Generator) {
                    powerToBeRedispatched.merge(country.get(), ((Generator) ne).getTargetP(), Double::sum);
                } else if (ne instanceof Load) {
                    powerToBeRedispatched.merge(country.get(), -((Load) ne).getP0(), Double::sum);
                } else {
                    BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it has an injection setpoint that's neither a generator nor a load.", naId, angleCnecId);
                    return false;
                }
                if (!glsks.containsKey(country.get())) {
                    BUSINESS_WARNS.warn("Glsks were not defined for country {}. Remedial action {} of AngleCnec {} is ignored.", country.get().getName(), naId, angleCnecId);
                    return false;
                }
                networkElementsToBeExcluded.add(ne.getId());
            }
        }
        return true;
    }

    private void redispatchNetworkActions(Network networkClone) {
        // Apply one redispatch action per country
        for (Map.Entry<Country, Double> redispatchPower : powerToBeRedispatched.entrySet()) {
            new RedispatchAction(redispatchPower.getKey().name(), redispatchPower.getValue(), networkElementsToBeExcluded, glsks.get(redispatchPower.getKey())).apply(networkClone);
        }
    }

    private Set<NetworkAction> getAngleCnecNetworkActions(State state, AngleCnec angleCnec) {
        Set<RemedialAction<?>> availableRemedialActions =
                crac.getRemedialActions().stream()
                        .filter(remedialAction ->
                                remedialAction.getUsageRules().stream().filter(OnAngleConstraint.class::isInstance)
                                        .map(OnAngleConstraint.class::cast)
                                        .anyMatch(onAngleConstraint -> onAngleConstraint.getAngleCnec().equals(angleCnec)))
                        .collect(Collectors.toSet());
        if (availableRemedialActions.isEmpty()) {
            BUSINESS_WARNS.warn("AngleCnec {} has no associated RA. Angle constraint cannot be remedied.", angleCnec.getId());
            return Collections.emptySet();
        }
        if (state.isPreventive()) {
            BUSINESS_WARNS.warn("AngleCnec {} is constrained in preventive state, it cannot be remedied.", angleCnec.getId());
            return Collections.emptySet();
        }
        // Convert remedial actions to network actions
        return availableRemedialActions.stream().filter(remedialAction -> {
            if (!(remedialAction instanceof NetworkAction)) {
                BUSINESS_WARNS.warn("Remedial action {} of AngleCnec {} is ignored : it's not a network action.", remedialAction.getId(), angleCnec.getId());
                return false;
            } else {
                return true;
            }
        }).map(NetworkAction.class::cast).collect(Collectors.toSet());
    }

    private boolean computeLoadFlow(Network networkClone) {
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
                .run(networkClone, loadFlowParameters);
        if (!loadFlowResult.isOk()) {
            BUSINESS_WARNS.warn("LoadFlow error.");
        }
        return loadFlowResult.isOk();
    }

    private void applyOptimalRemedialActions(State state, Network networkClone) {
        raoResult.getActivatedNetworkActionsDuringState(state)
                .forEach(na -> na.apply(networkClone));
        raoResult.getActivatedRangeActionsDuringState(state)
                .forEach(ra -> ra.apply(networkClone, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

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
}

