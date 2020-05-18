/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.sensitivity.SensitivityComputationResults;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityAnalysisResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityAnalysisResult.class);

    private class StateResult {
        private final Map<String, Double> referenceFlows = new HashMap<>();
        private final Map<String, Double> referenceIntensities = new HashMap<>();
        private final Map<String, Map<String, Double>> flowSensitivities = new HashMap<>();
        private final Map<String, Map<String, Double>> intensitySensitivities = new HashMap<>();

        public Map<String, Double> getReferenceFlows() {
            return referenceFlows;
        }

        public Map<String, Double> getReferenceIntensities() {
            return referenceIntensities;
        }

        public Map<String, Map<String, Double>> getFlowSensitivities() {
            return flowSensitivities;
        }

        public Map<String, Map<String, Double>> getIntensitySensitivities() {
            return intensitySensitivities;
        }
    }

    private final boolean isSuccess;
    private final StateResult nStateResult = new StateResult();
    private final Map<String, StateResult> contingencyResults = new HashMap<>();

    public SystematicSensitivityAnalysisResult(SensitivityComputationResults results) {
        if (results == null) {
            this.isSuccess = false;
            return;
        }
        this.isSuccess = results.isOk();
        LOGGER.debug("Filling data...");
        fillData(results);
        LOGGER.debug("Data post treatment...");
        postTreatIntensities();
    }

    private void postTreatIntensities() {
        postTreatIntensitiesOnState(nStateResult);
        contingencyResults.values().forEach(this::postTreatIntensitiesOnState);
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

    private void fillData(SensitivityComputationResults results) {
        results.getSensitivityValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult));
        results.getSensitivityValuesContingencies().forEach((contingencyId, sensitivityValues) -> {
            StateResult contingencyStateResult = new StateResult();
            sensitivityValues.forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, contingencyStateResult));
            contingencyResults.put(contingencyId, contingencyStateResult);
        });
    }

    private void fillIndividualValue(SensitivityValue value, StateResult stateResult) {
        double reference = value.getFunctionReference();
        double sensitivity = value.getValue();

        // TODO: remove this fix when reference function patched in case NaN and no divergence
        if (Double.isNaN(reference) && !Double.isNaN(sensitivity)) {
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
        return isSuccess;
    }

    public double getReferenceFlow(Cnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        return stateResult.getReferenceFlows().getOrDefault(cnec.getId(), Double.NaN);
    }

    public double getReferenceIntensity(Cnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        return stateResult.getReferenceIntensities().getOrDefault(cnec.getId(), Double.NaN);
    }

    public double getSensitivityOnFlow(RangeAction rangeAction, Cnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (!stateResult.getFlowSensitivities().containsKey(cnec.getId())) {
            return Double.NaN;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.get(netEl.getId())).sum();
    }

    public double getSensitivityOnIntensity(RangeAction rangeAction, Cnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (!stateResult.getIntensitySensitivities().containsKey(cnec.getId())) {
            return Double.NaN;
        }
        Map<String, Double> sensitivities = stateResult.getIntensitySensitivities().get(cnec.getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.get(netEl.getId())).sum();
    }

    private StateResult getCnecStateResult(Cnec cnec) {
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            return contingencyResults.get(optionalContingency.get().getId());
        } else {
            return nStateResult;
        }
    }
}
