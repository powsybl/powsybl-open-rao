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
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.search_tree_rao.commons.parameters.MaxMinRelativeMarginParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPConstraint;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.FaraoMPVariable;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;

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
    private final double highestThreshold;

    public MaxMinRelativeMarginFiller(Set<FlowCnec> optimizedCnecs,
                                      FlowResult initialFlowResult,
                                      Unit unit,
                                      MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
        super(optimizedCnecs, unit);
        this.initialFlowResult = initialFlowResult;
        this.unit = unit;
        this.ptdfSumLowerBound = maxMinRelativeMarginParameters.getPtdfSumLowerBound();
        this.highestThreshold = RaoUtil.getLargestCnecThreshold(optimizedCnecs, MEGAWATT);
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        super.fill(linearProblem, flowResult, sensitivityResult);
        buildMinimumRelativeMarginSignBinaryVariable(linearProblem);
        updateMinimumNegativeMarginDefinition(linearProblem);
        buildMinimumRelativeMarginVariable(linearProblem);
        buildMinimumRelativeMarginConstraints(linearProblem);
        fillObjectiveWithMinRelMargin(linearProblem);
    }

    private void updateMinimumNegativeMarginDefinition(LinearProblem linearProblem) {
        FaraoMPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        FaraoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        double maxNegativeRam = 5 * highestThreshold;

        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }
        if (minRelMarginSignBinaryVariable == null)  {
            throw new FaraoException("Minimum relative margin sign binary variable has not yet been created");
        }
        // Minimum Margin is negative or zero
        minimumMarginVariable.setUb(.0);
        // Forcing miminumRelativeMarginSignBinaryVariable to 0 when minimumMarginVariable is negative
        FaraoMPConstraint minimumRelMarginSignDefinition = linearProblem.addMinimumRelMarginSignDefinitionConstraint(-LinearProblem.infinity(), maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minRelMarginSignBinaryVariable, maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minimumMarginVariable, -1);
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
     * Build the  miminum relative margin sign binary variable, P.
     * P represents the sign of the minimum margin.
     */
    private void buildMinimumRelativeMarginSignBinaryVariable(LinearProblem linearProblem) {
        linearProblem.addMinimumRelativeMarginSignBinaryVariable();
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints(LinearProblem linearProblem) {
        FaraoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        FaraoMPVariable minRelMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();

        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        if (minRelMarginSignBinaryVariable == null)  {
            throw new FaraoException("Minimum relative margin sign binary variable has not yet been created");
        }
        double maxPositiveRelativeRam = highestThreshold / ptdfSumLowerBound;
        double maxNegativeRelativeRam = 5 * maxPositiveRelativeRam;
        // Minimum Relative Margin is positive or null
        minRelMarginVariable.setLb(.0);
        // Forcing minRelMarginVariable to 0 when minimumMarginVariable is negative
        FaraoMPConstraint minimumRelativeMarginSetToZero = linearProblem.addMinimumRelMarginSetToZeroConstraint(-LinearProblem.infinity(), 0);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginSignBinaryVariable, -maxPositiveRelativeRam);
        minimumRelativeMarginSetToZero.setCoefficient(minRelMarginVariable, 1);

        optimizedCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            double relMarginCoef = Math.max(initialFlowResult.getPtdfZonalSum(cnec, side), ptdfSumLowerBound);
            FaraoMPVariable flowVariable = linearProblem.getFlowVariable(cnec, side);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s (side %s)", cnec.getId(), side));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(side, MEGAWATT);
            maxFlow = cnec.getUpperBound(side, MEGAWATT);
            double unitConversionCoefficient = RaoUtil.getFlowUnitMultiplier(cnec, side, unit, MEGAWATT);

            if (minFlow.isPresent()) {
                FaraoMPConstraint minimumMarginNegative = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), -minFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam, cnec, side, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginNegative.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                FaraoMPConstraint minimumMarginPositive = linearProblem.addMinimumRelativeMarginConstraint(-LinearProblem.infinity(), maxFlow.get() + unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam, cnec, side, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginPositive.setCoefficient(minRelMarginSignBinaryVariable, unitConversionCoefficient * relMarginCoef * maxNegativeRelativeRam);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        }));
    }

    private void fillObjectiveWithMinRelMargin(LinearProblem linearProblem) {
        FaraoMPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        linearProblem.getObjective().setCoefficient(minRelMarginVariable, -1);
    }
}
