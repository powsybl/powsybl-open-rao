package com.farao_community.farao.linear_rao;

import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.util.SensitivityComputationException;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystematicAnalysisEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystematicAnalysisEngine.class);

    private LinearRaoParameters linearRaoParameters;
    private ComputationManager computationManager;
    private boolean useFallbackSensiParams;

    SystematicAnalysisEngine(LinearRaoParameters linearRaoParameters, ComputationManager computationManager) {
        this.linearRaoParameters = linearRaoParameters;
        this.computationManager = computationManager;
        this.useFallbackSensiParams = false;
    }

    private void run(AbstractSituation abstractSituation, SensitivityComputationParameters sensitivityComputationParameters) {
        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(abstractSituation.getNetwork(), abstractSituation.getCrac(), computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {

        } else {
            abstractSituation.setResults(systematicSensitivityAnalysisResult);
        }
    }

    public void run(AbstractSituation abstractSituation) throws SensitivityComputationException {
        if (!useFallbackSensiParams) { // with default parameters
            try {
                run(abstractSituation, linearRaoParameters.getSensitivityComputationParameters());
            } catch (SensitivityComputationException e) {
                useFallbackSensiParams = true;
                run(abstractSituation);
            }
        } else { // with fallback parameters
            if (linearRaoParameters.getFallbackSensiParameters() != null) {
                try {
                    LOGGER.warn("Fallback sensitivity parameters are used.");
                    run(abstractSituation,  linearRaoParameters.getFallbackSensiParameters());
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
