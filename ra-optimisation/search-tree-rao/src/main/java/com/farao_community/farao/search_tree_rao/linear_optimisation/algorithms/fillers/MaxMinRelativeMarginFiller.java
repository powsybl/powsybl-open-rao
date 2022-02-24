/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.rao_api.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {
    private final FlowResult initialFlowResult;
    private final Unit unit;
    private final double ptdfSumLowerBound;
    private final double negativeMarginObjectiveCoefficient;
    private final double highestThreshold;

    public MaxMinRelativeMarginFiller(Set<FlowCnec> optimizedCnecs,
                                      FlowResult initialFlowResult,
                                      Set<RangeAction<?>> rangeActions,
                                      Unit unit,
                                      MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
        super(optimizedCnecs, rangeActions, unit, maxMinRelativeMarginParameters);
        this.initialFlowResult = initialFlowResult;
        this.unit = unit;
        this.ptdfSumLowerBound = maxMinRelativeMarginParameters.getPtdfSumLowerBound();
        this.negativeMarginObjectiveCoefficient = maxMinRelativeMarginParameters.getNegativeMarginObjectiveCoefficient();
        this.highestThreshold = maxMinRelativeMarginParameters.getHighestThresholdValue();
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        super.fill(linearProblem, flowResult, sensitivityResult);
        buildMinimumRelativeMarginVariable(linearProblem);
        buildMinimumRelativeMarginConstraints(linearProblem);
        fillObjectiveWithMinRelMargin(linearProblem);
    }

    /**
     * Add a new minimum relative margin variable. Unfortunately, we cannot force it to be positive since it
     * should be able to be negative in unsecured cases (see constraints)
     */
    private void buildMinimumRelativeMarginVariable(LinearProblem linearProblem) {
        if (!optimizedCnecs.isEmpty()) {
            linearProblem.addMinimumRelativeMarginVariable(-LinearProblem.infinity(), LinearProblem.infinity());
        } else {
            // if there is no Cnecs, the minRelativeMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumRelativeMarginVariable(0.0, 0.0);
        }
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints(LinearProblem linearProblem) {
        MPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        MPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();

        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        if (minRelMarginSignBinaryVariable == null)  {
            throw new FaraoException("Minimum relative margin sign binary variable has not yet been created");
        }
        double maxPositiveRelativeRam = highestThreshold / ptdfSumLowerBound;
        double maxNegativeRelativeRam = 5 * maxPositiveRelativeRam;
        // Minimum Margin is negative or null
        minimumMarginVariable.setUb(.0);
        // Minimum Relative Margin is positive or null
        minRelMarginVariable.setLb(.0);
        // Forcing minRelMarginVariable to 0 when minimumMarginVariable is negative
        MPConstraint minimumRelativeMarginSetToZero = linearProblem.addMinimumRelMarginSetToZeroConstraint(-LinearProblem.infinity(), 0);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginSignBinaryVariable, -maxPositiveRelativeRam);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginVariable, 1);

        optimizedCnecs.forEach(cnec -> {
            double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(cnec), ptdfSumLowerBound);
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
            double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnec, Side.LEFT, unit, MEGAWATT);
            //TODO : check that using only Side.LEFT is sufficient

            if (minFlow.isPresent()) {
                MPConstraint minimumMarginNegative = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), -minFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam, cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginNegative.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), maxFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam, cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginPositive.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        });
    }

    private void fillObjectiveWithMinRelMargin(LinearProblem linearProblem) {
        MPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        linearProblem.getObjective().setCoefficient(minRelMarginVariable, -1);
    }
}
