/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.monitoring.voltagemonitoring;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.monitoring.monitoringcommon.json.MonitoringCommonReports;
import com.powsybl.openrao.util.AbstractNetworkPool;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Monitors voltage of VoltageCnecs
 *
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class VoltageMonitoring {
    public static final String CONTINGENCY_ERROR = "At least one contingency could not be monitored. This should not happen.";
    private final Crac crac;
    private final Network network;
    private final RaoResult raoResult;
    private List<VoltageMonitoringResult> stateSpecificResults;

    public VoltageMonitoring(Crac crac, Network network, RaoResult raoResult) {
        this.crac = crac;
        this.network = network;
        this.raoResult = raoResult;
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel) {
        return runAndUpdateRaoResult(loadFlowProvider, loadFlowParameters, numberOfLoadFlowsInParallel, ReportNode.NO_OP);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an RaoResult enhanced with VoltageMonitoringResult
     */
    public RaoResult runAndUpdateRaoResult(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, ReportNode reportNode) {
        return new RaoResultWithVoltageMonitoring(raoResult, run(loadFlowProvider, loadFlowParameters, numberOfLoadFlowsInParallel, reportNode));
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an VoltageMonitoringResult
     */
    @Deprecated
    public VoltageMonitoringResult run(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel) {
        return run(loadFlowProvider, loadFlowParameters, numberOfLoadFlowsInParallel, ReportNode.NO_OP);
    }

    /**
     * Main function : runs VoltageMonitoring computation on all VoltageCnecs defined in the CRAC.
     * Returns an VoltageMonitoringResult
     */
    @Deprecated
    public VoltageMonitoringResult run(String loadFlowProvider, LoadFlowParameters loadFlowParameters, int numberOfLoadFlowsInParallel, ReportNode reportNode) {
        ReportNode voltageMonitoringReportNode = VoltageMonitoringReports.reportVoltageMonitoringStart(reportNode);
        stateSpecificResults = new ArrayList<>();

        if (crac.getVoltageCnecs().isEmpty()) {
            VoltageMonitoringReports.reportNoVoltageCnecsDefined(voltageMonitoringReportNode);
            stateSpecificResults.add(new VoltageMonitoringResult(Collections.emptyMap(), Collections.emptyMap(), VoltageMonitoringResult.Status.SECURE));
            return assembleVoltageMonitoringResults(voltageMonitoringReportNode);
        }

        // I) Preventive state
        if (Objects.nonNull(crac.getPreventiveState())) {
            applyOptimalRemedialActions(crac.getPreventiveState(), network);
            stateSpecificResults.add(monitorVoltageCnecsAndLog(loadFlowProvider, loadFlowParameters, crac.getPreventiveState(), network, voltageMonitoringReportNode));
        }

        // II) Curative states
        Set<State> contingencyStates = crac.getVoltageCnecs().stream().map(Cnec::getState).filter(state -> !state.isPreventive()).collect(Collectors.toSet());
        if (contingencyStates.isEmpty()) {
            return assembleVoltageMonitoringResults(voltageMonitoringReportNode);
        }

        try (AbstractNetworkPool networkPool = AbstractNetworkPool.create(network, network.getVariantManager().getWorkingVariantId(), Math.min(numberOfLoadFlowsInParallel, contingencyStates.size()), true)) {
            List<ForkJoinTask<ReportNode>> tasks = contingencyStates.stream().sorted(Comparator.comparing(State::getId)).map(state ->
                networkPool.submit(() -> runPostContingencyTask(loadFlowProvider, loadFlowParameters, state, networkPool))).toList();
            for (ForkJoinTask<ReportNode> task : tasks) {
                try {
                    voltageMonitoringReportNode.include(task.get());
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return assembleVoltageMonitoringResults(voltageMonitoringReportNode);
    }

    private ReportNode runPostContingencyTask(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, AbstractNetworkPool networkPool) throws InterruptedException {
        ReportNode rootReportNode = VoltageMonitoringReports.generatePostContingencyRootReportNode();
        ReportNode scenarioReportNode = VoltageMonitoringReports.reportPostContingencyTask(state, rootReportNode);

        Network networkClone = networkPool.getAvailableNetwork();
        try {
            Contingency contingency = state.getContingency().orElseThrow();
            if (!contingency.isValid(networkClone)) {
                throw new OpenRaoException("Unable to apply contingency " + contingency.getId());
            }
            contingency.toModification().apply(networkClone, (ComputationManager) null);
            applyOptimalRemedialActionsOnContingencyState(state, networkClone);
            stateSpecificResults.add(monitorVoltageCnecsAndLog(loadFlowProvider, loadFlowParameters, state, networkClone, scenarioReportNode));
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new OpenRaoException(CONTINGENCY_ERROR, e);
        }
        networkPool.releaseUsedNetwork(networkClone);
        return rootReportNode;
    }

    /**
     * Gathers optimal remedial actions retrieved from raoResult for a given state on network.
     * For curative states, consider auto (when they exist) and curative states.
     */
    private void applyOptimalRemedialActionsOnContingencyState(State state, Network networkClone) {
        if (state.getInstant().isCurative()) {
            Optional<Contingency> contingency = state.getContingency();
            if (contingency.isPresent()) {
                crac.getStates(contingency.get()).forEach(contingencyState ->
                    applyOptimalRemedialActions(state, networkClone));
            } else {
                throw new OpenRaoException(String.format("Curative state %s was defined without a contingency", state.getId()));

            }
        } else {
            applyOptimalRemedialActions(state, networkClone);
        }
    }

    /**
     * Applies optimal remedial actions retrieved from raoResult for a given state on network.
     */
    private void applyOptimalRemedialActions(State state, Network network) {
        raoResult.getActivatedNetworkActionsDuringState(state)
            .forEach(na -> na.apply(network));
        raoResult.getActivatedRangeActionsDuringState(state)
            .forEach(ra -> ra.apply(network, raoResult.getOptimizedSetPointOnState(state, ra)));
    }

    private VoltageMonitoringResult monitorVoltageCnecsAndLog(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, Network networkClone, ReportNode reportNode) {
        ReportNode voltageMonitoringAtStateReportNode = VoltageMonitoringReports.reportMonitoringVoltagesAtState(reportNode, state);
        VoltageMonitoringResult result = monitorVoltageCnecs(loadFlowProvider, loadFlowParameters, state, networkClone, voltageMonitoringAtStateReportNode);
        result.reportConstraints(voltageMonitoringAtStateReportNode);
        VoltageMonitoringReports.reportMonitoringVoltagesAtStateEnd(voltageMonitoringAtStateReportNode, state);
        return result;
    }

    /**
     * VoltageMonitoring computation on all VoltageCnecs in the CRAC for a given state.
     * Returns an VoltageMonitoringResult.
     */
    private VoltageMonitoringResult monitorVoltageCnecs(String loadFlowProvider, LoadFlowParameters loadFlowParameters, State state, Network networkClone, ReportNode reportNode) {
        //First load flow with only preventive action, it is supposed to converge
        if (!computeLoadFlow(loadFlowProvider, loadFlowParameters, networkClone, reportNode)) {
            return catchVoltageMonitoringResult(state, VoltageMonitoringResult.Status.UNKNOWN, reportNode);
        }
        // Check for threshold overshoot for the voltages of each cnec
        Set<RemedialAction<?>> appliedNetworkActions = new TreeSet<>(Comparator.comparing(RemedialAction::getId));
        Map<VoltageCnec, ExtremeVoltageValues> voltageValues = computeVoltages(crac.getVoltageCnecs(state), networkClone);
        for (Map.Entry<VoltageCnec, ExtremeVoltageValues> voltages : voltageValues.entrySet()) {
            VoltageCnec voltageCnec = voltages.getKey();
            //If there is a threshold overshoot, apply topological network action
            if (thresholdOvershoot(voltageCnec, voltages.getValue())) {
                Set<NetworkAction> availableNetworkActions = getVoltageCnecNetworkActions(state, voltageCnec, reportNode);
                appliedNetworkActions.addAll(applyTopologicalNetworkActions(networkClone, availableNetworkActions));
            }
        }
        // If some action were applied, recompute a loadflow. If the loadflow doesn't converge, it is unsecure
        if (!appliedNetworkActions.isEmpty() && !computeLoadFlow(loadFlowProvider, loadFlowParameters, networkClone, reportNode)) {
            String stateId = state.getId();
            MonitoringCommonReports.reportLoadflowComputationFailedAtStateAfterRA(reportNode, stateId);
            return new VoltageMonitoringResult(voltageValues, new HashMap<>(), VoltageMonitoringResult.getUnsecureStatus(voltageValues));
        }
        Map<State, Set<RemedialAction<?>>> appliedRa = new HashMap<>();
        if (!appliedNetworkActions.isEmpty()) {
            appliedRa.put(state, appliedNetworkActions);
        }
        VoltageMonitoringResult.Status status = VoltageMonitoringResult.Status.SECURE;
        //Check that with the curative action, the new voltage don't overshoot the threshold, else it is unsecure
        Map<VoltageCnec, ExtremeVoltageValues> newVoltageValues = computeVoltages(crac.getVoltageCnecs(state), networkClone);
        if (newVoltageValues.entrySet().stream().anyMatch(entrySet -> thresholdOvershoot(entrySet.getKey(), entrySet.getValue()))) {
            status = VoltageMonitoringResult.getUnsecureStatus(newVoltageValues);
        }
        return new VoltageMonitoringResult(newVoltageValues, appliedRa, status);
    }

    private Map<VoltageCnec, ExtremeVoltageValues> computeVoltages(Set<VoltageCnec> voltageCnecs, Network networkClone) {
        Map<VoltageCnec, ExtremeVoltageValues> voltagePerCnec = new HashMap<>();
        voltageCnecs.forEach(vc -> {
            VoltageLevel voltageLevel = networkClone.getVoltageLevel(vc.getNetworkElement().getId());
            Set<Double> voltages = null;
            if (voltageLevel != null) {
                voltages = voltageLevel.getBusView().getBusStream().map(Bus::getV).collect(Collectors.toSet());
            }
            BusbarSection busbarSection = networkClone.getBusbarSection(vc.getNetworkElement().getId());
            if (busbarSection != null) {
                Double busBarVoltages = busbarSection.getV();
                voltages = new HashSet<>();
                voltages.add(busBarVoltages);
            }
            if (voltageLevel != null) {
                voltagePerCnec.put(vc, new ExtremeVoltageValues(voltages));
            }
        });
        return voltagePerCnec;
    }

    /**
     * Compares an voltageCnec's thresholds to a voltage (parameter).
     * Returns true if a threshold is breached.
     */
    private static boolean thresholdOvershoot(VoltageCnec voltageCnec, ExtremeVoltageValues voltages) {
        return voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMax() && voltages != null && voltages.getMax() > threshold.max().orElseThrow())
            || voltageCnec.getThresholds().stream()
            .anyMatch(threshold -> threshold.limitsByMin() && voltages != null && voltages.getMin() < threshold.min().orElseThrow());
    }

    /**
     * Retrieves the network actions that were defined for a VoltageCnec (parameter) in a given state (parameter).
     * Preventive network actions are filtered.
     */
    private Set<NetworkAction> getVoltageCnecNetworkActions(State state, VoltageCnec voltageCnec, ReportNode reportNode) {
        Set<RemedialAction<?>> availableRemedialActions =
            crac.getRemedialActions().stream()
                .filter(remedialAction ->
                    remedialAction.getUsageRules().stream().filter(OnConstraint.class::isInstance)
                        .map(OnConstraint.class::cast)
                        .anyMatch(onVoltageConstraint -> onVoltageConstraint.getCnec().equals(voltageCnec)))
                .collect(Collectors.toSet());
        if (availableRemedialActions.isEmpty()) {
            VoltageMonitoringReports.reportNoRaAvailable(reportNode, voltageCnec.getId(), state.getId());
            return Collections.emptySet();
        }
        if (state.isPreventive()) {
            VoltageMonitoringReports.reportConstraintInPreventive(reportNode, voltageCnec.getId());
            return Collections.emptySet();
        }
        // Convert remedial actions to network actions
        Set<NetworkAction> networkActions = availableRemedialActions.stream().filter(remedialAction -> {
            if (remedialAction instanceof NetworkAction) {
                return true;
            } else {
                VoltageMonitoringReports.reportIgnoredRemedialAction(reportNode, remedialAction.getId(), voltageCnec.getId(), state.getId());
                return false;
            }
        }).map(NetworkAction.class::cast).collect(Collectors.toSet());
        String cnecId = voltageCnec.getId();
        String appliedRasList = networkActions.stream().map(Identifiable::getId).collect(Collectors.joining(", "));
        MonitoringCommonReports.reportAppliedNetworkActions(reportNode, cnecId, appliedRasList);
        return networkActions;
    }

    /**
     * Apply any topological network action
     *
     * @param networkClone
     * @param availableNetworkActions
     * @return the set of applied network action
     */
    private Set<NetworkAction> applyTopologicalNetworkActions(Network networkClone, Set<NetworkAction> availableNetworkActions) {
        Set<NetworkAction> topologicalNetworkActionsAdded = new HashSet<>();
        for (NetworkAction na : availableNetworkActions) {
            na.apply(networkClone);
            topologicalNetworkActionsAdded.add(na);
        }
        return topologicalNetworkActionsAdded;
    }

    /**
     * Runs a LoadFlow computation
     * Returns false if loadFlow has not converged.
     */
    private boolean computeLoadFlow(String loadFlowProvider, LoadFlowParameters loadFlowParameters, Network networkClone, ReportNode reportNode) {
        ReportNode loadflowReportNode = MonitoringCommonReports.reportLoadFlowComputationStart(reportNode);
        LoadFlowResult loadFlowResult = LoadFlow.find(loadFlowProvider)
            .run(networkClone, networkClone.getVariantManager().getWorkingVariantId(), LocalComputationManager.getDefault(), loadFlowParameters, loadflowReportNode);
        if (!loadFlowResult.isOk()) {
            MonitoringCommonReports.reportLoadFlowError(loadflowReportNode);
        }
        MonitoringCommonReports.reportLoadFlowComputationEnd(loadflowReportNode);
        return loadFlowResult.isOk();
    }

    /**
     * Assembles all VoltageMonitoringResults computed.
     * Individual VoltageResults and appliedCras maps are concatenated.
     * Global status :
     * - SECURE if all VoltageMonitoringResults are SECURE.
     * - HIGH_AND_LOW_VOLTAGE_CONSTRAINT if any AngleMonitoringResult is HIGH_AND_LOW_VOLTAGE_CONSTRAINT
     * or if an AngleMonitoringResult is LOW_VOLTAGE_CONSTRAINT and another is HIGH_VOLTAGE_CONSTRAINT.
     * - HIGH/LOW_VOLTAGE_CONSTRAINT if any AngleMonitoringResult is HIGH/LOW_VOLTAGE_CONSTRAINT.
     * - UNKNOWN if any AngleMonitoringResult is UNKNOWN and no AngleMonitoringResult is UNSECURE.
     */
    private VoltageMonitoringResult assembleVoltageMonitoringResults(ReportNode reportNode) {
        Map<VoltageCnec, ExtremeVoltageValues> extremeVoltageValuesMap = new HashMap<>();
        Map<State, Set<RemedialAction<?>>> appliedRas = new HashMap<>();
        stateSpecificResults.forEach(s -> {
            extremeVoltageValuesMap.putAll(s.getExtremeVoltageValues());
            appliedRas.putAll(s.getAppliedRas());
        });
        VoltageMonitoringResult.Status securityStatus = concatenateSpecificResults();
        VoltageMonitoringResult result = new VoltageMonitoringResult(extremeVoltageValuesMap, appliedRas, securityStatus);
        result.reportConstraints(reportNode);
        VoltageMonitoringReports.reportVoltageMonitoringEnd(reportNode);
        return result;
    }

    private VoltageMonitoringResult.Status concatenateSpecificResults() {
        if (stateSpecificResults.isEmpty()) {
            return VoltageMonitoringResult.Status.UNKNOWN;
        }
        AtomicBoolean atLeastOneHigh = new AtomicBoolean(false);
        AtomicBoolean atLeastOneLow = new AtomicBoolean(false);
        AtomicBoolean atLeastOneUnknown = new AtomicBoolean(false);

        stateSpecificResults.forEach(result -> {
                switch (result.getStatus()) {
                    case HIGH_VOLTAGE_CONSTRAINT -> atLeastOneHigh.set(true);
                    case LOW_VOLTAGE_CONSTRAINT -> atLeastOneLow.set(true);
                    case HIGH_AND_LOW_VOLTAGE_CONSTRAINTS -> {
                        atLeastOneHigh.set(true);
                        atLeastOneLow.set(true);
                    }
                    case UNKNOWN -> atLeastOneUnknown.set(true);
                    case SECURE -> { }
                }
            }
        );

        if (atLeastOneHigh.get() && atLeastOneLow.get()) {
            return VoltageMonitoringResult.Status.HIGH_AND_LOW_VOLTAGE_CONSTRAINTS;
        }
        if (atLeastOneHigh.get()) {
            return VoltageMonitoringResult.Status.HIGH_VOLTAGE_CONSTRAINT;
        }
        if (atLeastOneLow.get()) {
            return VoltageMonitoringResult.Status.LOW_VOLTAGE_CONSTRAINT;
        }
        if (atLeastOneUnknown.get()) {
            return VoltageMonitoringResult.Status.UNKNOWN;
        }
        return VoltageMonitoringResult.Status.SECURE;
    }

    private VoltageMonitoringResult catchVoltageMonitoringResult(State state, VoltageMonitoringResult.Status securityStatus, ReportNode reportNode) {
        String stateId = state.getId();
        MonitoringCommonReports.reportLoadFlowComputationFailureAtState(reportNode, stateId);
        Map<VoltageCnec, ExtremeVoltageValues> voltagePerCnec = new HashMap<>();
        crac.getVoltageCnecs(state).forEach(vc ->
            voltagePerCnec.put(vc, new ExtremeVoltageValues(new HashSet<>(Arrays.asList(Double.NaN))))
        );
        return new VoltageMonitoringResult(voltagePerCnec, new HashMap<>(), securityStatus);
    }
}
