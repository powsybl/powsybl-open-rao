/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.sensitivityanalysis.rasensihandler.RangeActionSensiHandler;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityResult {

    private static class StateResult {
        private SensitivityComputationStatus status;
        private final Map<String, Map<TwoSides, Double>> referenceFlows = new HashMap<>();
        private final Map<String, Map<TwoSides, Double>> referenceIntensities = new HashMap<>();
        private final Map<String, Map<String, Map<TwoSides, Double>>> flowSensitivities = new HashMap<>();
        private final Map<String, Map<String, Map<TwoSides, Double>>> intensitySensitivities = new HashMap<>();

        private SensitivityComputationStatus getSensitivityComputationStatus() {
            return status;
        }

        private Map<String, Map<TwoSides, Double>> getReferenceFlows() {
            return referenceFlows;
        }

        private Map<String, Map<TwoSides, Double>> getReferenceIntensities() {
            return referenceIntensities;
        }

        private Map<String, Map<String, Map<TwoSides, Double>>> getFlowSensitivities() {
            return flowSensitivities;
        }

        private Map<String, Map<String, Map<TwoSides, Double>>> getIntensitySensitivities() {
            return intensitySensitivities;
        }
    }

    public enum SensitivityComputationStatus {
        SUCCESS,
        PARTIAL_FAILURE,
        FAILURE
    }

    private SensitivityComputationStatus status;
    private final StateResult nStateResult = new StateResult();
    private final Map<Integer, Map<String, StateResult>> postContingencyResults = new HashMap<>();

    private final Map<Cnec<?>, StateResult> memoizedStateResultPerCnec = new ConcurrentHashMap<>();

    public SystematicSensitivityResult() {
        this.status = SensitivityComputationStatus.SUCCESS;
    }

    public SystematicSensitivityResult(SensitivityComputationStatus status) {
        this.status = status;
    }

    public SystematicSensitivityResult completeData(SensitivityAnalysisResult results, Integer instantOrder) {
        postContingencyResults.putIfAbsent(instantOrder, new HashMap<>());
        // status set to failure initially, and set to success if we find at least one non NaN value
        this.status = SensitivityComputationStatus.FAILURE;
        if (results == null) {
            return this;
        }

        boolean anyContingencyFailure = false;

        results.getPreContingencyValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult, results.getFactors(), SensitivityAnalysisResult.Status.SUCCESS));
        for (SensitivityAnalysisResult.SensitivityContingencyStatus contingencyStatus : results.getContingencyStatuses()) {
            if (contingencyStatus.getStatus() == SensitivityAnalysisResult.Status.FAILURE) {
                anyContingencyFailure = true;
            }
            StateResult contingencyStateResult = new StateResult();
            contingencyStateResult.status = contingencyStatus.getStatus().equals(SensitivityAnalysisResult.Status.FAILURE) ? SensitivityComputationStatus.FAILURE : SensitivityComputationStatus.SUCCESS;
            results.getValues(contingencyStatus.getContingencyId()).forEach(sensitivityValue ->
                fillIndividualValue(sensitivityValue, contingencyStateResult, results.getFactors(), contingencyStatus.getStatus())
            );
            postContingencyResults.get(instantOrder).put(contingencyStatus.getContingencyId(), contingencyStateResult);
        }

        nStateResult.status = this.status;

        if (nStateResult.status != SensitivityComputationStatus.FAILURE && anyContingencyFailure) {
            this.status = SensitivityComputationStatus.PARTIAL_FAILURE;
        }
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
                            Map<String, Map<TwoSides, Double>> sensitivities = stateResult.getIntensitySensitivities().get(neId);
                            sensitivities.forEach((actionId, sideToSensi) -> sensitivities.get(actionId).put(side, -sideToSensi.get(side)));
                        }
                    });
                }
            });
    }

    public SystematicSensitivityResult postTreatHvdcs(Network network, Map<String, HvdcRangeAction> hvdcRangeActions) {
        postTreatHvdcsOnState(network, hvdcRangeActions, nStateResult);
        postContingencyResults.values().forEach(stringStateResultMap ->
            stringStateResultMap.values().forEach(stateResult -> postTreatHvdcsOnState(network, hvdcRangeActions, stateResult))
        );
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

    private Map<TwoSides, Double> invertMapValues(Map<TwoSides, Double> map) {
        Map<TwoSides, Double> invertedMap = new EnumMap<>(TwoSides.class);
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

        TwoSides side = null;
        double activePowerCoefficient = 0;
        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1)) {
            side = TwoSides.ONE;
            activePowerCoefficient = 1;
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            side = TwoSides.TWO;
            activePowerCoefficient = -1; // Open RAO always considers flows as seen from Side 1. Sensitivity providers invert side flows.
        }

        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2)) {
            stateResult.getReferenceFlows()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(TwoSides.class))
                .putIfAbsent(side, reference * activePowerCoefficient);
            stateResult.getFlowSensitivities()
                .computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .computeIfAbsent(factor.getVariableId(), k -> new EnumMap<>(TwoSides.class))
                .putIfAbsent(side, sensitivity * activePowerCoefficient);
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            stateResult.getReferenceIntensities()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(TwoSides.class))
                .putIfAbsent(side, reference);
        }
    }

    public boolean isSuccess() {
        return status != SensitivityComputationStatus.FAILURE;
    }

    public SensitivityComputationStatus getStatus() {
        return status;
    }

    public SensitivityComputationStatus getStatus(State state) {
        Optional<Contingency> optionalContingency = state.getContingency();
        if (optionalContingency.isPresent()) {
            List<Integer> possibleInstants = postContingencyResults.keySet().stream()
                    .filter(instantOrder -> instantOrder <= state.getInstant().getOrder())
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Integer instantOrder : possibleInstants) {
                // Use latest sensi computed on state
                if (postContingencyResults.get(instantOrder).containsKey(optionalContingency.get().getId())) {
                    return postContingencyResults.get(instantOrder).get(optionalContingency.get().getId()).getSensitivityComputationStatus();
                }
            }
            return SensitivityComputationStatus.FAILURE;
        } else {
            return nStateResult.getSensitivityComputationStatus();
        }
    }

    public void setStatus(SensitivityComputationStatus status) {
        this.status = status;
    }

    public Set<String> getContingencies() {
        return postContingencyResults.values().stream().flatMap(contingencyResult -> contingencyResult.keySet().stream()).collect(Collectors.toSet());
    }

    public double getReferenceFlow(FlowCnec cnec, TwoSides side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getReferenceFlows().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getReferenceFlow(FlowCnec cnec, TwoSides side, Instant instant) {
        StateResult stateResult = getCnecStateResult(cnec, instant);
        if (stateResult == null ||
            !stateResult.getReferenceFlows().containsKey(cnec.getNetworkElement().getId()) ||
            !stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getReferenceIntensity(FlowCnec cnec, TwoSides side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getReferenceIntensities().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getReferenceIntensity(FlowCnec cnec, TwoSides side, Instant instant) {
        StateResult stateResult = getCnecStateResult(cnec, instant);
        if (stateResult == null ||
            !stateResult.getReferenceIntensities().containsKey(cnec.getNetworkElement().getId()) ||
            !stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).get(side);
    }

    public double getSensitivityOnFlow(RangeAction<?> rangeAction, FlowCnec cnec, TwoSides side) {
        return RangeActionSensiHandler.get(rangeAction).getSensitivityOnFlow(cnec, side, this);
    }

    public double getSensitivityOnFlow(SensitivityVariableSet glsk, FlowCnec cnec, TwoSides side) {
        return getSensitivityOnFlow(glsk.getId(), cnec, side);
    }

    public double getSensitivityOnFlow(String variableId, FlowCnec cnec, TwoSides side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
                !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId()) ||
                !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId) ||
                !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).get(side);
    }

    public double getSensitivityOnFlow(String variableId, FlowCnec cnec, TwoSides side, Instant instant) {
        StateResult stateResult = getCnecStateResult(cnec, instant);
        if (stateResult == null ||
            !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId()) ||
            !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId) ||
            !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).containsKey(side)) {
            return 0.0;
        }
        return stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId).get(side);
    }

    private StateResult getCnecStateResult(Cnec<?> cnec) {
        if (memoizedStateResultPerCnec.containsKey(cnec)) {
            return memoizedStateResultPerCnec.get(cnec);
        }
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            List<Integer> possibleInstants = postContingencyResults.keySet().stream()
                    .filter(instantOrder -> instantOrder <= cnec.getState().getInstant().getOrder())
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Integer instantOrder : possibleInstants) {
                // Use latest sensi computed on the cnec's contingency amidst the last instants before cnec state.
                String contingencyId = optionalContingency.get().getId();
                if (postContingencyResults.get(instantOrder).containsKey(contingencyId)) {
                    memoizedStateResultPerCnec.put(cnec, postContingencyResults.get(instantOrder).get(contingencyId));
                    return memoizedStateResultPerCnec.get(cnec);
                }
            }
            return null;
        } else {
            return nStateResult;
        }
    }

    private StateResult getCnecStateResult(Cnec<?> cnec, Instant instant) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            int maxAdmissibleInstantOrder = instant == null ? 1 : Math.max(1, instant.getOrder()); // when dealing with post-contingency CNECs, a null instant refers to the outage instant
            List<Integer> possibleInstants = postContingencyResults.keySet().stream()
                .filter(instantOrder -> instantOrder <= cnec.getState().getInstant().getOrder() && instantOrder <= maxAdmissibleInstantOrder)
                .sorted(Comparator.reverseOrder())
                .toList();
            String contingencyId = optionalContingency.get().getId();
            return possibleInstants.isEmpty() ? null : postContingencyResults.get(possibleInstants.get(0)).get(contingencyId);
        } else {
            return nStateResult; // when dealing with preventive CNECs, a null instant refers to the initial instant
        }
    }
}
