/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Optional;

import static com.farao_community.farao.data.crac_api.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller extends AbstractProblemFiller {

    //TODO : load from config
    private static final double PST_PENALTY_COST = 1; // in MW/degree

    public MaxMinMarginFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
    }

    @Override
    public void fill() {
        // build variables
        buildMinimumMarginVariable();

        // build constraints
        buildMinimumMarginConstraints();

        // complete objective
        fillObjectiveWithMinMargin();
        fillObjectiveWithRangeActionPenaltyCost();
    }

    @Override
    public void update() {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * This variable represents the smallest margin of all Cnecs.
     * It is given in MEGAWATT.
     */
    private void buildMinimumMarginVariable() {
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
    private void buildMinimumMarginConstraints() {
        linearRaoData.getCrac().getCnecs().forEach(cnec -> {

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

            try {
                minFlow = cnec.getThreshold().getMinThreshold(MEGAWATT);
                maxFlow = cnec.getThreshold().getMaxThreshold(MEGAWATT);
            } catch (SynchronizationException e) {
                throw new FaraoException(String.format("Min/max threshold of cnec %s cannot be obtained as they are not synchronised", cnec.getId()));
            }

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
    private void fillObjectiveWithMinMargin() {
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
    private void fillObjectiveWithRangeActionPenaltyCost() {
        linearRaoData.getCrac().getRangeActions().forEach(rangeAction -> {

            MPVariable absoluteVariationVariable = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            if (absoluteVariationVariable == null) {
                throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
            }

            if (rangeAction instanceof PstRange) {
                linearRaoProblem.getObjective().setCoefficient(absoluteVariationVariable, PST_PENALTY_COST);
            }
        });
    }
}

