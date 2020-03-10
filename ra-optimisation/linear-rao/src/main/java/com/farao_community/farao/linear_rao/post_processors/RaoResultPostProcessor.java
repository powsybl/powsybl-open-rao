/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.linear_rao.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.powsybl.iidm.network.TwoWindingsTransformer;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    @Override
    public void process(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, RaoComputationResult raoComputationResult, String variantId) {

        linearRaoData.getCrac().getRangeActions().forEach(
            rangeAction -> {
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();

                double rangeActionVar = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction).solutionValue();
                double rangeActionVal = linearRaoProblem.getRangeActionSetPointVariable(rangeAction).solutionValue();

                if (rangeActionVar > 0) {
                    if (rangeAction instanceof PstRange) {

                        TwoWindingsTransformer transformer = linearRaoData.getNetwork().getTwoWindingsTransformer(networkElementId);

                        //todo : get pre optim angle and tap with a cleaner manner
                        double preOptimAngle = linearRaoProblem.getAbsoluteRangeActionVariationConstraint(rangeAction, LinearRaoProblem.AbsExtension.POSITIVE).lb();
                        int preOptimTap = ((PstRange) rangeAction).computeTapPosition(preOptimAngle);

                        int approximatedPostOptimTap = ((PstRange) rangeAction).computeTapPosition(rangeActionVal);
                        double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                        if (approximatedPostOptimTap != preOptimTap) {
                            ((PstRangeResult) rangeAction.getExtension(PstRangeResult.class)).setSetPoint(linearRaoData.getCrac().getPreventiveState(), approximatedPostOptimAngle);
                        }
                    }
                }
            }
        );
    }
}
