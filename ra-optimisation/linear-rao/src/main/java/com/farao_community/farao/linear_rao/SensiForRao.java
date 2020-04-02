package com.farao_community.farao.linear_rao;

import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SensiForRao {

    private SensitivityComputationResults sensitivityComputationResult;

    SensitivityComputationResults getResult(String variantId) {
        return sensitivityComputationResult;
    }

    void runSensi() {
        // Initiate sensitivity analysis results
        SystematicSensitivityAnalysisResult currentSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);
        // Failure if some sensitivities are not computed
        if (currentSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || currentSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            resultVariantManager.deleteVariants(preOptimVariant, bestResultVariant);
            return CompletableFuture.completedFuture(new RaoResult(RaoResult.Status.FAILURE));
        }
    }



}
