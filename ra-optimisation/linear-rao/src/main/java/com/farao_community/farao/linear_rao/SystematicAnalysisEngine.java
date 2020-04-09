package com.farao_community.farao.linear_rao;

import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class SystematicAnalysisEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicAnalysisEngine.class);

    private LinearRaoParameters linearRaoParameters;
    private ComputationManager computationManager;
    private boolean useFallbackSensiParams;
    private Network network;

    SystematicAnalysisEngine(Network network, LinearRaoParameters linearRaoParameters, ComputationManager computationManager) {
        this.network = network;
        this.linearRaoParameters = linearRaoParameters;
        this.computationManager = computationManager;
        this.useFallbackSensiParams = false;
    }

    public void run(AbstractSituation abstractSituation, SensitivityComputationParameters sensitivityComputationParameters, boolean useFallbackSensiParams) {
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, abstractSituation.getCrac(), computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {

        } else {
            abstractSituation.setResults(systematicSensitivityAnalysisResult);
        }
    }

    public void runWithParametersSwitch(AbstractSituation abstractSituation) throws SensitivityComputationException{
        if (!useFallbackSensiParams) { // with default parameters
            try {
                run(abstractSituation, linearRaoParameters.getSensitivityComputationParameters(), useFallbackSensiParams);
            } catch (SensitivityComputationException e) {
                useFallbackSensiParams = true;
                runWithParametersSwitch(abstractSituation);
            }
        } else { // with fallback parameters
            if (linearRaoParameters.getFallbackSensiParameters() != null) {
                try {
                    LOGGER.warn("Fallback sensitivity parameters are used.");
                    run(abstractSituation,  linearRaoParameters.getFallbackSensiParameters(), useFallbackSensiParams);
                } catch (SensitivityComputationException e) {
                    abstractSituation.deleteResultVariant();
                    throw new SensitivityComputationException("Sensitivity computation failed with all sensitivity parameters.");
                }
            } else {
                abstractSituation.deleteResultVariant();
                useFallbackSensiParams = false; // in order to show in the export that no fallback computation was run
                throw new SensitivityComputationException("Sensitivity computation failed with all available sensitivity parameters.");
            }
        }
    }

    public boolean isSensiFallback() {
        return useFallbackSensiParams;
    }
}
