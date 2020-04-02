package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.util.SystematicSensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityComputationResults;

import static java.lang.String.format;

public class LinearRaoSituation {

    enum ComputationStatus {
        NOT_RUN,
        RUN_OK,
        RUN_NOK
    }

    ComputationStatus sensiStatus;

    Crac crac;
    SystematicSensitivityAnalysisResult sensiAsResult;
    String resultVariantId;

    LinearRaoSituation(Crac crac) {
        sensiStatus = ComputationStatus.NOT_RUN;
        this.crac = crac;
    }

    boolean compareRaResults(LinearRaoInitialSituation otherLinearRaoSituation) {
        return true;
    }

    void delete() {

    }

    double getMinMargin() {
        double minMargin = Double.POSITIVE_INFINITY;
        for (Cnec cnec : crac.getCnecs()) {
            double flow = sensiAsResult.getCnecFlowMap().getOrDefault(cnec, Double.NaN);
            double margin = cnec.computeMargin(flow, Unit.MEGAWATT);
            if (Double.isNaN(margin)) {
                throw new FaraoException(format("Cnec %s is not present in the linear RAO result. Bad behaviour.", cnec.getId()));
            }
            minMargin = Math.min(minMargin, margin);
        }
        return minMargin;
    }

    void completeResult() {

    }

}
