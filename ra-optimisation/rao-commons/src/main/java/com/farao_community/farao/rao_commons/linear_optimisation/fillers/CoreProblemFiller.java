/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.fillers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.results.FlowResult;
import com.farao_community.farao.rao_api.results.RangeActionResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearProblem;
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
    private final Set<RangeAction> rangeActions;
    private final RangeActionResult prePerimeterRangeActionResult;
    private final double pstSensitivityThreshold;

    public CoreProblemFiller(Network network,
                             Set<FlowCnec> flowCnecs,
                             Set<RangeAction> rangeActions,
                             RangeActionResult prePerimeterRangeActionResult,
                             double pstSensitivityThreshold) {
        this.network = network;
        this.flowCnecs = flowCnecs;
        this.rangeActions = rangeActions;
        this.prePerimeterRangeActionResult = prePerimeterRangeActionResult;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }

    private Set<RangeAction> getRangeActions() {
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
            buildRangeActionGroupConstraint(linearProblem, rangeAction);
        });

        // add constraints
        buildFlowConstraints(linearProblem, flowResult, sensitivityResult);
        buildRangeActionConstraints(linearProblem);
    }

    @Override
    public void update(LinearProblem linearProblem, FlowResult flowResult, SensitivityResult sensitivityResult) {
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
     * </ul>
     *
     * This set point of the a RangeAction is bounded between the min/max variations
     * of the RangeAction :
     *
     * initialSetPoint[r] - maxNegativeVariation[r] <= S[r]
     * S[r] >= initialSetPoint[r] + maxPositiveVariation[r]
     */
    private void buildRangeActionSetPointVariables(LinearProblem linearProblem, RangeAction rangeAction, double prePerimeterValue) {
        double minSetPoint = rangeAction.getMinAdmissibleSetpoint(prePerimeterValue);
        double maxSetPoint = rangeAction.getMaxAdmissibleSetpoint(prePerimeterValue);
        linearProblem.addRangeActionSetPointVariable(minSetPoint, maxSetPoint, rangeAction);
    }

    /**
     * Build one absolute variable variable AV[r] for each RangeAction r
     * This variable describes the absolute difference between the range action set point
     * and its initial value. It is given :
     * <ul>
     *     <li>in DEGREE for PST range actions</li>
     * </ul>
     */
    private void buildRangeActionAbsoluteVariationVariables(LinearProblem linearProblem, RangeAction rangeAction) {
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
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec);
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
            addImpactOfRangeActionOnCnec(linearProblem, sensitivityResult, cnec);
        });
    }

    private void addImpactOfRangeActionOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, FlowCnec cnec) {
        MPVariable flowVariable = linearProblem.getFlowVariable(cnec);
        MPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec);

        if (flowVariable == null || flowConstraint == null) {
            throw new FaraoException(format("Flow variable and/or constraint on %s has not been defined yet.", cnec.getId()));
        }

        getRangeActions().forEach(rangeAction -> {
            if (rangeAction instanceof PstRangeAction) {
                addImpactOfPstOnCnec(linearProblem, sensitivityResult, rangeAction, cnec, flowConstraint);
            } else {
                throw new FaraoException("Type of RangeAction not yet handled by the LinearRao.");
            }
        });
    }

    private void addImpactOfPstOnCnec(LinearProblem linearProblem, SensitivityResult sensitivityResult, RangeAction rangeAction, FlowCnec cnec, MPConstraint flowConstraint) {
        MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
        if (setPointVariable == null) {
            throw new FaraoException(format("Range action variable for %s has not been defined yet.", rangeAction.getId()));
        }

        double sensitivity = sensitivityResult.getSensitivityValue(cnec, rangeAction, Unit.MEGAWATT);

        if (Math.abs(sensitivity) >= pstSensitivityThreshold) {
            double currentSetPoint = rangeAction.getCurrentSetpoint(network);
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

            MPVariable setPointVariable = linearProblem.getRangeActionSetPointVariable(rangeAction);
            MPVariable absoluteVariationVariable = linearProblem.getAbsoluteRangeActionVariationVariable(rangeAction);

            varConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
            varConstraintNegative.setCoefficient(setPointVariable, -1);

            varConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
            varConstraintPositive.setCoefficient(setPointVariable, 1);
        });
    }

    private void buildRangeActionGroupConstraint(LinearProblem linearProblem, RangeAction rangeAction) {
        Optional<String> optGroupId = rangeAction.getGroupId();
        if (optGroupId.isPresent()) {
            String groupId = optGroupId.get();
            // For the first time the group ID is encountered a common variable for set point has to be created
            if (linearProblem.getRangeActionGroupSetPointVariable(groupId) == null) {
                linearProblem.addRangeActionGroupSetPointVariable(-LinearProblem.infinity(), LinearProblem.infinity(), groupId);
            }
            addRangeActionGroupConstraint(linearProblem, rangeAction, groupId);
        }
    }

    private void addRangeActionGroupConstraint(LinearProblem linearProblem, RangeAction rangeAction, String groupId) {
        MPConstraint groupSetPointConstraint = linearProblem.addRangeActionGroupSetPointConstraint(0, 0, rangeAction);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionSetPointVariable(rangeAction), 1);
        groupSetPointConstraint.setCoefficient(linearProblem.getRangeActionGroupSetPointVariable(groupId), -1);
    }
}
