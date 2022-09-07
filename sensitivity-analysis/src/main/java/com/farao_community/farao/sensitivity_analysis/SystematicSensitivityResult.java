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
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.RangeActionSensiHandler;
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
        for (com.powsybl.contingency.Contingency contingency : results.getContingencies()) {
            StateResult contingencyStateResult = new StateResult();
            results.getValues(contingency.getId()).forEach(sensitivityValue ->
                fillIndividualValue(sensitivityValue, contingencyStateResult, results.getFactors())
            );
            postContingencyResults.get(instant).put(contingency.getId(), contingencyStateResult);
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
        Set.of(Side.LEFT, Side.RIGHT).forEach(side -> {
            stateResult.getReferenceIntensities().forEach((cnecId, sideToIntensity) -> {
                if (stateResult.getReferenceFlows().containsKey(cnecId)
                    && stateResult.getReferenceFlows().get(cnecId).containsKey(side)
                    && stateResult.getReferenceFlows().get(cnecId).get(side) < 0) {
                    stateResult.getReferenceIntensities().computeIfAbsent(cnecId, k -> new EnumMap<>(Side.class)).put(side, -sideToIntensity.get(side));
                }
            });
            stateResult.getIntensitySensitivities().forEach((cnecId, sensitivities) -> {
                if (stateResult.getReferenceFlows().containsKey(cnecId)
                    && stateResult.getReferenceFlows().get(cnecId).containsKey(side)
                    && stateResult.getReferenceFlows().get(cnecId).get(side) < 0) {
                    sensitivities.forEach((actionId, sideToSensi) -> sensitivities.computeIfAbsent(actionId, k -> new EnumMap<>(Side.class)).put(side, -sideToSensi.get(side)));
                }
            });
        });
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
        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1)) {
            side = Side.LEFT;
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            side = Side.RIGHT;
        }

        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2)) {
            stateResult.getReferenceFlows()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, reference);
            stateResult.getFlowSensitivities()
                .computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .computeIfAbsent(factor.getVariableId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, sensitivity);
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            stateResult.getReferenceIntensities()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, reference);
            stateResult.getIntensitySensitivities()
                .computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .computeIfAbsent(factor.getVariableId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, sensitivity);
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
