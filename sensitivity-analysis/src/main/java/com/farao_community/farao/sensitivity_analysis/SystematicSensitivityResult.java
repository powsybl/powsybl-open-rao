/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
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

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityResult {

    private static class StateResult {
        private final Map<String, Map<Side, Double>> referenceFlows = new HashMap<>();
        private final Map<String, Map<Side, Double>> referenceIntensities = new HashMap<>();
        private final Map<String, Map<String, Map<Side, Double>>> flowSensitivities = new HashMap<>();
        private final Map<String, Map<String, Map<Side, Double>>> intensitySensitivities = new HashMap<>();

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

        results.getPreContingencyValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult, results.getFactors()));
        for (SensitivityAnalysisResult.SensitivityContingencyStatus contingencyStatus : results.getContingencyStatuses()) {
            StateResult contingencyStateResult = new StateResult();
            results.getValues(contingencyStatus.getContingencyId()).forEach(sensitivityValue ->
                fillIndividualValue(sensitivityValue, contingencyStateResult, results.getFactors())
            );
            postContingencyResults.get(instant).put(contingencyStatus.getContingencyId(), contingencyStateResult);
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
                        if (flow < 0
                            && stateResult.getReferenceFlows().get(neId).containsKey(side)
                            && stateResult.getReferenceFlows().get(neId).get(side) < 0) {
                            stateResult.getReferenceIntensities().get(neId).put(side, -stateResult.getReferenceIntensities().get(neId).get(side));
                        }
                    });
                }
                if (stateResult.getIntensitySensitivities().containsKey(neId)) {
                    sideAndFlow.forEach((side, flow) -> {
                        if (flow < 0
                            && stateResult.getReferenceFlows().get(neId).containsKey(side)
                            && stateResult.getReferenceFlows().get(neId).get(side) < 0) {
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

    private void fillIndividualValue(SensitivityValue value, StateResult stateResult, List<SensitivityFactor> factors) {
        double reference = value.getFunctionReference();
        double sensitivity = value.getValue();
        SensitivityFactor factor = factors.get(value.getFactorIndex());

        if (Double.isNaN(reference) || Double.isNaN(sensitivity)) {
            reference = 0.;
        } else {
            this.status = SensitivityComputationStatus.SUCCESS;
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

    public SensitivityComputationStatus getStatus() {
        return status;
    }

    public void setStatus(SensitivityComputationStatus status) {
        this.status = status;
    }

    public double getReferenceFlow(FlowCnec cnec, Side side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
            !stateResult.getReferenceFlows().containsKey(cnec.getNetworkElement().getId())) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().get(cnec.getNetworkElement().getId()).getOrDefault(side, 0.0);
    }

    public double getReferenceIntensity(FlowCnec cnec, Side side) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
            !stateResult.getReferenceIntensities().containsKey(cnec.getNetworkElement().getId())) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().get(cnec.getNetworkElement().getId()).getOrDefault(side, 0.0);
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
            !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId)) {
            return 0.0;
        }
        Map<Side, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).get(variableId);
        return sensitivities.getOrDefault(side, 0.0);
    }

    private StateResult getCnecStateResult(Cnec<?> cnec) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            if (postContingencyResults.containsKey(cnec.getState().getInstant()) && postContingencyResults.get(cnec.getState().getInstant()).containsKey(optionalContingency.get().getId())) {
                return postContingencyResults.get(cnec.getState().getInstant()).get(optionalContingency.get().getId());
            } else {
                return postContingencyResults.get(Instant.OUTAGE).get(optionalContingency.get().getId());
            }
        } else {
            return nStateResult;
        }
    }
}
