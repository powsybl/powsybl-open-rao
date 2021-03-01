/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {

    private Unit unit;
    private double pstPenaltyCost;
    private Set<String> operatorsNotToOptimize;

    public MaxMinMarginFiller(Unit unit, double pstPenaltyCost, Set<String> operatorsNotToOptimize) {
        this.unit = unit;
        this.pstPenaltyCost = pstPenaltyCost;
        if (!Objects.isNull(operatorsNotToOptimize)) {
            this.operatorsNotToOptimize = operatorsNotToOptimize;
        } else {
            this.operatorsNotToOptimize = new HashSet<>();
        }
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }
    // End of methods for tests

    @Override
    public void fill(RaoData raoData, LinearProblem linearProblem) {
        // build variables
        buildMarginDecreaseVariables(raoData, linearProblem);
        buildMinimumMarginVariable(linearProblem, raoData);

        // build constraints
        buildMarginDecreaseConstraints(raoData, linearProblem);
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
    private void buildMinimumMarginVariable(LinearProblem linearProblem, RaoData raoData) {

        if (raoData.getCnecs().stream().anyMatch(BranchCnec::isOptimized)) {
            linearProblem.addMinimumMarginVariable(-linearProblem.infinity(), linearProblem.infinity());
        } else {
            // if there is no Cnecs, the minMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumMarginVariable(0.0, 0.0);
        }
    }

    /**
     * This method defines, for each CNEC belonging to a TSO that does not share RAs in the given perimeter, a binary variable
     * The binary variable should detect the decrease of the margin on the given CNEC compared to the preperimeter margin
     * The variable should be equal to 1 if there is a decrease
     */
    private void buildMarginDecreaseVariables(RaoData raoData, LinearProblem linearProblem) {
        getCnecsForOperatorsNotToOptimize(raoData).forEach(linearProblem::addMarginDecreaseBinaryVariable);
    }

    Stream<BranchCnec> getCnecsForOperatorsNotToOptimize(RaoData raoData) {
        return raoData.getCnecs().stream()
                .filter(BranchCnec::isOptimized)
                .filter(cnec -> operatorsNotToOptimize.contains(cnec.getOperator()));
    }

    /**
     * This method defines, for each CNEC belonging to a TSO that does not share RAs in the given perimeter, a constraint
     * The constraint defines the behaviour of the binary variable "margin decrease"
     * margin >= margin_preperimeter - margin_decrease * bigM
     * => (1) -flow + margin_decrease * bigM >= margin_preperimeter - maxFlow
     * and (2) flow + margin_decrease * bigM >= margin_preperimeter + minFlow
     * bigM is computed to be equal to the maximum margin decrease possible, which is the amount that decreases the cnec's margin to the initial worst margin
     */
    private void buildMarginDecreaseConstraints(RaoData raoData, LinearProblem linearProblem) {
        if (operatorsNotToOptimize.isEmpty()) {
            return;
        }

        double finalWorstMargin = getMinPossibleMarginOnPerimeter(raoData);
        // No margin should be smaller than the worst margin computed above, otherwise it means the linear optimizer or the search tree rao is degrading the situation
        // So we can use this to estimate the worst decrease possible of the margins on cnecs
        RaoParameters.ObjectiveFunction objFunction = raoData.getRaoParameters().getObjectiveFunction();
        String prePerimeterVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        getCnecsForOperatorsNotToOptimize(raoData).forEach(cnec -> {
            double initialMargin = RaoUtil.computeCnecMargin(cnec, prePerimeterVariantId, objFunction.getUnit(), false);
            double worstMarginDecrease = initialMargin - finalWorstMargin; // cant' be negative !

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }
            MPVariable marginDecreaseBinaryVariable = linearProblem.getMarginDecreaseBinaryVariable(cnec);
            if (marginDecreaseBinaryVariable == null) {
                throw new FaraoException(String.format("Margin decrease binary variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);

            if (minFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addMarginDecreaseConstraint(initialMargin + minFlow.get(), linearProblem.infinity(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, 1);
                decreaseMinmumThresholdMargin.setCoefficient(marginDecreaseBinaryVariable, worstMarginDecrease);
            }

            if (maxFlow.isPresent()) {
                MPConstraint decreaseMinmumThresholdMargin = linearProblem.addMarginDecreaseConstraint(initialMargin - maxFlow.get(), linearProblem.infinity(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                decreaseMinmumThresholdMargin.setCoefficient(flowVariable, -1);
                decreaseMinmumThresholdMargin.setCoefficient(marginDecreaseBinaryVariable, worstMarginDecrease);
            }
        });
    }

    /**
     * Get the minimum possible margin on the current perimeter
     * It's the minimum between the worst margin in the preperimeter variant, and the one in the current leaf before range actions optimization
     */
    double getMinPossibleMarginOnPerimeter(RaoData raoData) {
        RaoParameters.ObjectiveFunction objFunction = raoData.getRaoParameters().getObjectiveFunction();

        String preOptimVariantId = raoData.getCracVariantManager().getPreOptimVariantId();
        BranchCnec mostLimitingElement = RaoUtil.getMostLimitingElement(raoData.getCnecs(), preOptimVariantId, objFunction.getUnit(), false);
        double preOptimVariantWorstMargin = RaoUtil.computeCnecMargin(mostLimitingElement, preOptimVariantId, objFunction.getUnit(), false);

        String prePerimeterVariantId = raoData.getCrac().getExtension(ResultVariantManager.class).getPrePerimeterVariantId();
        mostLimitingElement = RaoUtil.getMostLimitingElement(raoData.getCnecs(), prePerimeterVariantId, objFunction.getUnit(), false);
        double prePerimeterVariantWorstMargin = RaoUtil.computeCnecMargin(mostLimitingElement, prePerimeterVariantId, objFunction.getUnit(), false);

        return Math.min(preOptimVariantWorstMargin, prePerimeterVariantWorstMargin);
    }

    /**
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definition
     * of the min margin : MM = min{c in CNEC} margin[c].
     * <p>
     * For each Cnec c, the two constraints are (if the max margin is defined in MEGAWATT) :
     * <p>
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     * <p>
     * For each Cnec c, the two constraints are (if the max margin is defined in AMPERE) :
     * <p>
     * MM <= (fmax[c] - F[c]) * 1000 / (Unom * sqrt(3))     (ABOVE_THRESHOLD)
     * MM <= (F[c] - fmin[c]) * 1000 / (Unom * sqrt(3))     (BELOW_THRESHOLD)
     */
    private void buildMinimumMarginConstraints(RaoData raoData, LinearProblem linearProblem) {
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }
        raoData.getCnecs().stream().filter(BranchCnec::isOptimized).forEach(cnec -> {
            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);

            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable has not yet been created for Cnec %s", cnec.getId()));
            }

            Optional<Double> minFlow;
            Optional<Double> maxFlow;
            minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
            maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
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

        // For CNECs of operators not sharing RAs, deactivate their participation in the definition of the minimum margin
        // if their margin is not decreased (ie margin_decrease = 0)
        // Do this by adding (1 - margin_decrease) * bigM to the right side of the inequality
        // bigM is computed as 2 times the largest absolute threshold between all CNECs
        // Of course this can be restrictive as CNECs can have hypothetically infinite margins if they are monitored in one direction only
        // But we'll suppose for now that the minimum margin can never be greater than 1 * the largest threshold
        double bigM = 2 * getLargestCnecThreshold(raoData);
        getCnecsForOperatorsNotToOptimize(raoData).forEach(cnec -> {
            MPVariable marginDecreaseBinaryVariable = linearProblem.getMarginDecreaseBinaryVariable(cnec);
            if (marginDecreaseBinaryVariable == null) {
                throw new FaraoException(String.format("Margin decrease binary variable has not yet been created for Cnec %s", cnec.getId()));
            }
            MPConstraint minimumMarginNegative = linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
            if (minimumMarginNegative != null) {
                minimumMarginNegative.setCoefficient(marginDecreaseBinaryVariable, bigM);
                minimumMarginNegative.setUb(minimumMarginNegative.ub() + bigM);
            }
            MPConstraint minimumMarginPositive = linearProblem.getMinimumMarginConstraint(cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
            if (minimumMarginPositive != null) {
                minimumMarginPositive.setCoefficient(marginDecreaseBinaryVariable, bigM);
                minimumMarginPositive.setUb(minimumMarginPositive.ub() + bigM);
            }
        });
    }

    double getLargestCnecThreshold(RaoData raoData) {
        double max = 0;
        for (BranchCnec cnec : raoData.getCnecs()) {
            if (cnec.isOptimized()) {
                Optional<Double> minFlow = cnec.getLowerBound(Side.LEFT, MEGAWATT);
                if (minFlow.isPresent() && Math.abs(minFlow.get()) > max) {
                    max = Math.abs(minFlow.get());
                }
                Optional<Double> maxFlow = cnec.getUpperBound(Side.LEFT, MEGAWATT);
                if (maxFlow.isPresent() && Math.abs(maxFlow.get()) > max) {
                    max = Math.abs(maxFlow.get());
                }
            }
        }
        return max;
    }

    /**
     * Add in the objective function of the linear problem the min Margin.
     * <p>
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
     * <p>
     * min( sum{r in RangeAction} penaltyCost[r] - AV[r] )
     */
    private void fillObjectiveWithRangeActionPenaltyCost(RaoData raoData, LinearProblem linearProblem) {
        raoData.getAvailableRangeActions().forEach(rangeAction -> {
            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            // If the PST has been filtered out, then absoluteVariationVariable is null
            if (absoluteVariationVariable != null && rangeAction instanceof PstRangeAction) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, pstPenaltyCost);
            }
        });
    }

    /**
     * Get unit conversion coefficient
     * the flows are always defined in MW, so if the minimum margin is defined in ampere,
     * and appropriate conversion coefficient should be used.
     */
    protected double getUnitConversionCoefficient(BranchCnec cnec, RaoData linearRaoData) {
        if (unit.equals(MEGAWATT)) {
            return 1;
        } else {
            // Unom(cnec) * sqrt(3) / 1000
            return linearRaoData.getNetwork().getBranch(cnec.getNetworkElement().getId()).getTerminal1().getVoltageLevel().getNominalV() * Math.sqrt(3) / 1000;
        }
    }
}

