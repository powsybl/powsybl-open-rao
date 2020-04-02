package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.concurrent.CompletableFuture;

public class LinearRaoInitialSituation extends LinearRaoSituation{

    boolean initial;


    double cost;




    LinearRaoInitialSituation(Crac crac) {

        super(crac);
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager == null) {
            resultVariantManager = new ResultVariantManager();
            crac.addExtension(ResultVariantManager.class, resultVariantManager);
        }

        this.variantResultId = "preOptimisationResults-".concat(resultVariantManager.createNewUniqueVariantId());

    }


    void evaluateSensiAndCost(Network network, ComputationManager computationManager, SensitivityComputationParameters sensitivityComputationParameters) {

        SystematicSensitivityAnalysisResult currentSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (currentSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || currentSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            // delete()
            sensiStatus = ComputationStatus.RUN_NOK;
        } else {
            cost = getMinMargin();
            sensiStatus = ComputationStatus.RUN_OK;
        }
    }


    void completeResults(Network network) {
        super.completeResults();

        // add into Crac initial RA setpoints
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            double valueInNetwork = rangeAction.getCurrentValue(network);
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            RangeActionResult rangeActionResult = rangeActionResultMap.getVariant(resultVariantId);
            rangeActionResult.setSetPoint(preventiveState, valueInNetwork);
            if (rangeAction instanceof PstRange) {
                ((PstRangeResult) rangeActionResult).setTap(preventiveState, ((PstRange) rangeAction).computeTapPosition(valueInNetwork));
            }
        }
    }
}
