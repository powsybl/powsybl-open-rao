/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.linear_optimisation.parameters.MaxMinRelativeMarginParameters;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {
    private final Map<BranchCnec, Double> initialAbsolutePtdfSumPerOptimizedCnec;
    private final Unit unit;
    private final double ptdfSumLowerBound;
    private final double negativeMarginObjectiveCoefficient;

    public MaxMinRelativeMarginFiller(LinearProblem linearProblem,
                                      Map<BranchCnec, Double> initialAbsolutePtdfSumPerOptimizedCnec,
                                      Set<RangeAction> rangeActions,
                                      Unit unit,
                                      MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
        super(linearProblem, initialAbsolutePtdfSumPerOptimizedCnec.keySet(), rangeActions, unit, maxMinRelativeMarginParameters);
        this.initialAbsolutePtdfSumPerOptimizedCnec = initialAbsolutePtdfSumPerOptimizedCnec;
        this.unit = unit;
        this.ptdfSumLowerBound = maxMinRelativeMarginParameters.getPtdfSumLowerBound();
        this.negativeMarginObjectiveCoefficient = maxMinRelativeMarginParameters.getNegativeMarginObjectiveCoefficient();
    }

    final Map<BranchCnec, Double> getInitialAbsolutePtdfSumPerOptimizedCnec() {
        return initialAbsolutePtdfSumPerOptimizedCnec;
    }

    final double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    final double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    @Override
    public void fill(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        super.fill(sensitivityAndLoopflowResults);
        updateMinimumNegativeMarginDefinitionAndCost();
        buildMinimumRelativeMarginVariable();
        buildMinimumRelativeMarginConstraints();
        fillObjectiveWithMinRelMargin();
    }

    /**
     * Force the minimum margin variable (absolute margin) to be negative (unsecured case)
     * Add a big coefficient to it in the objective function, in order to render it the primary objective
     */
    private void updateMinimumNegativeMarginDefinitionAndCost() {
        MPVariable minNegMargin = linearProblem.getMinimumMarginVariable();
        if (minNegMargin == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }
        minNegMargin.setUb(.0);
        MPObjective objective = linearProblem.getObjective();
        objective.setCoefficient(minNegMargin, -1 * negativeMarginObjectiveCoefficient);
    }

    /**
     * Add a new minimum relative margin variable. Unfortunately, we cannot force it to be positive since it
     * should be able to be negative in unsecured cases (see constraints)
     */
    private void buildMinimumRelativeMarginVariable() {
        linearProblem.addMinimumRelativeMarginVariable(-linearProblem.infinity(), linearProblem.infinity());
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints() {
        MPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        optimizedCnecs.forEach(cnec -> {
            double relMarginCoef = Math.max(initialAbsolutePtdfSumPerOptimizedCnec.get(cnec), ptdfSumLowerBound);
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
            double unitConversionCoefficient = RaoUtil.getBranchFlowUnitMultiplier(cnec, Side.LEFT, unit, MEGAWATT);
            //TODO : check that using only Side.LEFT is sufficient

            if (minFlow.isPresent()) {
                MPConstraint minimumMarginNegative = linearProblem.addMinimumRelativeMarginConstraint(-linearProblem.infinity(), -minFlow.get(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearProblem.addMinimumRelativeMarginConstraint(-linearProblem.infinity(), maxFlow.get(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient * relMarginCoef);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        });
    }

    private void fillObjectiveWithMinRelMargin() {
        MPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        linearProblem.getObjective().setCoefficient(minRelMarginVariable, -1);
    }
}
