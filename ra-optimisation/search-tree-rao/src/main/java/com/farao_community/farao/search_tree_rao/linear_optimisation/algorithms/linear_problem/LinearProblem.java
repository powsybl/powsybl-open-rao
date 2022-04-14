/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.fillers.ProblemFiller;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.google.ortools.linearsolver.*;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

import static com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemIdGenerator.*;
import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class LinearProblem {

    private final FaraoMPSolver solver;
    private final List<ProblemFiller> fillerList;
    private final double relativeMipGap;
    private final String solverSpecificParameters;

    public enum AbsExtension {
        POSITIVE,
        NEGATIVE
    }

    public enum VariationDirectionExtension {
        UPWARD,
        DOWNWARD
    }

    public enum VariationReferenceExtension {
        PREPERIMETER,
        PREVIOUS_ITERATION
    }

    public enum MarginExtension {
        BELOW_THRESHOLD,
        ABOVE_THRESHOLD
    }

    public enum BoundExtension {
        LOWER_BOUND,
        UPPER_BOUND
    }

    public static LinearProblemBuilder create() {
        return new LinearProblemBuilder();
    }

    LinearProblem(List<ProblemFiller> fillerList, FaraoMPSolver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = solver;
        this.fillerList = fillerList;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
        this.solver.objective().setMinimization();
    }

    public List<ProblemFiller> getFillers() {
        return fillerList;
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult));
    }

    public void updateBetweenSensiIteration(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        fillerList.forEach(problemFiller -> problemFiller.updateBetweenSensiIteration(this, flowResult, sensitivityResult, rangeActionActivationResult));
    }

    public void updateBetweenMipIteration(RangeActionActivationResult rangeActionActivationResult) {
        fillerList.forEach(problemFiller -> problemFiller.updateBetweenMipIteration(this, rangeActionActivationResult));
    }

    public LinearProblemStatus solve() {
        MPSolverParameters solveConfiguration = new MPSolverParameters();
        solveConfiguration.setDoubleParam(MPSolverParameters.DoubleParam.RELATIVE_MIP_GAP, relativeMipGap);
        if (solverSpecificParameters != null) {
            solver.setSolverSpecificParametersAsString(solverSpecificParameters);
        }
        return convertResultStatus(solver.solve(solveConfiguration));
    }

    private static LinearProblemStatus convertResultStatus(MPSolver.ResultStatus status) {
        switch (status) {
            case OPTIMAL:
                return LinearProblemStatus.OPTIMAL;
            case ABNORMAL:
                return LinearProblemStatus.ABNORMAL;
            case FEASIBLE:
                return LinearProblemStatus.FEASIBLE;
            case UNBOUNDED:
                return LinearProblemStatus.UNBOUNDED;
            case INFEASIBLE:
                return LinearProblemStatus.INFEASIBLE;
            case NOT_SOLVED:
                return LinearProblemStatus.NOT_SOLVED;
            default:
                throw new NotImplementedException(format("Status %s not handled.", status));
        }
    }

    public MPObjective getObjective() {
        return solver.getObjective();
    }

    public int numVariables() {
        return solver.numVariables();
    }

    public int numConstraints() {
        return solver.numConstraints();
    }

    public MPVariable addFlowVariable(double lb, double ub, FlowCnec cnec) {
        return solver.makeNumVar(lb, ub, flowVariableId(cnec));
    }

    public MPVariable getFlowVariable(FlowCnec cnec) {
        return solver.getVariable(flowVariableId(cnec));
    }

    public MPConstraint addFlowConstraint(double lb, double ub, FlowCnec cnec) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec));
    }

    public MPConstraint getFlowConstraint(FlowCnec cnec) {
        return solver.getConstraint(flowConstraintId(cnec));
    }

    public MPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction, state));
    }

    public MPVariable getRangeActionSetpointVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionSetpointVariableId(rangeAction, state));
    }

    public MPConstraint addRangeActionRelativeSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, rangeActionRelativeSetpointConstraintId(rangeAction, state));
    }

    public MPVariable addRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.makeBoolVar(rangeActionBinaryVariableId(rangeAction, state));
    }

    public MPVariable getRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionBinaryVariableId(rangeAction, state));
    }

    public MPVariable addPstTapVariationVariable(double lb, double ub, PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeIntVar(lb, ub, pstTapVariableVariationId(rangeAction, state, variation));
    }

    public MPVariable getPstTapVariationVariable(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapVariableVariationId(rangeAction, state, variation));
    }

    public MPVariable addPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeBoolVar(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public MPVariable getPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public MPConstraint addTapToAngleConversionConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tapToAngleConversionConstraintId(rangeAction, state));
    }

    public MPConstraint getTapToAngleConversionConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(tapToAngleConversionConstraintId(rangeAction, state));
    }

    public MPConstraint addUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public MPConstraint getUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public MPConstraint addIsVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, isVariationConstraintId(rangeAction, state));
    }

    public MPConstraint getIsVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(isVariationConstraintId(rangeAction, state));
    }

    public MPConstraint addIsVariationInDirectionConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.makeConstraint(lb, ub, isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public MPConstraint getIsVariationInDirectionConstraint(RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.getConstraint(isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public MPVariable addRangeActionGroupSetpointVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public MPVariable getRangeActionGroupSetpointVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public MPVariable addPstGroupTapVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public MPVariable getPstGroupTapVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public MPConstraint addRangeActionGroupSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public MPConstraint getRangeActionGroupSetpointConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public MPConstraint addPstGroupTapConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, pstGroupTapConstraintId(rangeAction, state));
    }

    public MPConstraint getPstGroupTapConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(pstGroupTapConstraintId(rangeAction, state));
    }

    public MPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public MPVariable getAbsoluteRangeActionVariationVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public MPConstraint addAbsoluteRangeActionVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public MPConstraint getAbsoluteRangeActionVariationConstraint(RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.getConstraint(absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public MPConstraint addMinimumMarginConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumMarginConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint addMinimumRelMarginSignDefinitionConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelMarginSignDefinitionConstraintId());
    }

    public MPConstraint getMinimumRelMarginSignDefinitionConstraint() {
        return solver.getConstraint(minimumRelMarginSignDefinitionConstraintId());
    }

    public MPConstraint addMinimumRelMarginSetToZeroConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginSetToZeroConstraintId());
    }

    public MPConstraint getMinimumRelMarginSetToZeroConstraint() {
        return solver.getConstraint(minimumRelativeMarginSetToZeroConstraintId());
    }

    public MPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumRelativeMarginConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPVariable addMinimumMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumMarginVariableId());
    }

    public MPVariable getMinimumMarginVariable() {
        return solver.getVariable(minimumMarginVariableId());
    }

    public MPVariable addMinimumRelativeMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumRelativeMarginVariableId());
    }

    public MPVariable getMinimumRelativeMarginVariable() {
        return solver.getVariable(minimumRelativeMarginVariableId());
    }

    public MPVariable addMinimumRelativeMarginSignBinaryVariable() {
        return solver.makeBoolVar(minimumRelativeMarginSignBinaryVariableId());
    }

    public MPVariable getMinimumRelativeMarginSignBinaryVariable() {
        return solver.getVariable(minimumRelativeMarginSignBinaryVariableId());
    }

    //Begin MaxLoopFlowFiller section
    public MPConstraint addMaxLoopFlowConstraint(double lb, double ub, FlowCnec cnec, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPConstraint getMaxLoopFlowConstraint(FlowCnec cnec, BoundExtension lbOrUb) {
        return solver.getConstraint(maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPVariable addLoopflowViolationVariable(double lb, double ub, FlowCnec cnec) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec));
    }

    public MPVariable getLoopflowViolationVariable(FlowCnec cnec) {
        return solver.getVariable(loopflowViolationVariableId(cnec));
    }

    public MPVariable addMnecViolationVariable(double lb, double ub, FlowCnec mnec) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec));
    }

    public MPVariable getMnecViolationVariable(FlowCnec mnec) {
        return solver.getVariable(mnecViolationVariableId(mnec));
    }

    public MPConstraint addMnecFlowConstraint(double lb, double ub, FlowCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPConstraint getMnecFlowConstraint(FlowCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPVariable addMarginDecreaseBinaryVariable(FlowCnec cnec) {
        return solver.makeIntVar(0, 1, marginDecreaseVariableId(cnec));
    }

    public MPVariable getMarginDecreaseBinaryVariable(FlowCnec cnec) {
        return solver.getVariable(marginDecreaseVariableId(cnec));
    }

    public MPConstraint addMarginDecreaseConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMarginDecreaseConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint addMaxRaConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxRaConstraintId(state));
    }

    public MPConstraint getMaxRaConstraint(State state) {
        return solver.getConstraint(maxRaConstraintId(state));
    }

    public MPConstraint addMaxTsoConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxTsoConstraintId(state));
    }

    public MPConstraint getMaxTsoConstraint(State state) {
        return solver.getConstraint(maxTsoConstraintId(state));
    }

    public MPConstraint addMaxRaPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxRaPerTsoConstraintId(operator, state));
    }

    public MPConstraint getMaxRaPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxRaPerTsoConstraintId(operator, state));
    }

    public MPConstraint addMaxPstPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxPstPerTsoConstraintId(operator, state));
    }

    public MPConstraint getMaxPstPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxPstPerTsoConstraintId(operator, state));
    }

    public MPVariable addTsoRaUsedVariable(double lb, double ub, String operator, State state) {
        return solver.makeNumVar(lb, ub, tsoRaUsedVariableId(operator, state));
    }

    public MPVariable getTsoRaUsedVariable(String operator, State state) {
        return solver.getVariable(tsoRaUsedVariableId(operator, state));
    }

    public MPConstraint addTsoRaUsedConstraint(double lb, double ub, String operator, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public MPConstraint getTsoRaUsedConstraint(String operator, RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public static double infinity() {
        return MPSolver.infinity();
    }

}
