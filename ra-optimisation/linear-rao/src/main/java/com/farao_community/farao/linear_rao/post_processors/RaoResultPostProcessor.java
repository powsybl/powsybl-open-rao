/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.linear_rao.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    @Override
    public void process(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData, RaoComputationResult raoComputationResult) {

        List<RemedialActionResult> remedialActionResults = new ArrayList<>();
        linearRaoData.getCrac().getRangeActions().forEach(
            rangeAction -> {
                String rangeActionId = rangeAction.getId();
                String rangeActionName = rangeAction.getName();
                String networkElementId = rangeAction.getNetworkElements().iterator().next().getId();

                Optional<MPVariable> rangeActionVar = checkRangeAction(linearRaoProblem.getNegativeRangeActionVariable(rangeActionId, networkElementId), linearRaoProblem.getPositiveRangeActionVariable(rangeActionId, networkElementId));

                if (rangeActionVar.isPresent()) {
                    Identifiable pNetworkElement = linearRaoData.getNetwork().getIdentifiable(networkElementId);
                    if (pNetworkElement instanceof TwoWindingsTransformer) {
                        TwoWindingsTransformer transformer = (TwoWindingsTransformer) pNetworkElement;

                        double preOptimAngle = rangeActionVar.get().solutionValue();
                        int preOptimTap = 0;
                        double postOptimAngle = rangeActionVar.get().solutionValue();
                        int postOptimTap = 0;

                        PstElementResult pstElementResult = new PstElementResult(networkElementId, preOptimAngle, preOptimTap, postOptimAngle, postOptimTap);
                        remedialActionResults.add(new RemedialActionResult(rangeActionId, rangeActionName, true, Collections.singletonList(pstElementResult)));
                    }
                }
            }
        );

        raoComputationResult.getPreContingencyResult().getRemedialActionResults().addAll(remedialActionResults);
    }

    private Optional<MPVariable> checkRangeAction(MPVariable negativeRangeActionVariable, MPVariable positiveRangeActionVariable) {
        if (negativeRangeActionVariable.solutionValue() != 0) {
            return Optional.of(negativeRangeActionVariable);
        } else if (positiveRangeActionVariable.solutionValue() != 0) {
            return Optional.of(positiveRangeActionVariable);
        } else {
            return Optional.empty();
        }
    }
}
