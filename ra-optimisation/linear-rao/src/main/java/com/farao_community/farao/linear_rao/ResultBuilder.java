package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResultBuilder.class);

    Crac crac;


    ResultBuilder(Crac crac) {
        this.crac = crac;
    }

    void updateCnecExtensions(String resultVariantId, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        crac.getCnecs().forEach(cnec -> {
            CnecResultExtension cnecResultMap = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultMap.getVariant(resultVariantId);
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setThresholds(cnec);
        });
    }

    //this method is only used for pre optim result (to store all the rangeAction initial setPoints)
    void fillPreOptimRangeActionResultsFromNetwork(String resultVariantId, Network network) {
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

    void updateResultExtensions(double minMargin, String resultVariantId, SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult) {
        updateCracExtension(resultVariantId, minMargin);
        updateCnecExtensions(resultVariantId, systematicSensitivityAnalysisResult);
        //The range action extensions are already updated by the solve method.
        //The network action extensions are not to be updated by the linear rao. They will be updated by the search tree rao if required.
    }

    void updateCracExtension(String resultVariantId, double minMargin) {
        CracResultExtension cracResultMap = crac.getExtension(CracResultExtension.class);
        CracResult cracResult = cracResultMap.getVariant(resultVariantId);
        cracResult.setCost(-minMargin);
    }

    RaoResult buildRaoResult(double minMargin, String preOptimVariantId, String postOptimVariantId) {
        RaoResult raoResult = new RaoResult(RaoResult.Status.SUCCESS);
        raoResult.setPreOptimVariantId(preOptimVariantId);
        raoResult.setPostOptimVariantId(postOptimVariantId);
        LOGGER.info("LinearRaoResult: minimum margin = {}, security status: {}", (int) minMargin, minMargin >= 0 ?
            CracResult.NetworkSecurityStatus.SECURED : CracResult.NetworkSecurityStatus.UNSECURED);
        return raoResult;
    }

}
