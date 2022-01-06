/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.*;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CoreProblemFiller implements ProblemFiller {
    private final Network network;
    private final Set<FlowCnec> flowCnecs;
    private final Set<RangeAction<?>> rangeActions;
    private final RangeActionResult prePerimeterRangeActionResult;
    private final double pstSensitivityThreshold;
    private final double hvdcSensitivityThreshold;
    private final boolean relativePositiveMargins;

    public CoreProblemFiller(Network network,
                             Set<FlowCnec> flowCnecs,
                             Set<RangeAction<?>> rangeActions,
                             RangeActionResult prePerimeterRangeActionResult,
                             double pstSensitivityThreshold,
                             double hvdcSensitivityThreshold,
                             boolean relativePositiveMargins) {
        this.network = network;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(flowCnecs);
        this.rangeActions = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.rangeActions.addAll(rangeActions);
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
        this.relativePositiveMargins = relativePositiveMargins;
    }

    private Set<RangeAction<?>> getRangeActions() {
        return rangeActions;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        // add variables
        buildFlowVariables(linearProblem);
        getRangeActions().forEach(rangeAction -> {
            double prePerimeterSetpoint = prePerimeterRangeActionResult.getOptimizedSetPoint(rangeAction);
            buildRangeActionSetPointVariables(linearProblem, rangeAction, prePerimeterSetpoint);
            buildRangeActionAbsoluteVariationVariables(linearProblem, rangeAction);
        });

        // add constraints
        buildFlowConstraints(linearProblem, flowResult, sensitivityResult);
        buildRangeActionConstraints(linearProblem);
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        // update reference flow and sensitivities of flow constraints
        updateFlowConstraints(linearProblem, flowResult, sensitivityResult);
    }

    /**
     * Build one flow variable F[c] for each Cnec c
     * This variable describes the estimated flow on the given Cnec c, in MEGAWATT
     */
    private void buildFlowVariables(LinearProblem linearProblem) {
        flowCnecs.forEach(cnec ->
                linearProblem.addFlowVariable(-LinearProblem.infinity(), LinearProblem.infinity(), cnec)
        );
    }

    /**
     * Build one set point variable S[r] for each RangeAction r
     * This variable describes the set point of the given RangeAction r, given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     *     <li>in MEGAWATT for HVDC range actions</li>
     * </ul>
     *
     * This set point of the RangeAction is bounded between the min/max variations
     * of the RangeAction :
     *
     * initialSetPoint[r] - maxNegativeVariation[r] <= S[r]
     * S[r] >= initialSetPoint[r] + maxPositiveVariation[r]
     */
    private void buildRangeActionSetPointVariables(LinearProblem linearProblem, RangeAction<?> rangeAction, double prePerimeterValue) {
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterValue);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterValue);
        linearProblem.addRangeActionSetpointVariable(minSetPoint, maxSetPoint, rangeAction);
    }

    /**
     * Build one absolute variable variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action set point
     * and its initial value. It is given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     *     <li>in MEGAWATT for HVDC range actions</li>
     * </ul>
     */
    private void buildRangeActionAbsoluteVariationVariables(LinearProblem linearProblem, RangeAction<?> rangeAction) {
        linearProblem.addAbsoluteRangeActionVariationVariable(0, LinearProblem.infinity(), rangeAction);
    }

    /**
     * Build one flow constraint for each Cnec c.
     * This constraints link the estimated flow on a Cnec with the impact of the RangeActions
     * on this Cnec.
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void buildFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        flowCnecs.forEach(cnec -> {
            // create constraint
            double referenceFlow = flowResult.getFlow(cnec, Unit.MEGAWATT);
            MPConstraint flowConstraint = linearProblem.addFlowConstraint(referenceFlow, referenceFlow, cnec);

            MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
            if (flowVariable == null) {
                throw new FaraoException(format("Flow variable on %s has not been defined yet.", cnec.getId()));
            }

            flowConstraint.setCoefficient(flowVariable, 1);

            // add sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, flowResult, cnec);
        });
    }

    /**
     * Update the flow constraints, with the new reference flows and new sensitivities
     *
     * F[c] = f_ref[c] + sum{r in RangeAction} sensitivity[c,r] * (S[r] - currentSetPoint[r])
     */
    private void updateFlowConstraints(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
        flowCnecs.forEach(cnec -> {
            double referenceFlow = flowResult.getFlow(cnec, Unit.MEGAWATT);
            MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);
            if (flowConstraint == null) {
                throw new FaraoException(format("Flow constraint on %s has not been defined yet.", cnec.getId()));
            }

            //reset bounds
            flowConstraint.setUb(referenceFlow);
            flowConstraint.setLb(referenceFlow);

            //reset sensitivity coefficients
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, flowResult, cnec);
        });
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowResult flowResult, FlowCnec cnec) {
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        getRangeActions().forEach(rangeAction -> {
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, flowResult, rangeAction, cnec, flowConstraint);
        });
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowResult flowResult, RangeAction<?> rangeAction, FlowCnec cnec, MPConstraint flowConstraint) {
        MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction);
        if (setPointVariable == null) {
            throw new FaraoException(format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double sensitivity = sensitivityResult.getSensitivityValue(cnec, rangeAction, Unit.MEGAWATT);

        // If objective function uses relative positive margins, and if the margin on the cnec is positive,
        // the sensi should be divided by the absolute PTDF sum
        double sensiDivider = (relativePositiveMargins && flowResult.getMargin(cnec, Unit.MEGAWATT) > 0) ? flowResult.getPtdfZonalSum(cnec) : 1;
        if (isRangeActionSensitivityAboveThreshold(rangeAction, Math.abs(sensitivity / sensiDivider))) {
            double currentSetPoint = rangeAction.getCurrentSetpoint(network);
            // care : might not be robust as getCurrentValue get the current setPoint from a network variant
            //        we need to be sure that this variant has been properly set
            flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentSetPoint);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentSetPoint);

            flowConstraint.setCoefficient(setPointVariable, -sensitivity);
        } else {
            // We need to do this in case of an update
            flowConstraint.setCoefficient(setPointVariable, 0);
        }
    }

    private boolean isRangeActionSensitivityAboveThreshold(RangeAction<?> rangeAction, double sensitivity) {
        if (rangeAction instanceof PstRangeAction) {
            return sensitivity >= pstSensitivityThreshold;
        } else if (rangeAction instanceof HvdcRangeAction) {
            return sensitivity >= hvdcSensitivityThreshold;
        } else if (rangeAction instanceof InjectionRangeAction) {
            return sensitivity >= hvdcSensitivityThreshold;
            //todo, create dedicated parameter
        } else {
            throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
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
    private void buildRangeActionConstraints(LinearProblem linearProblem) {
        getRangeActions().forEach(rangeAction -> {
            double prePerimeterSetPoint = prePerimeterRangeActionResult.getOptimizedSetPoint(rangeAction);
            MPConstraint varConstraintNegative = linearProblem.addAbsoluteRangeActionVariationConstraint(
                    -prePerimeterSetPoint,
                    LinearProblem.infinity(),
                    rangeAction,
                    LinearProblem.AbsExtension.NEGATIVE
            );
            MPConstraint varConstraintPositive = linearProblem.addAbsoluteRangeActionVariationConstraint(
                    prePerimeterSetPoint,
                    LinearProblem.infinity(),
                    rangeAction,
                    LinearProblem.AbsExtension.POSITIVE
            );

            MPVariable setPointVariable = linearProblem.getRangeActionSetpointVariable(rangeAction);
            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);

            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
        });
    }
}
