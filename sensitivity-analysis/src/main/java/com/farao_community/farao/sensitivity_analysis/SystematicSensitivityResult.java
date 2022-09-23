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
        this.status =  SensitivityComputationStatus.FAILURE;

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

        if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1)) {
            stateResult.getReferenceFlows().putIfAbsent(factor.getFunctionId(), reference);
            stateResult.getFlowSensitivities().computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .putIfAbsent(factor.getVariableId(), sensitivity);
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            if (!stateResult.getReferenceIntensities().containsKey(factor.getFunctionId())
                || Math.abs(reference) > Math.abs(stateResult.getReferenceIntensities().get(factor.getFunctionId()))) {
                stateResult.getReferenceIntensities().put(factor.getFunctionId(), reference);
            }
            stateResult.getIntensitySensitivities().computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .putIfAbsent(factor.getVariableId(), sensitivity);
        }
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
                        cnecFlowSensis.put(networkElementId, -cnecFlowSensis.get(networkElementId));
                    }
                });
                stateResult.getIntensitySensitivities().forEach((cnecId, cnecIntensitySensis) -> {
                    if (cnecIntensitySensis.containsKey(networkElementId)) {
                        cnecIntensitySensis.put(networkElementId, -cnecIntensitySensis.get(networkElementId));
                    }
                });
            }
        });
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

    public double getReferenceFlow(FlowCnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null) {
            return 0.0;
        }
        return stateResult.getReferenceFlows().getOrDefault(cnec.getNetworkElement().getId(), 0.0);
    }

    public double getReferenceIntensity(FlowCnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null) {
            return 0.0;
        }
        return stateResult.getReferenceIntensities().getOrDefault(cnec.getNetworkElement().getId(), 0.0);
    }

    public double getSensitivityOnFlow(RangeAction<?> rangeAction, FlowCnec cnec) {
        return RangeActionSensiHandler.get(rangeAction).getSensitivityOnFlow(cnec, this);
    }

    public double getSensitivityOnFlow(SensitivityVariableSet glsk, FlowCnec cnec) {
        return getSensitivityOnFlow(glsk.getId(), cnec);
    }

    public double getSensitivityOnFlow(String variableId, FlowCnec cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null ||
            !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId()) ||
            !stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId()).containsKey(variableId)) {
            return 0.0;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return sensitivities.getOrDefault(variableId, 0.0);
    }

    @Deprecated (since = "3.6.0")
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
