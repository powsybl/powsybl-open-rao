package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.*;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.farao_community.farao.util.SystematicSensitivityAnalysisService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;

import static java.lang.String.format;

abstract class AbstractLinearRaoSituation {

    enum ComputationStatus {
        NOT_RUN,
        RUN_OK,
        RUN_NOK
    }

    protected ComputationStatus sensiStatus;

    protected Crac crac;
    protected SystematicSensitivityAnalysisResult systematicSensitivityAnalysisResult;
    protected String resultVariantId;

    protected double cost;

    AbstractLinearRaoSituation(Crac crac) {
        sensiStatus = ComputationStatus.NOT_RUN;
        this.crac = crac;
    }

    boolean sameRaResults(AbstractLinearRaoSituation otherLinearRaoSituation) {
        String otherResultVariantId = otherLinearRaoSituation.getResultVariant();
        //TODO: manage curative RA
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            double value1 = rangeActionResultMap.getVariant(resultVariantId).getSetPoint(preventiveState);
            double value2 = rangeActionResultMap.getVariant(otherResultVariantId).getSetPoint(preventiveState);
            if (value1 != value2 && (!Double.isNaN(value1) || !Double.isNaN(value2))) {
                return false;
            }
        }
        return true;
    }

    void deleteResultVariant() {
        crac.getExtension(ResultVariantManager.class).deleteVariant(resultVariantId);
    }

    void evaluateSensiAndCost(Network network, ComputationManager computationManager, SensitivityComputationParameters sensitivityComputationParameters) {

        systematicSensitivityAnalysisResult = SystematicSensitivityAnalysisService
            .runAnalysis(network, crac, computationManager, sensitivityComputationParameters);

        // Failure if some sensitivities are not computed
        if (systematicSensitivityAnalysisResult.getStateSensiMap().containsValue(null) || systematicSensitivityAnalysisResult.getCnecFlowMap().isEmpty()) {
            // delete()
            sensiStatus = ComputationStatus.RUN_NOK;
        } else {
            cost = -getMinMargin();
            sensiStatus = ComputationStatus.RUN_OK;
        }
    }

    ComputationStatus getSensiStatus() {
        return sensiStatus;
    }

    protected double getMinMargin() {
        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : crac.getCnecs()) {
            double flow = systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    void completeResults(Network network) {
        updateCracExtension();
        updateCnecExtensions();
    }

    private void updateCracExtension() {
        CracResultExtension cracResultMap = crac.getExtension(CracResultExtension.class);
        CracResult cracResult = cracResultMap.getVariant(resultVariantId);
        cracResult.setCost(cost);
    }

    private void updateCnecExtensions() {
        crac.getCnecs().forEach(cnec -> {
            CnecResultExtension cnecResultMap = cnec.getExtension(CnecResultExtension.class);
            CnecResult cnecResult = cnecResultMap.getVariant(resultVariantId);
            cnecResult.setFlowInMW(systematicSensitivityAnalysisResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setFlowInA(systematicSensitivityAnalysisResult.getCnecIntensityMap().getOrDefault(cnec, Double.NaN));
            cnecResult.setThresholds(cnec);
        });
    }

    double getCost() {
        return cost;
    }

    String getResultVariant() {
        return resultVariantId;
    }

    SystematicSensitivityAnalysisResult getSystematicSensitivityAnalysisResult() {
        return systematicSensitivityAnalysisResult;
    }
}
