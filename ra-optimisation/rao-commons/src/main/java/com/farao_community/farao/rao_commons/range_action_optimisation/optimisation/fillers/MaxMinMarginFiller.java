/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.Unit;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Optional;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {

    @Override
    public void fill(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        // build variables
        buildMinimumMarginVariable(linearRaoProblem);

        // build constraints
        buildMinimumMarginConstraints(raoData, linearRaoProblem);

        // complete objective
        fillObjectiveWithMinMargin(linearRaoProblem);
        fillObjectiveWithRangeActionPenaltyCost(raoData, linearRaoProblem, fillerParameters);
    }

    @Override
    public void update(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * This variable represents the smallest margin of all Cnecs.
     * It is given in MEGAWATT.
     */
    private void buildMinimumMarginVariable(LinearRaoProblem linearRaoProblem) {
        linearRaoProblem.addMinimumMarginVariable(-linearRaoProblem.infinity(), linearRaoProblem.infinity());
    }

    /**
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definition
     * of the min margin : MM = min{c in CNEC} margin[c].
     *
     * For each Cnec c, the two constraints are :
     *
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     */
    private void buildMinimumMarginConstraints(RaoData raoData, LinearRaoProblem linearRaoProblem) {
        raoData.getCrac().getCnecs().forEach(cnec -> {

            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            MPVariable minimumMarginVariable = linearRaoProblem.getMinimumMarginVariable();

            if (minimumMarginVariable == null) {
                throw new FaraoException("Minimum margin variable has not yet been created");
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getMinThreshold(Unit.MEGAWATT);
            maxFlow = cnec.getMaxThreshold(Unit.MEGAWATT);

            if (minFlow.isPresent()) {
                MPConstraint minimumMarginNegative = linearRaoProblem.addMinimumMarginConstraint(-linearRaoProblem.infinity(), -minFlow.get(), cnec, LinearRaoProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minimumMarginVariable, 1);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearRaoProblem.addMinimumMarginConstraint(-linearRaoProblem.infinity(), maxFlow.get(), cnec, LinearRaoProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minimumMarginVariable, 1);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        });
    }

    /**
     * Add in the objective function of the linear problem the min Margin.
     *
     * min(-MM)
     */
    private void fillObjectiveWithMinMargin(LinearRaoProblem linearRaoProblem) {
        MPVariable minimumMarginVariable = linearRaoProblem.getMinimumMarginVariable();

        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        linearRaoProblem.getObjective().setCoefficient(minimumMarginVariable, -1);
    }

    /**
     * Add in the objective function a penalty cost associated to the RangeAction
     * activations. This penalty cost prioritizes the solutions which change as less
     * as possible the set points of the RangeActions.
     *
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    private void fillObjectiveWithRangeActionPenaltyCost(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        raoData.getCrac().getRangeActions().forEach(rangeAction -> {

            MPVariable absoluteVariationVariable = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            if (absoluteVariationVariable == null) {
                throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
            }

            if (rangeAction instanceof PstRange) {
                linearRaoProblem.getObjective().setCoefficient(absoluteVariationVariable, fillerParameters.getPstPenaltyCost());
            }
        });
    }
}

