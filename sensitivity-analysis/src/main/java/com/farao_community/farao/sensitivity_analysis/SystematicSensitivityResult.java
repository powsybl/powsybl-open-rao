/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFunction;
import com.powsybl.sensitivity.SensitivityValue;
import com.powsybl.sensitivity.SensitivityVariable;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BranchIntensity;
import com.powsybl.sensitivity.factors.functions.BusVoltage;
import com.powsybl.sensitivity.factors.variables.*;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SystematicSensitivityResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicSensitivityResult.class);

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

    public SystematicSensitivityResult completeData(SensitivityAnalysisResult results, Network network, List<Contingency> contingencies, boolean afterCra) {

        if (results == null || !results.isOk()) {
            this.status = SensitivityComputationStatus.FAILURE;
            return this;
        }

        Map<String, StateResult> contingencyResultsToFill = afterCra ? postCraResults : postContingencyResults;
        results.getSensitivityValues().forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, nStateResult, network));
        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        contingencies.forEach(contingency -> {
            String contingencyVariantId = initialVariantId + contingency.getId();
            network.getVariantManager().cloneVariant(initialVariantId, contingencyVariantId);
            network.getVariantManager().setWorkingVariant(contingencyVariantId);
            contingency.toTask().modify(network, computationManager);

            StateResult contingencyStateResult = new StateResult();
            if (results.getSensitivityValuesContingencies().containsKey(contingency.getId())) {
                results.getSensitivityValuesContingencies().get(contingency.getId()).forEach(sensitivityValue -> fillIndividualValue(sensitivityValue, contingencyStateResult, network));
            }
            contingencyResultsToFill.put(contingency.getId(), contingencyStateResult);

            network.getVariantManager().removeVariant(contingencyVariantId);
        });
        network.getVariantManager().setWorkingVariant(initialVariantId);

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

    private void fillIndividualValue(SensitivityValue value, StateResult stateResult, Network network) {
        double reference = value.getFunctionReference();
        double sensitivity = value.getValue();

        // TODO: remove this fix when reference function patched in case NaN and no divergence
        if (Double.isNaN(reference) || Double.isNaN(sensitivity)) {
            if (isfFunctionOrVariableIsDisconnected(value, network)) {
                LOGGER.warn("NaN returned by sensitivity tool, but variable and function both connected and in main cc.");
            }
            sensitivity = 0.;
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

    static boolean isfFunctionOrVariableIsDisconnected(SensitivityValue value, Network network) {
        SensitivityFunction function = value.getFactor().getFunction();
        SensitivityVariable variable = value.getFactor().getVariable();

        // Check function
        if (function instanceof BranchFlow || function instanceof BranchIntensity) {
            Branch<?> branch = network.getBranch(function.getId());
            if (branch.getTerminals().stream().anyMatch(terminal -> !terminalConnectedAndInMainCC(terminal))) {
                return true;
            }
        }
        if (function instanceof BusVoltage) {
            throw new NotImplementedException("Bus voltages not implemented yet");
        }

        // Check variable
        if (variable instanceof HvdcSetpointIncrease) {
            HvdcLine hvdc = network.getHvdcLine(variable.getId());
            if (!terminalConnectedAndInMainCC(hvdc.getConverterStation1().getTerminal())) {
                return true;
            }
            if (!terminalConnectedAndInMainCC(hvdc.getConverterStation2().getTerminal())) {
                return true;
            }
        }

        if (variable instanceof LinearGlsk) {
            LinearGlsk glsk = (LinearGlsk) variable;
            for (String glskId : glsk.getGLSKs().keySet()) {
                Connectable<?> glskConnectable = (Connectable<?>) network.getIdentifiable(glskId);
                if (glskConnectable.getTerminals().stream().anyMatch(terminal -> !terminalConnectedAndInMainCC(terminal))) {
                    return true;
                }
            }
        }

        if (variable instanceof PhaseTapChangerAngle) {
            TwoWindingsTransformer pst = network.getTwoWindingsTransformer(variable.getId());
            if (pst.getTerminals().stream().anyMatch(terminal -> !terminalConnectedAndInMainCC(terminal))) {
                return true;
            }
        }

        if (variable instanceof InjectionIncrease) {
            throw new NotImplementedException("Injection increases not implemented yet");
        }

        if (variable instanceof TargetVoltage) {
            throw new NotImplementedException("Target voltages not implemented yet");
        }
        return false;
    }

    private static boolean terminalConnectedAndInMainCC(Terminal terminal) {
        return terminal.isConnected() && terminal.getBusBreakerView().getBus().isInMainConnectedComponent();
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
            return 0;
        }
        return stateResult.getReferenceFlows().getOrDefault(cnec.getNetworkElement().getId(), 0.);
    }

    public double getReferenceIntensity(Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null) {
            return 0;
        }
        return stateResult.getReferenceIntensities().getOrDefault(cnec.getNetworkElement().getId(), 0.);
    }

    public double getSensitivityOnFlow(RangeAction rangeAction, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (stateResult == null || !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0.;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.getOrDefault(netEl.getId(), 0.)).sum();
    }

    public double getSensitivityOnFlow(LinearGlsk glsk, Cnec<?> cnec) {
        return getSensitivityOnFlow(glsk.getId(), cnec);
    }

    public double getSensitivityOnFlow(String variableId, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        if (stateResult == null || !stateResult.getFlowSensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0;
        }
        Map<String, Double> sensitivities = stateResult.getFlowSensitivities().get(cnec.getNetworkElement().getId());
        return sensitivities.get(variableId);
    }

    public double getSensitivityOnIntensity(RangeAction rangeAction, Cnec<?> cnec) {
        StateResult stateResult = getCnecStateResult(cnec);
        Set<NetworkElement> networkElements = rangeAction.getNetworkElements();
        if (stateResult == null || !stateResult.getIntensitySensitivities().containsKey(cnec.getNetworkElement().getId())) {
            return 0;
        }
        Map<String, Double> sensitivities = stateResult.getIntensitySensitivities().get(cnec.getNetworkElement().getId());
        return networkElements.stream().mapToDouble(netEl -> sensitivities.getOrDefault(netEl.getId(), 0.)).sum();
    }

    private StateResult getCnecStateResult(Cnec<?> cnec) {
        Optional<com.farao_community.farao.data.crac_api.Contingency> optionalContingency = cnec.getState().getContingency();
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
