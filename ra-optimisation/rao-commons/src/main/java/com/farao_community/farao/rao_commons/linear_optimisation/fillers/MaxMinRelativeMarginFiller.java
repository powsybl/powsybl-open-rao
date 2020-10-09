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
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Map;
import java.util.Optional;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class MaxMinRelativeMarginFiller extends MaxMinMarginFiller {

    double absMarginObjCoef;

    public MaxMinRelativeMarginFiller(Unit unit, double pstPenaltyCost, double absMarginObjCoef) {
        super(unit, pstPenaltyCost, false);
        this.absMarginObjCoef = absMarginObjCoef;
    }

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        super.fill(raoData, linearProblem);
        updateMinimumNegativeMarginDefinitionAndCost(linearProblem);
        buildMinimumRelativeMarginVariable(linearProblem);
        buildMinimumRelativeMarginConstraints(raoData, linearProblem);
        fillObjectiveWithMinRelMargin(linearProblem);
    }

    /**
     * Force the minimum margin variable (absolute margin) to be negative (unsecured case)
     * Add a big coefficient to it in the objective function, in order to render it the primary objective
     */
    private void updateMinimumNegativeMarginDefinitionAndCost(LinearProblem linearProblem) {
        MPVariable minAbsMargin = linearProblem.getMinimumMarginVariable();
        if (minAbsMargin == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }
        minAbsMargin.setUb(.0);
        MPObjective objective = linearProblem.getObjective();
        objective.setCoefficient(minAbsMargin, absMarginObjCoef);
    }

    /**
     * Add a new minimum relative margin variable. Unfortunately, we cannot force it to be positive since it
     * should be able to be negative in unsecured cases (see constraints)
     */
    private void buildMinimumRelativeMarginVariable(LinearProblem linearProblem) {
        linearProblem.addMinimumMarginVariable(-linearProblem.infinity(), linearProblem.infinity());
    }

    /**
     * Define the minimum relative margin (like absolute margin but by dividing by sum of PTDFs)
     */
    private void buildMinimumRelativeMarginConstraints(RaoData raoData, LinearProblem linearProblem) {
        MPVariable minRelMarginVariable = linearProblem.getMinimumRelativeMarginVariable();
        if (minRelMarginVariable == null) {
            throw new FaraoException("Minimum relative margin variable has not yet been created");
        }
        Map<String, Double> ptdfSums = raoData.getCracResult(raoData.getInitialVariantId()).getAbsPtdfSums();
        raoData.getCnecs().stream().filter(Cnec::isOptimized).forEach(cnec -> {
            double marginCoef = 1 / ptdfSums.get(cnec.getId());
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getMinThreshold(MEGAWATT);
            maxFlow = cnec.getMaxThreshold(MEGAWATT);
            double unitConversionCoefficient = getUnitConversionCoefficient(cnec, raoData);

            if (minFlow.isPresent()) {
                MPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), -minFlow.get(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minRelMarginVariable, unitConversionCoefficient);
                minimumMarginNegative.setCoefficient(flowVariable, -1 * marginCoef);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-linearProblem.infinity(), maxFlow.get(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minRelMarginVariable, unitConversionCoefficient);
                minimumMarginPositive.setCoefficient(flowVariable, 1 * marginCoef);
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
