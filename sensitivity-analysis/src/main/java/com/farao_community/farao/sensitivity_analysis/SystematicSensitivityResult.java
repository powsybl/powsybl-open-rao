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
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.RangeActionSensiHandler;
import com.powsybl.sensitivity.*;

import java.util.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityResult {

    private static class StateResult {
        private final Map<String, Double> referenceFlows = new HashMap<>();
        private final Map<String, Double> referenceIntensities = new HashMap<>();
        private final Map<String, Map<String, Double>> flowSensitivities = new HashMap<>();
        private final Map<String, Map<String, Double>> intensitySensitivities = new HashMap<>();

        private Map<String, Double> getReferenceFlows() {
            return referenceFlows;
        }

        private Map<String, Double> getReferenceIntensities() {
            return referenceIntensities;
        }

        private Map<String, Map<String, Double>> getFlowSensitivities() {
            return flowSensitivities;
        }

        private Map<String, Map<String, Double>> getIntensitySensitivities() {
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
    private final Map<String, StateResult> postContingencyResults = new HashMap<>();
    private final Map<String, StateResult> postCraResults = new HashMap<>();

    public SystematicSensitivityResult() {
        this.status = SensitivityComputationStatus.SUCCESS;
    }

    public SystematicSensitivityResult completeData(SensitivityAnalysisResult results, boolean afterCra) {

        if (results == null) {
            this.status = SensitivityComputationStatus.FAILURE;
            return this;
        }
        // status set to failure initially, and set to success if we find at least one non NaN value;
        this.status =  SensitivityComputationStatus.FAILURE;

        Map<String, StateResult> contingencyResultsToFill = afterCra ? postCraResults : postContingencyResults;
        results.getPreContingencyValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult, results.getFactors()));
        results.getContingencies().forEach(contingency -> {
            StateResult contingencyStateResult = new StateResult();
            results.getValues(contingency.getId()).forEach(sensitivityValue ->
                fillIndividualValue(sensitivityValue, contingencyStateResult, results.getFactors())
            );
            contingencyResultsToFill.put(contingency.getId(), contingencyStateResult);
        });

        return this;
    }

    public SystematicSensitivityResult postTreatIntensities() {
        postTreatIntensitiesOnState(nStateResult);
        postContingencyResults.values().forEach(this::postTreatIntensitiesOnState);
        postCraResults.values().forEach(this::postTreatIntensitiesOnState);
        return this;
    }

    private void postTreatIntensitiesOnState(StateResult stateResult) {
        stateResult.getReferenceIntensities().forEach((cnecId, value) -> {
            if (stateResult.getReferenceFlows().containsKey(cnecId) && stateResult.getReferenceFlows().get(cnecId) < 0) {
                stateResult.getReferenceIntensities().put(cnecId, -value);
            }
        });
        stateResult.getIntensitySensitivities().forEach((cnecId, sensitivities) -> {
            if (stateResult.getReferenceFlows().containsKey(cnecId) && stateResult.getReferenceFlows().get(cnecId) < 0) {
                sensitivities.forEach((actionId, sensi) -> sensitivities.put(actionId, -sensi));
            }
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

        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER)) {
            stateResult.getReferenceFlows().putIfAbsent(factor.getFunctionId(), reference);
            stateResult.getFlowSensitivities().computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .putIfAbsent(factor.getVariableId(), sensitivity);
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT)) {
            stateResult.getReferenceIntensities().putIfAbsent(factor.getFunctionId(), reference);
            stateResult.getIntensitySensitivities().computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .putIfAbsent(factor.getVariableId(), sensitivity);
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

    public double getReferenceFlow(Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().getOrDefault(cnec.getNetworkElement().getId(), 0.0);
    }

    public double getReferenceIntensity(Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().getOrDefault(cnec.getNetworkElement().getId(), 0.0);
    }

    public double getSensitivityOnFlow(RangeAction<?> rangeAction, Cnec<?> cnec) {
        return RangeActionSensiHandler.get(rangeAction).getSensitivityOnFlow((FlowCnec) cnec, this);
    }

    public double getSensitivityOnFlow(SensitivityVariableSet glsk, Cnec<?> cnec) {
        return getSensitivityOnFlow(glsk.getId(), cnec);
    }

    public double getSensitivityOnFlow(String variableId, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
            !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId()) ||
            !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId)) {
            return 0.0;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return sensitivities.getOrDefault(variableId, 0.0);
    }

    @Deprecated
    public double getSensitivityOnIntensity(RangeAction<?> rangeAction, Cnec<?> cnec) {
        /*
        Should not be useful in the RAO -> sensi on intensity are never used + might crash for
        some rangeAction time
        For now: deprecate the method and make it throw an exception to ensure that is not used in RAO.
        Later: reprecate the method if it has some purpose ouside of the RAO.
         */

        throw new UnsupportedOperationException();
    }

    private StateResult getCnecStateResult(Cnec<?> cnec) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            if (cnec.getState().getInstant().equals(Instant.CURATIVE) && postCraResults.containsKey(optionalContingency.get().getId())) {
                return postCraResults.get(optionalContingency.get().getId());
            } else {
                return postContingencyResults.get(optionalContingency.get().getId());
            }
        } else {
            return nStateResult;
        }
    }
}
