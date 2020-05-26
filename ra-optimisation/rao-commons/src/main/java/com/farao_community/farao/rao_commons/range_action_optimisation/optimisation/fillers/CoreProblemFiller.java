/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.range_action_optimisation.optimisation.LinearRaoProblem;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller implements ProblemFiller {

    @Override
    public void fill(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        // add variables
        buildFlowVariables(raoData, linearRaoProblem);
        buildRangeActionSetPointVariables(raoData, linearRaoProblem);
        buildRangeActionAbsoluteVariationVariables(raoData, linearRaoProblem);

        // add constraints
        buildFlowConstraints(raoData, linearRaoProblem, fillerParameters);
        buildRangeActionConstraints(raoData, linearRaoProblem);
    }

    @Override
    public void update(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        // update reference flow and sensitivities of flow constraints
        updateFlowConstraints(raoData, linearRaoProblem, fillerParameters);
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables(RaoData raoData, LinearRaoProblem linearRaoProblem) {
        raoData.getCrac().getCnecs().forEach(cnec ->
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
    private void buildRangeActionSetPointVariables(RaoData raoData, LinearRaoProblem linearRaoProblem) {
        raoData.getCrac().getRangeActions().forEach(rangeAction -> {
            double minSetPoint = rangeAction.getMinValue(raoData.getNetwork());
            double maxSetPoint = rangeAction.getMaxValue(raoData.getNetwork());
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
    private void buildRangeActionAbsoluteVariationVariables(RaoData raoData, LinearRaoProblem linearRaoProblem) {
        raoData.getCrac().getRangeActions().forEach(rangeAction ->
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
    private void buildFlowConstraints(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        raoData.getCrac().getCnecs().forEach(cnec -> {
            // create constraint
            double referenceFlow = raoData.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearRaoProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec);

            MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(raoData, linearRaoProblem, cnec, fillerParameters);
        });
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints(RaoData raoData, LinearRaoProblem linearRaoProblem, FillerParameters fillerParameters) {
        raoData.getCrac().getCnecs().forEach(cnec -> {
            double referenceFlow = raoData.getReferenceFlow(cnec);
            MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec);
            if (flowConstraint == null) {
                throw new FaraoException(String.format("Flow constraint on %s has not been defined yet.", cnec.getId()));
            }

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(raoData, linearRaoProblem, cnec, fillerParameters);
        });
    }

    private void addImpactOfRangeActionOnCnec(RaoData raoData, LinearRaoProblem linearRaoProblem, Cnec cnec, FillerParameters fillerParameters) {
        MPVariable flowVariable = linearRaoProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearRaoProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(String.format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        raoData.getCrac().getRangeActions().forEach(rangeAction -> {
            if (rangeAction instanceof PstRange) {
                addImpactOfPstOnCnec(raoData, linearRaoProblem, rangeAction, cnec, flowConstraint, fillerParameters);
            } else {
                throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
            }
        });
    }

    private void addImpactOfPstOnCnec(RaoData raoData, LinearRaoProblem linearRaoProblem, RangeAction rangeAction, Cnec cnec, MPConstraint flowConstraint, FillerParameters fillerParameters) {
        MPVariable setPointVariable = linearRaoProblem.getRangeActionSetPointVariable(rangeAction);
        if (setPointVariable == null) {
            throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double sensitivity = raoData.getSensitivity(cnec, rangeAction);

        if (Math.abs(sensitivity) >= fillerParameters.getPstSensitivityThreshold()) {
            double currentSetPoint = rangeAction.getCurrentValue(raoData.getNetwork());
            // care : might not be robust as getCurrentValue get the current setPoint from a network variant
            //        we need to be sure that this variant has been properly set
            flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -sensitivity);
        }
    }

    /**
     * Build two range action constraints for each RangeAction r.
     * These constraints link the set point variable of the RangeAction with its absolute
     * variation variable.
     *
     * AV[r] >= S[r] - initialSetPoint[r]     (NEGATIVE)
     * AV[r] >= initialSetPoint[r] - S[r]     (POSITIVE)
     */
    private void buildRangeActionConstraints(RaoData raoData, LinearRaoProblem linearRaoProblem) {
        raoData.getCrac().getRangeActions().forEach(rangeAction -> {
            double initialSetPoint = rangeAction.getCurrentValue(raoData.getNetwork());
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
