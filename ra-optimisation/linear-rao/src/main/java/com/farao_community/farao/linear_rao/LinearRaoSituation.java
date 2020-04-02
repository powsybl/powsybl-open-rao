package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.data.crac_result_extensions.CnecResultExtension;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityComputationResults;

import static java.lang.String.format;

class LinearRaoSituation {

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


    LinearRaoSituation(Crac crac) {
        sensiStatus = ComputationStatus.NOT_RUN;
        this.crac = crac;
    }

    boolean compareRaResults(LinearRaoInitialSituation otherLinearRaoSituation) {
        return true;
    }

    void delete() {

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

    protected void completeResults() {
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
}
