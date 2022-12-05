/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.RangeActionSensiHandler;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityResult {

    private static class StateResult {
        private SensitivityComputationStatus status;
        private final Map<String, Map<Side, Double>> referenceFlows = new HashMap<>();
        private final Map<String, Map<Side, Double>> referenceIntensities = new HashMap<>();
        private final Map<String, Map<String, Map<Side, Double>>> flowSensitivities = new HashMap<>();
        private final Map<String, Map<String, Map<Side, Double>>> intensitySensitivities = new HashMap<>();

        private SensitivityComputationStatus getStatus() {
            return status;
        }

        private Map<String, Map<Side, Double>> getReferenceFlows() {
            return referenceFlows;
        }

        private Map<String, Map<Side, Double>> getReferenceIntensities() {
            return referenceIntensities;
        }

        private Map<String, Map<String, Map<Side, Double>>> getFlowSensitivities() {
            return flowSensitivities;
        }

        private Map<String, Map<String, Map<Side, Double>>> getIntensitySensitivities() {
            return intensitySensitivities;
        }
    }

    public enum SensitivityComputationStatus {
        SUCCESS,
        FALLBACK,
        FAILURE
    }

    private SensitivityComputationStatus status;
    private final StateResult nStateResult = new StateResult();
    private final Map<Instant, Map<String, StateResult>> postContingencyResults = new EnumMap<>(Instant.class);

    public SystematicSensitivityResult() {
        this.status = SensitivityComputationStatus.SUCCESS;
        this.postContingencyResults.put(Instant.OUTAGE, new HashMap<>());
        this.postContingencyResults.put(Instant.AUTO, new HashMap<>());
        this.postContingencyResults.put(Instant.CURATIVE, new HashMap<>());
    }

    public SystematicSensitivityResult completeData(SensitivityAnalysisResult results, Instant instant) {

        if (results == null) {
            this.status = SensitivityComputationStatus.FAILURE;
            return this;
        }
        // status set to failure initially, and set to success if we find at least one non NaN value
        this.status = SensitivityComputationStatus.FAILURE;

        results.getPreContingencyValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult, results.getFactors(), SensitivityAnalysisResult.Status.SUCCESS));
        for (SensitivityAnalysisResult.SensitivityContingencyStatus contingencyStatus : results.getContingencyStatuses()) {
            StateResult contingencyStateResult = new StateResult();
            contingencyStateResult.status = contingencyStatus.getStatus().equals(SensitivityAnalysisResult.Status.FAILURE) ? SensitivityComputationStatus.FAILURE : SensitivityComputationStatus.SUCCESS;
            results.getValues(contingencyStatus.getContingencyId()).forEach(sensitivityValue ->
                fillIndividualValue(sensitivityValue, contingencyStateResult, results.getFactors(), contingencyStatus.getStatus())
            );
            postContingencyResults.get(instant).put(contingencyStatus.getContingencyId(), contingencyStateResult);
        }
        nStateResult.status = this.status;
        return this;
    }

    public SystematicSensitivityResult postTreatIntensities() {
        postTreatIntensitiesOnState(nStateResult);
        postContingencyResults.values().forEach(map -> map.values().forEach(this::postTreatIntensitiesOnState));
        return this;
    }

    /**
     * Sensitivity providers return absolute values for intensities
     * In case flows are negative, we shall replace this value by its opposite
     */
    private void postTreatIntensitiesOnState(StateResult stateResult) {
        stateResult.getReferenceFlows()
            .forEach((neId, sideAndFlow) -> {
                if (stateResult.getReferenceIntensities().containsKey(neId)) {
                    sideAndFlow.forEach((side, flow) -> {
                        if (flow < 0) {
                            stateResult.getReferenceIntensities().get(neId).put(side, -stateResult.getReferenceIntensities().get(neId).get(side));
                        }
                    });
                }
                if (stateResult.getIntensitySensitivities().containsKey(neId)) {
                    sideAndFlow.forEach((side, flow) -> {
                        if (flow < 0) {
                            Map<String, Map<Side, Double>> sensitivities = stateResult.getIntensitySensitivities().get(neId);
                            sensitivities.forEach((actionId, sideToSensi) -> sensitivities.get(actionId).put(side, -sideToSensi.get(side)));
                        }
                    });
                }
            });
    }

    public SystematicSensitivityResult postTreatHvdcs(Network network, Map<String, HvdcRangeAction> hvdcRangeActions) {
        postTreatHvdcsOnState(network, hvdcRangeActions, nStateResult);
        postContingencyResults.get(Instant.OUTAGE).values().forEach(stateResult -> postTreatHvdcsOnState(network, hvdcRangeActions, stateResult));
        postContingencyResults.get(Instant.AUTO).values().forEach(stateResult -> postTreatHvdcsOnState(network, hvdcRangeActions, stateResult));
        postContingencyResults.get(Instant.CURATIVE).values().forEach(stateResult -> postTreatHvdcsOnState(network, hvdcRangeActions, stateResult));
        return this;
    }

    private void postTreatHvdcsOnState(Network network, Map<String, HvdcRangeAction> hvdcRangeActions, StateResult stateResult) {
        hvdcRangeActions.forEach((networkElementId, hvdcRangeAction) -> {
            HvdcLine hvdcLine = network.getHvdcLine(networkElementId);
            if (hvdcLine.getConvertersMode() == HvdcLine.ConvertersMode.SIDE_1_INVERTER_SIDE_2_RECTIFIER) {
                stateResult.getFlowSensitivities().forEach((cnecId, cnecFlowSensis) -> {
                    if (cnecFlowSensis.containsKey(networkElementId)) {
                        cnecFlowSensis.put(networkElementId, invertMapValues(cnecFlowSensis.get(networkElementId)));
                    }
                });
                stateResult.getIntensitySensitivities().forEach((cnecId, cnecIntensitySensis) -> {
                    if (cnecIntensitySensis.containsKey(networkElementId)) {
                        cnecIntensitySensis.put(networkElementId, invertMapValues(cnecIntensitySensis.get(networkElementId)));
                    }
                });
            }
        });
    }

    private Map<Side, Double> invertMapValues(Map<Side, Double> map) {
        Map<Side, Double> invertedMap = new EnumMap<>(Side.class);
        map.forEach((key, value) -> invertedMap.put(key, -value));
        return invertedMap;
    }

    private void fillIndividualValue(SensitivityValue value, StateResult stateResult, List<SensitivityFactor> factors, SensitivityAnalysisResult.Status status) {
        double reference = status.equals(SensitivityAnalysisResult.Status.FAILURE) ? Double.NaN : value.getFunctionReference();
        double sensitivity = status.equals(SensitivityAnalysisResult.Status.FAILURE) ? Double.NaN : value.getValue();
        SensitivityFactor factor = factors.get(value.getFactorIndex());

        if (!Double.isNaN(reference) && !Double.isNaN(sensitivity)) {
            this.status = SensitivityComputationStatus.SUCCESS;
        }
        if (Double.isNaN(reference) && status != SensitivityAnalysisResult.Status.FAILURE) {
            reference = 0;
            sensitivity = 0;
        }

        Side side = null;
        double activePowerCoefficient = 0;
        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1)) {
            side = Side.LEFT;
            activePowerCoefficient = 1;
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            side = Side.RIGHT;
            activePowerCoefficient = -1; // FARAO always considers flows as seen from Side 1. Sensitivity providers invert side flows.
        }

        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2)) {
            stateResult.getReferenceFlows()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, reference * activePowerCoefficient);
            stateResult.getFlowSensitivities()
                .computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .computeIfAbsent(factor.getVariableId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, sensitivity * activePowerCoefficient);
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            stateResult.getReferenceIntensities()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, reference);
        }
    }

    public boolean isSuccess() {
        return status != SensitivityComputationStatus.FAILURE;
    }

    public boolean isOnePerimeterInFailure() {
        if (nStateResult.getStatus() == SensitivityComputationStatus.FAILURE) {
            return true;
        }
        return postContingencyResults.values().stream().flatMap(stringStateResultMap -> stringStateResultMap.values().stream())
            .anyMatch(stateResult -> stateResult.getStatus() == SensitivityComputationStatus.FAILURE);
    }

    public SensitivityComputationStatus getStatus() {
        return status;
    }

    public SensitivityComputationStatus getStatus(State state) {
        Optional<Contingency> optionalContingency = state.getContingency();
        if (optionalContingency.isPresent()) {
            if (postContingencyResults.containsKey(state.getInstant()) && postContingencyResults.get(state.getInstant()).containsKey(optionalContingency.get().getId())) {
                return postContingencyResults.get(state.getInstant()).get(optionalContingency.get().getId()).getStatus();
            } else if (postContingencyResults.containsKey(Instant.OUTAGE) && postContingencyResults.get(Instant.OUTAGE).containsKey(optionalContingency.get().getId())) {
                return postContingencyResults.get(Instant.OUTAGE).get(optionalContingency.get().getId()).getStatus();
            } else {
                return SensitivityComputationStatus.FAILURE;
            }
        } else {
            return nStateResult.getStatus();
        }
    }

    public void setStatus(SensitivityComputationStatus status) {
        this.status = status;
    }

    public Set<String> getContingencies() {
        return postContingencyResults.values().stream().flatMap(contingencyResult -> contingencyResult.keySet().stream()).collect(Collectors.toSet());
    }

    public double getReferenceFlow(FlowCnec cnec, Side side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getReferenceFlows().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getReferenceIntensity(FlowCnec cnec, Side side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getReferenceIntensities().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getSensitivityOnFlow(RangeAction<?> rangeAction, FlowCnec cnec, Side side) {
        return RangeActionSensiHandler.get(rangeAction).getSensitivityOnFlow(cnec, side, this);
    }

    public double getSensitivityOnFlow(SensitivityVariableSet glsk, FlowCnec cnec, Side side) {
        return getSensitivityOnFlow(glsk.getId(), cnec, side);
    }

    public double getSensitivityOnFlow(String variableId, FlowCnec cnec, Side side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId) ||
                !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).get(side);
    }

    private StateResult getCnecStateResult(Cnec<?> cnec) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            List<Instant> possibleInstants = postContingencyResults.keySet().stream()
                    .filter(instant -> instant.comesBefore(cnec.getState().getInstant()) || instant.equals(cnec.getState().getInstant()))
                    .sorted(Comparator.comparingInt(instant -> -instant.getOrder()))
                    .collect(Collectors.toList());
            for (Instant instant : possibleInstants) {
                // Use latest sensi computed on the cnec's contingency amidst the last instants before cnec state.
                if (postContingencyResults.get(instant).containsKey(optionalContingency.get().getId())) {
                    return postContingencyResults.get(instant).get(optionalContingency.get().getId());
                }
            }
            return null;
        } else {
            return nStateResult;
        }
    }
}
