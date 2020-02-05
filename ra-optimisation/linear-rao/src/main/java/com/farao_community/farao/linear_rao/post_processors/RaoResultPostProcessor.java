/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.post_processors;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.linear_rao.AbstractPostProcessor;
import com.farao_community.farao.linear_rao.LinearRao;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.farao_community.farao.ra_optimisation.PstElementResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.ra_optimisation.RemedialActionResult;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RaoResultPostProcessor extends AbstractPostProcessor {

    private static final double EPSILON = 1e-3;

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

                        double preOptimAngle = transformer.getPhaseTapChanger().getCurrentStep().getAlpha();
                        int preOptimTap = transformer.getPhaseTapChanger().getTapPosition();

                        double postOptimAngle = rangeActionVar.get().solutionValue();
                        int postOptimTap = getClosestTapPosition(postOptimAngle, transformer);

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

    private int getClosestTapPosition(double finalAngle, TwoWindingsTransformer twoWindingsTransformer) {
        //todo : put into crac-impl
        PhaseTapChanger phaseTapChanger = twoWindingsTransformer.getPhaseTapChanger();

        Map<Integer, PhaseTapChangerStep> steps = new TreeMap<>();
        for (int tapPosition = phaseTapChanger.getLowTapPosition(); tapPosition <= phaseTapChanger.getHighTapPosition(); tapPosition++) {
            steps.put(tapPosition, phaseTapChanger.getStep(tapPosition));
        }
        double minAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).min().orElse(Double.NaN);
        double maxAngle = steps.values().stream().mapToDouble(PhaseTapChangerStep::getAlpha).max().orElse(Double.NaN);
        if (Double.isNaN(minAngle) || Double.isNaN(maxAngle)) {
            throw new FaraoException(String.format("Phase tap changer %s steps may be invalid", twoWindingsTransformer.getId()));
        }

        // Modification of the range limitation control allowing the final angle to exceed of an EPSILON value the limitation.
        if (finalAngle < minAngle && Math.abs(finalAngle - minAngle) > EPSILON || finalAngle > maxAngle && Math.abs(finalAngle - maxAngle) > EPSILON) {
            throw new FaraoException(String.format("Angle value %.4f not is the range of minimum and maximum angle values [%.4f,%.4f] of the phase tap changer %s steps", finalAngle, minAngle, maxAngle, twoWindingsTransformer.getId()));
        }
        AtomicReference<Double> angleDifference = new AtomicReference<>(Double.MAX_VALUE);
        AtomicInteger approximatedTapPosition = new AtomicInteger(phaseTapChanger.getTapPosition());
        steps.forEach((tapPosition, step) -> {
            double diff = Math.abs(step.getAlpha() - finalAngle);
            if (diff < angleDifference.get()) {
                angleDifference.set(diff);
                approximatedTapPosition.set(tapPosition);
            }
        });
        return approximatedTapPosition.get();
    }
}
