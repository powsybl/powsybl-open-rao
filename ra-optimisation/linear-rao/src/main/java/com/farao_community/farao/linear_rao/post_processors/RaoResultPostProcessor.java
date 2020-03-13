/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_result_extensions.PstRangeResult;
import com.farao_community.farao.data.crac_result_extensions.ResultExtension;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.linear_rao.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    @Override
    public void process(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, RaoComputationResult raoComputationResult, String resultVariantId) {
        State preventiveState = linearRaoData.getCrac().getPreventiveState();

        //Old computation result code
        List<RemedialActionResult> remedialActionResults = new ArrayList<>();
        for (RangeAction<?> rangeAction: linearRaoData.getCrac().getRangeActions()) {
            //Old computation result code
            String rangeActionId = rangeAction.getId();
            String rangeActionName = rangeAction.getName();
          
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
                            ResultExtension<PstRange, PstRangeResult> pstRangeResultMap = pstRange.getExtension(ResultExtension.class);
                            PstRangeResult pstRangeResult = pstRangeResultMap.getVariant(resultVariantId);
                            pstRangeResult.setSetPoint(preventiveState, approximatedPostOptimAngle);
                            pstRangeResult.setTap(preventiveState, approximatedPostOptimTap);

                            //old computation result code
                            PstElementResult pstElementResult = new PstElementResult(networkElementId, preOptimAngle, preOptimTap, approximatedPostOptimAngle, approximatedPostOptimTap);
                            remedialActionResults.add(new RemedialActionResult(rangeActionId, rangeActionName, true, Collections.singletonList(pstElementResult)));
                        }
                    }
                }
            }
        );
        //old computation result code
        raoComputationResult.getPreContingencyResult().getRemedialActionResults().addAll(remedialActionResults);
    }
}
