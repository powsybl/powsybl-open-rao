package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityComputationParameters;
import com.powsybl.sensitivity.SensitivityComputationResults;

import java.util.concurrent.CompletableFuture;

public final class LinearRaoOptimizedSituation extends LinearRaoSituation {

    SensitivityComputationResults sensitivityComputationResults;
    double cost;

    ComputationStatus lpStatus;

    LinearRaoOptimizedSituation(Crac crac) {
        super(crac);
        this.resultVariantId = "postOptimisationResults-".concat(crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId());
        this.lpStatus = ComputationStatus.NOT_RUN;
    }

    void evaluateCost() {
    }

    boolean compareRaResults(LinearRaoSituation otherLinearRaoSituation) {
        return true;
    }

    void solveLp(LinearRaoModeller linearRaoModeller) {
        RaoResult LPRaoResult = linearRaoModeller.solve(resultVariantId);

        if (LPRaoResult.getStatus() == RaoResult.Status.FAILURE) {
            lpStatus = ComputationStatus.RUN_NOK;
        }
    }

    ComputationStatus getLpStatus() {
        return lpStatus;
    }

    void completeResults(Crac crac) {
    }

    void delete() {
    }


    void applyRAs(Network network) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(network, rangeActionResultMap.getVariant(resultVariantId).getSetPoint(preventiveState));
        }
    }

}
