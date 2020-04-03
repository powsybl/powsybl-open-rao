package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.Network;

public final class LinearRaoOptimizedSituation extends AbstractLinearRaoSituation {

    ComputationStatus lpStatus;

    LinearRaoOptimizedSituation(Crac crac) {
        super(crac);
        this.resultVariantId = crac.getExtension(ResultVariantManager.class).createNewUniqueVariantId("postOptimisationResults-");
        this.lpStatus = ComputationStatus.NOT_RUN;
    }

    void solveLp(LinearRaoModeller linearRaoModeller) {
        RaoResult lpRaoResult = linearRaoModeller.solve(resultVariantId);

        if (lpRaoResult.getStatus() == RaoResult.Status.FAILURE) {
            lpStatus = ComputationStatus.RUN_NOK;
        } else {
            lpStatus = ComputationStatus.RUN_OK;
        }
    }

    ComputationStatus getLpStatus() {
        return lpStatus;
    }

    void applyRAs(Network network) {
        String preventiveState = crac.getPreventiveState().getId();
        for (RangeAction rangeAction : crac.getRangeActions()) {
            RangeActionResultExtension rangeActionResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
            rangeAction.apply(network, rangeActionResultMap.getVariant(resultVariantId).getSetPoint(preventiveState));
        }
    }

}
