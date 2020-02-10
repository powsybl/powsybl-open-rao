/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.post_processors.PstElementResultsPostProcessor;
import com.farao_community.farao.linear_rao.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    private static final double PST_LIMITS_TOLERANCE = 1e-3;

    @Override
    public void process(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, RaoComputationResult raoComputationResult) {

        List<RemedialActionResult> remedialActionResults = new ArrayList<>();
        linearRaoData.getCrac().getRangeActions().forEach(
            rangeAction -> {
                String rangeActionId = rangeAction.getId();
                String rangeActionName = rangeAction.getName();
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();

                double rangeActionVar = getRangeActionVariation(linearRaoProblem.getNegativeRangeActionVariable(rangeActionId, networkElementId), linearRaoProblem.getPositiveRangeActionVariable(rangeActionId, networkElementId));

                if (Math.abs(rangeActionVar) > 0) {
                    Identifiable pNetworkElement = linearRaoData.getNetwork().getIdentifiable(networkElementId);
                    if (pNetworkElement instanceof TwoWindingsTransformer) {
                        TwoWindingsTransformer transformer = (TwoWindingsTransformer) pNetworkElement;

                        double preOptimAngle = transformer.getPhaseTapChanger().getCurrentStep().getAlpha();
                        int preOptimTap = transformer.getPhaseTapChanger().getTapPosition();

                        double postOptimAngle = preOptimAngle + rangeActionVar;
                        int approximatedPostOptimTap = getClosestTapPosition(postOptimAngle, transformer);
                        double approximatedPostOptimAngle = transformer.getPhaseTapChanger().getStep(approximatedPostOptimTap).getAlpha();

                        if (approximatedPostOptimTap != preOptimTap) {
                            PstElementResult pstElementResult = new PstElementResult(networkElementId, preOptimAngle, preOptimTap, approximatedPostOptimAngle, approximatedPostOptimTap);
                            remedialActionResults.add(new RemedialActionResult(rangeActionId, rangeActionName, true, Collections.singletonList(pstElementResult)));
                        }
                    }
                }
            }
        );

        raoComputationResult.getPreContingencyResult().getRemedialActionResults().addAll(remedialActionResults);
    }

    private double getRangeActionVariation(MPVariable negativeRangeActionVariable, MPVariable positiveRangeActionVariable) {
        return positiveRangeActionVariable.solutionValue() - negativeRangeActionVariable.solutionValue();
    }

    private int getClosestTapPosition(double finalAngle, TwoWindingsTransformer twoWindingsTransformer) {
        //todo : put this method into crac-impl
        //todo : make PstElementResultsPostProcessor().computeTapPosition() private again
        return new PstElementResultsPostProcessor().computeTapPosition(finalAngle, twoWindingsTransformer.getPhaseTapChanger(), twoWindingsTransformer);
    }
}
