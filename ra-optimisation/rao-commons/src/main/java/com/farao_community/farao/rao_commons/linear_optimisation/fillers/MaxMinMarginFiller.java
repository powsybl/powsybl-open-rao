/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.PstRange;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Optional;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {
    public static final double DEFAULT_PST_PENALTY_COST = 0.01;

    private Unit unit;
    private double pstPenaltyCost;

    public MaxMinMarginFiller(Unit unit, double pstPenaltyCost) {
        this.unit = unit;
        this.pstPenaltyCost = pstPenaltyCost;
    }

    // Methods for tests
    public MaxMinMarginFiller() {
        this(MEGAWATT, DEFAULT_PST_PENALTY_COST);
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
    // End of methods for tests

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        // build variables
        buildMinimumMarginVariable(linearProblem);

        // build constraints
        buildMinimumMarginConstraints(raoData, linearProblem);

        // complete objective
        fillObjectiveWithMinMargin(linearProblem);
        fillObjectiveWithRangeActionPenaltyCost(raoData, linearProblem);
    }

    @Override
    public void update(RaoData raoData, LinearProblem linearProblem) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * This variable represents the smallest margin of all Cnecs.
     * It is given in MEGAWATT.
     */
    private void buildMinimumMarginVariable(LinearProblem linearProblem) {
        linearProblem.addMinimumMarginVariable(-linearProblem.infinity(), linearProblem.infinity());
    }

    /**
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definition
     * of the min margin : MM = min{c in CNEC} margin[c].
     *
     * For each Cnec c, the two constraints are (if the max margin is defined in MEGAWATT) :
     *
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     *
     * For each Cnec c, the two constraints are (if the max margin is defined in AMPERE) :
     *
     * MM <= (fmax[c] - F[c]) * 1000 / (Unom * sqrt(3))     (ABOVE_THRESHOLD)
     * MM <= (F[c] - fmin[c]) * 1000 / (Unom * sqrt(3))     (BELOW_THRESHOLD)
     */
    private void buildMinimumMarginConstraints(RaoData raoData, LinearProblem linearProblem) {
        raoData.getCrac().getCnecs().forEach(cnec -> {

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();

            if (minimumMarginVariable == null) {
                throw new FaraoException("Minimum margin variable has not yet been created");
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getMinThreshold(MEGAWATT);
            maxFlow = cnec.getMaxThreshold(MEGAWATT);
            double unitConversionCoefficient = getUnitConversionCoefficient(cnec, raoData);

            if (minFlow.isPresent()) {
                MPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), -minFlow.get(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), maxFlow.get(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        });
    }

    /**
     * Add in the objective function of the linear problem the min Margin.
     *
     * min(-MM)
     */
    private void fillObjectiveWithMinMargin(LinearProblem linearProblem) {
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();

        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        linearProblem.getObjective().setCoefficient(minimumMarginVariable, -1);
    }

    /**
     * Add in the objective function a penalty cost associated to the RangeAction
     * activations. This penalty cost prioritizes the solutions which change as less
     * as possible the set points of the RangeActions.
     *
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    private void fillObjectiveWithRangeActionPenaltyCost(RaoData raoData, LinearProblem linearProblem) {
        raoData.getCrac().getRangeActions().forEach(rangeAction -> {

            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            if (absoluteVariationVariable == null) {
                throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
            }

            if (rangeAction instanceof PstRange) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, pstPenaltyCost);
            }
        });
    }

    /**
     * Get unit conversion coefficient
     * the flows are always defined in MW, so if the minimum margin is defined in ampere,
     * and appropriate conversion coefficient should be used.
     */
    private double getUnitConversionCoefficient(Cnec cnec, RaoData linearRaoData) {
        if (unit.equals(MEGAWATT)) {
            return 1;
        } else {
            // Unom(cnec) * sqrt(3) / 1000
            return linearRaoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV() * Math.sqrt(3) / 1000;
        }
    }
}

