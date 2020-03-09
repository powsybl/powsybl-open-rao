/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.linear_rao.AbstractProblemFiller;
import com.farao_community.farao.linear_rao.LinearRaoData;
import com.farao_community.farao.linear_rao.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller extends AbstractProblemFiller {

    public CoreProblemFiller(LinearRaoProblem linearRaoProblem, LinearRaoData linearRaoData) {
        super(linearRaoProblem, linearRaoData);
    }

    @Override
    public void fill() {
        // add variables
        buildFlowVariables();
        buildRangeActionSetPointVariables();
        buildRangeActionAbsoluteVariationVariables();

        // add constraints
        buildFlowConstraints();
        buildRangeActionConstraints();
    }

    @Override
    public void update() {
        // update reference flow and sensitivities of flow constraints
        updateFlowConstraints();
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables() {
        linearRaoData.getCrac().getCnecs().forEach(cnec ->
                linearRaoProblem.addFlowVariable(-linearRaoProblem.infinity(), linearRaoProblem.infinity(), cnec)
        );
    }

    /**
     * Build one set point variable S[r] for each RangeAction r
     * This variable describes the set point of the given RangeAction r, given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     * </ul>
     *
     * This set point of the a RangeAction is bounded between the min/max variations
     * of the RangeAction :
     *
     * initialSetPoint[r] - maxNegativeVariation[r] <= S[r]
     * S[r] >= initialSetPoint[r] + maxPositiveVariation[r]
     */
    private void buildRangeActionSetPointVariables() {
        linearRaoData.getCrac().getRangeActions().forEach(rangeAction -> {
            double minSetPoint = rangeAction.getMinValue(linearRaoData.getNetwork());
            double maxSetPoint = rangeAction.getMaxValue(linearRaoData.getNetwork());
            linearRaoProblem.addRangeActionSetPointVariable(minSetPoint, maxSetPoint, rangeAction);
        });
    }

    /**
     * Build one absolute variable variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action set point
     * and its initial value. It is given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     * </ul>
     */
    private void buildRangeActionAbsoluteVariationVariables() {
        linearRaoData.getCrac().getRangeActions().forEach(rangeAction ->
                linearRaoProblem.addAbsoluteRangeActionVariationVariable(0, linearRaoProblem.infinity(), rangeAction)
        );
    }

    /**
     * Build one flow constraint for each Cnec c.
     * This constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints() {
        linearRaoData.getCrac().getCnecs().forEach(cnec -> {
            // create constraint
            double referenceFlow = linearRaoData.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearRaoProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec);

            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(cnec);
        });
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints() {
        linearRaoData.getCrac().getCnecs().forEach(cnec -> {
            double referenceFlow = linearRaoData.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec);
            if (flowConstraint == null) {
                throw new FaraoException(String.format("Flow constraint on %s has not been defined yet.", cnec.getId()));
            }

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(cnec);
        });
    }

    private void addImpactOfRangeActionOnCnec(Cnec cnec) {
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(String.format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        linearRaoData.getCrac().getRangeActions().forEach(rangeAction -> {
            MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
            if (setPointVariable == null) {
                throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
            }

            double sensitivity = rangeAction.getSensitivityValue(linearRaoData.getSensitivityComputationResults(cnec.getState()), cnec);
            double currentSetPoint = rangeAction.getCurrentValue(linearRaoData.getNetwork());
            // care : might not be robust as getCurrentValue get the current setPoint from a network variant
            //        we need to be sure that this variant has been properly set

            flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -sensitivity);
        });
    }

    /**
     * Build two range action constraints for each RangeAction r.
     * These constraints link the set point variable of the RangeAction with its absolute
     * variation variable.
     *
     * AV[r] >= S[r] - initialSetPoint[r]     (NEGATIVE)
     * AV[r] >= initialSetPoint[r] - S[r]     (POSITIVE)
     */
    private void buildRangeActionConstraints() {
        linearRaoData.getCrac().getRangeActions().forEach(rangeAction -> {
            double initialSetPoint = rangeAction.getCurrentValue(linearRaoData.getNetwork());
            MPConstraint varConstraintNegative = linearRaoProblem.addAbsoluteRangeActionVariationConstraint(-initialSetPoint, linearRaoProblem.infinity(), rangeAction, LinearRaoProblem.AbsExtension.NEGATIVE);
            MPConstraint varConstraintPositive = linearRaoProblem.addAbsoluteRangeActionVariationConstraint(initialSetPoint, linearRaoProblem.infinity(), rangeAction, LinearRaoProblem.AbsExtension.POSITIVE);

            MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
            MPVariable absoluteVariationVariable = linearRaoProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);

            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
        });
    }
}
