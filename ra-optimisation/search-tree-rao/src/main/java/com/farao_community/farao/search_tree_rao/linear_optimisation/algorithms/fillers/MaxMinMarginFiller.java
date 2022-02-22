/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.farao_community.farao.rao_api.parameters.MaxMinMarginParameters;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.LinearProblem;
import com.farao_community.farao.search_tree_rao.result.api.FlowResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionResult;
import com.farao_community.farao.search_tree_rao.result.api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MaxMinMarginFiller implements ProblemFiller {
    protected final Set<FlowCnec> optimizedCnecs;
    private final Set<RangeAction<?>> rangeActions;
    private final Unit unit;
    private final double pstPenaltyCost;
    private final double hvdcPenaltyCost;
    private final double injectionPenaltyCost;
    private final double highestThreshold;

    public MaxMinMarginFiller(Set<FlowCnec> optimizedCnecs, Set<RangeAction<?>> rangeActions, Unit unit, MaxMinMarginParameters maxMinMarginParameters) {
        this.optimizedCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.optimizedCnecs.addAll(optimizedCnecs);
        this.rangeActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.rangeActions.addAll(rangeActions);
        this.unit = unit;
        this.pstPenaltyCost = maxMinMarginParameters.getPstPenaltyCost();
        this.hvdcPenaltyCost = maxMinMarginParameters.getHvdcPenaltyCost();
        this.injectionPenaltyCost = maxMinMarginParameters.getInjectionPenaltyCost();
        this.highestThreshold = maxMinMarginParameters.getHighestThresholdValue();
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // build variables
        buildMinimumMarginVariable(linearProblem);
        buildMinimumRelativeMarginSignBinaryVariable(linearProblem);

        // build constraints
        buildMinimumMarginConstraints(linearProblem);

        // complete objective
        fillObjectiveWithMinMargin(linearProblem);
        fillObjectiveWithRangeActionPenaltyCost(linearProblem);
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        // Objective does not change, nothing to do
    }

    /**
     * Build the minimum margin variable MM.
     * MM represents the smallest margin of all Cnecs.
     * It is given in MEGAWATT.
     */
    private void buildMinimumMarginVariable(LinearProblem linearProblem) {
        if (!optimizedCnecs.isEmpty()) {
            linearProblem.addMinimumMarginVariable(-LinearProblem.infinity(), LinearProblem.infinity());
        } else {
            // if there is no Cnecs, the minMarginVariable is forced to zero.
            // otherwise it would be unbounded in the LP
            linearProblem.addMinimumMarginVariable(0.0, 0.0);
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
     * Build two minimum margin constraints for each Cnec c.
     * The minimum margin constraints ensure that the minimum margin variable is below
     * the margin of each Cnec. They consist in a linear equivalent of the definition
     * of the min margin : MM = min{c in CNEC} margin[c].
     * <p>
     * For each Cnec c, the 4 constraints are (if the max margin is defined in MEGAWATT) :
     * <p>
     * MM <= fmax[c] - F[c]    (ABOVE_THRESHOLD)
     * MM <= F[c] - fmin[c]    (BELOW_THRESHOLD)
     * - (1 - P) * mMinRAM <= MM <= 0
     * MM <= 0
     * <p>
     * For each Cnec c, the 4 constraints are (if the max margin is defined in AMPERE) :
     * <p>
     * MM <= (fmax[c] - F[c]) * 1000 / (Unom * sqrt(3))     (ABOVE_THRESHOLD)
     * MM <= (F[c] - fmin[c]) * 1000 / (Unom * sqrt(3))     (BELOW_THRESHOLD)
     * - (1 - P) * mMinRAM <= MM
     * MM <= 0
     */
    private void buildMinimumMarginConstraints(LinearProblem linearProblem) {
        MPVariable minimumMarginVariable = linearProblem.getMinimumMarginVariable();
        MPVariable miminumRelativeMarginSignBinaryVariable = linearProblem.getMinimumRelativeMarginSignBinaryVariable();
        if (minimumMarginVariable == null) {
            throw new FaraoException("Minimum margin variable has not yet been created");
        }

        // Minimum Margin is negative or null
        minimumMarginVariable.setUb(.0);
        // Forcing miminumRelativeMarginSignBinaryVariable to 0 when minimumMarginVariable is negative
        double maxNegativeRam = 5 * highestThreshold;
        MPConstraint minimumRelMarginSignDefinition = linearProblem.addMinimumRelMarginSignDefinitionConstraint(-LinearProblem.infinity(), maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(miminumRelativeMarginSignBinaryVariable, maxNegativeRam);
        minimumRelMarginSignDefinition.setCoefficient(minimumMarginVariable, -1);

        optimizedCnecs.forEach(cnec -> {
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
                MPConstraint minimumMarginNegative = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), -minFlow.get(), cnec, LinearProblem.MarginExtension.BELOW_THRESHOLD);
                minimumMarginNegative.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginNegative.setCoefficient(flowVariable, -1);
            }

            if (maxFlow.isPresent()) {
                MPConstraint minimumMarginPositive = linearProblem.addMinimumMarginConstraint(-LinearProblem.infinity(), maxFlow.get(), cnec, LinearProblem.MarginExtension.ABOVE_THRESHOLD);
                minimumMarginPositive.setCoefficient(minimumMarginVariable, unitConversionCoefficient);
                minimumMarginPositive.setCoefficient(flowVariable, 1);
            }
        });
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
    private void fillObjectiveWithRangeActionPenaltyCost(LinearProblem linearProblem) {
        rangeActions.forEach(rangeAction -> {
            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            // If the range action has been filtered out, then absoluteVariationVariable is null
            if (absoluteVariationVariable != null && rangeAction instanceof PstRangeAction) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, pstPenaltyCost);
            } else if (absoluteVariationVariable != null && rangeAction instanceof HvdcRangeAction) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, hvdcPenaltyCost);
            } else if (absoluteVariationVariable != null && rangeAction instanceof InjectionRangeAction) {
                linearProblem.getObjective().setCoefficient(absoluteVariationVariable, injectionPenaltyCost);
            }
        });
    }
}

