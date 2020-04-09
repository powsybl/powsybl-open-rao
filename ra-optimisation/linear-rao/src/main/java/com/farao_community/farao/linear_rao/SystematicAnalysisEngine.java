package com.farao_community.farao.linear_rao;


import com.farao_community.farao.linear_rao.config.LinearRaoParameters;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;

public class SystematicAnalysisEngine {

    private SensitivityComputationParameters defaultParameters;
    private ComputationManager computationManager;

    SystematicAnalysisEngine(LinearRaoParameters linearRaoParameters, ComputationManager computationManager) {
        this.defaultParameters = linearRaoParameters.getSensitivityComputationParameters();
        this.computationManager = computationManager;
    }

    public void run(AbstractSituation abstractSituation, Network network) {

        SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, abstractSituation.getCrac(), computationManager, defaultParameters);

        // Failure if some sensitivities are not computed
        if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {

        } else {
            abstractSituation.setCost(-getMinMargin());
            abstractSituation.setResult(systematicSensitivityAnalysisResult, network);
        }
    }

}
