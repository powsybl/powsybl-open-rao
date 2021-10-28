/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

        if (results == null || !results.isOk()) {
            this.status = SensitivityComputationStatus.FAILURE;
            return this;
        }

        Map<String, StateResult> contingencyResultsToFill = afterCra ? postCraResults : postContingencyResults;
        results.getSensitivityValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult));
        results.getSensitivityValuesContingencies().forEach((contingencyId, sensitivityValues) -> {
            StateResult contingencyStateResult = new StateResult();
            sensitivityValues.forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, contingencyStateResult));
            contingencyResultsToFill.put(contingencyId, contingencyStateResult);
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

    private void fillIndividualValue(SensitivityValue value, StateResult stateResult) {
        double reference = value.getFunctionReference();
        double sensitivity = value.getValue();

        if (Double.isNaN(reference) || Double.isNaN(sensitivity)) {
            reference = 0.;
        }

        if (value.getFactor().getFunction() instanceof BranchFlow) {
            stateResult.getReferenceFlows().putIfAbsent(value.getFactor().getFunction().getId(), reference);
            stateResult.getFlowSensitivities().computeIfAbsent(value.getFactor().getFunction().getId(), k -> new HashMap<>())
                    .putIfAbsent(value.getFactor().getVariable().getId(), sensitivity);
        } else if (value.getFactor().getFunction() instanceof BranchIntensity) {
            stateResult.getReferenceIntensities().putIfAbsent(value.getFactor().getFunction().getId(), reference);
            stateResult.getIntensitySensitivities().computeIfAbsent(value.getFactor().getFunction().getId(), k -> new HashMap<>())
                    .putIfAbsent(value.getFactor().getVariable().getId(), sensitivity);
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

    public double getSensitivityOnFlow(RangeAction rangeAction, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (stateResult == null || !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0.0;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.getOrDefault(netEl.getId(), 0.0)).sum();
    }

    public double getSensitivityOnFlow(LinearGlsk glsk, Cnec<?> cnec) {
        return getSensitivityOnFlow(glsk.getId(), cnec);
    }

    public double getSensitivityOnFlow(String variableId, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null || !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0.0;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return sensitivities.get(variableId);
    }

    public double getSensitivityOnIntensity(RangeAction rangeAction, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (stateResult == null || !stateResult.getIntensitySensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0.0;
        }
        Map<String, Double> sensitivities = stateResult.getIntensitySensitivities().get(cnec.getNetworkElement().getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.get(netEl.getId())).sum();
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
