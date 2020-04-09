/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.optimisation.post_processors;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.linear_rao.optimisation.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoData;
import com.farao_community.farao.linear_rao.optimisation.LinearRaoProblem;
import com.farao_community.farao.rao_api.RaoResult;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    @Override
    public void process(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, RaoResult raoResult, String resultVariantId) {
        String preventiveState = linearRaoData.getCrac().getPreventiveState().getId();

        for (RangeAction rangeAction: linearRaoData.getCrac().getRangeActions()) {
            String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();

            double rangeActionVar = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction).solutionValue();
            double rangeActionVal = linearRaoProblem.getRangeActionSetPointVariable(rangeAction).solutionValue();

            if (rangeActionVar > 0) {
                if (rangeAction instanceof PstRange) {
                    PstRange pstRange = (PstRange) rangeAction;
                    TwoWindingsTransformer transformer = linearRaoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                    //todo : get pre optim angle and tap with a cleaner manner
                    double preOptimAngle = linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE).lb();
                    int preOptimTap = pstRange.computeTapPosition(preOptimAngle);

                    int approximatedPostOptimTap = pstRange.computeTapPosition(rangeActionVal);
                    double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                    if (approximatedPostOptimTap != preOptimTap) {
                        RangeActionResultExtension pstRangeResultMap = rangeAction.getExtension(RangeActionResultExtension.class);
                        PstRangeResult pstRangeResult = (PstRangeResult) pstRangeResultMap.getVariant(resultVariantId);
                        pstRangeResult.setSetPoint(preventiveState, approximatedPostOptimAngle);
                        pstRangeResult.setTap(preventiveState, approximatedPostOptimTap);
                    }
                }
            }
        }
    }
}
