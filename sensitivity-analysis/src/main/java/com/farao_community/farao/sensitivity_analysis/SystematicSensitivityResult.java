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
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.sensitivity_analysis.ra_sensi_handler.RangeActionSensiHandler;
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

        private SensitivityComputationStatus getSensitivityComputationStatus() {
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
    }

    public enum SensitivityComputationStatus {
        SUCCESS,
        FAILURE
    }

    private SensitivityComputationStatus status;
    private final StateResult nStateResult = new StateResult();
    private final Map<Instant, Map<String, StateResult>> postContingencyResults = new EnumMap<>(Instant.class);

    private final Map<Cnec, StateResult> memoizedStateResultPerCnec = new HashMap<>();

    public SystematicSensitivityResult() {
        this.status = SensitivityComputationStatus.SUCCESS;
        this.postContingencyResults.put(Instant.OUTAGE, new HashMap<>());
        this.postContingencyResults.put(Instant.AUTO, new HashMap<>());
        this.postContingencyResults.put(Instant.CURATIVE, new HashMap<>());
    }

    public SystematicSensitivityResult(SensitivityComputationStatus status) {
        this.status = status;
        this.postContingencyResults.put(Instant.OUTAGE, new HashMap<>());
        this.postContingencyResults.put(Instant.AUTO, new HashMap<>());
        this.postContingencyResults.put(Instant.CURATIVE, new HashMap<>());
    }

    protected static SensitivityResultWriter getSensitivityResultWriter(List<SensitivityFactor> factors, SystematicSensitivityResult result, Instant instant, List<com.powsybl.contingency.Contingency> contingencies, Set<String> hvdcsToInvert) {
        return new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
                if (contingencyIndex == -1) {
                    fillIndividualValue(functionReference, value, factors.get(factorIndex), hvdcsToInvert, result.nStateResult);
                } else {
                    StateResult contingencyStateResult = result.postContingencyResults.get(instant).getOrDefault(contingencies.get(contingencyIndex).getId(), new StateResult());
                    fillIndividualValue(functionReference, value, factors.get(factorIndex), hvdcsToInvert, contingencyStateResult);
                    result.postContingencyResults.get(instant).put(contingencies.get(contingencyIndex).getId(), contingencyStateResult);
                }

                // status set to failure initially, and set to success if we find at least one non NaN value
                if (result.getStatus().equals(SensitivityComputationStatus.FAILURE) && (!Double.isNaN(functionReference) && !Double.isNaN(value))) {
                        result.setStatus(SensitivityComputationStatus.SUCCESS);
                }
            }

            @Override
            public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
                    StateResult contingencyStateResult = result.postContingencyResults.get(instant).getOrDefault(contingencies.get(contingencyIndex).getId(), new StateResult());
                    contingencyStateResult.status = status.equals(SensitivityAnalysisResult.Status.FAILURE) ? SensitivityComputationStatus.FAILURE : SensitivityComputationStatus.SUCCESS;
                    result.postContingencyResults.get(instant).put(contingencies.get(contingencyIndex).getId(), contingencyStateResult);
            }
        };
    }

    private static void fillIndividualValue(double reference, double sensitivity, SensitivityFactor factor, Set<String> hvdcsToInvert, StateResult stateResult) {
        double functionReference = reference;
        if (Double.isNaN(reference)) {
            functionReference = 0;
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
                .putIfAbsent(side, functionReference * activePowerCoefficient);
            stateResult.getFlowSensitivities()
                .computeIfAbsent(factor.getFunctionId(), k -> new HashMap<>())
                .computeIfAbsent(factor.getVariableId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, Double.isNaN(reference) ? 0 : sensitivity * activePowerCoefficient * ((hvdcsToInvert.contains(factor.getVariableId())) ? -1 : 1));
        } else if (factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || factor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)) {
            stateResult.getReferenceIntensities()
                .computeIfAbsent(factor.getFunctionId(), k -> new EnumMap<>(Side.class))
                .putIfAbsent(side, functionReference);
        }
    }

    public SystematicSensitivityResult postTreatIntensitiesAndStatus() {
        nStateResult.status = this.getStatus();
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
            });
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
            List<Instant> possibleInstants = postContingencyResults.keySet().stream()
                    .filter(instant -> instant.comesBefore(state.getInstant()) || instant.equals(state.getInstant()))
                    .sorted(Comparator.comparingInt(instant -> -instant.getOrder()))
                    .collect(Collectors.toList());
            for (Instant instant : possibleInstants) {
                // Use latest sensi computed on state
                if (postContingencyResults.get(instant).containsKey(optionalContingency.get().getId())) {
                    return postContingencyResults.get(instant).get(optionalContingency.get().getId()).getSensitivityComputationStatus();
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
        if (memoizedStateResultPerCnec.containsKey(cnec)) {
            return memoizedStateResultPerCnec.get(cnec);
        }
        Optional<Contingency> optionalContingency = cnec.getState().getContingency();
        if (optionalContingency.isPresent()) {
            List<Instant> possibleInstants = postContingencyResults.keySet().stream()
                    .filter(instant -> instant.comesBefore(cnec.getState().getInstant()) || instant.equals(cnec.getState().getInstant()))
                    .sorted(Comparator.comparingInt(instant -> -instant.getOrder()))
                    .collect(Collectors.toList());
            for (Instant instant : possibleInstants) {
                // Use latest sensi computed on the cnec's contingency amidst the last instants before cnec state.
                String contingencyId = optionalContingency.get().getId();
                if (postContingencyResults.get(instant).containsKey(contingencyId)) {
                    memoizedStateResultPerCnec.put(cnec, postContingencyResults.get(instant).get(contingencyId));
                    return memoizedStateResultPerCnec.get(cnec);
                }
            }
            return null;
        } else {
            return nStateResult;
        }
    }
}
