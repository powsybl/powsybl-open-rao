/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.powsybl.open_rao.data.crac_api.State;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.range_action.RangeAction;
import com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.open_rao.search_tree_rao.result.api.FlowResult;
import com.powsybl.open_rao.search_tree_rao.result.api.LinearProblemStatus;
import com.powsybl.open_rao.search_tree_rao.result.api.RangeActionActivationResult;
import com.powsybl.open_rao.search_tree_rao.result.api.SensitivityResult;

import java.util.List;

import static com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem.LinearProblemIdGenerator.*;

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

    public enum RaRangeShrinking {
        TRUE("iterative-shrink"),
        FALSE("");

        private final String name;

        RaRangeShrinking(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
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
        solver.setRelativeMipGap(relativeMipGap);
        solver.setSolverSpecificParametersAsString(solverSpecificParameters);
        return solver.solve();
    }

    public FaraoMPObjective getObjective() {
        return solver.getObjective();
    }

    public int numVariables() {
        return solver.numVariables();
    }

    public int numConstraints() {
        return solver.numConstraints();
    }

    public FaraoMPVariable addFlowVariable(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeNumVar(lb, ub, flowVariableId(cnec, side));
    }

    public FaraoMPVariable getFlowVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(flowVariableId(cnec, side));
    }

    public FaraoMPConstraint addFlowConstraint(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec, side));
    }

    public FaraoMPConstraint getFlowConstraint(FlowCnec cnec, Side side) {
        return solver.getConstraint(flowConstraintId(cnec, side));
    }

    public FaraoMPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction, state));
    }

    public FaraoMPVariable getRangeActionSetpointVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionSetpointVariableId(rangeAction, state));
    }

    public FaraoMPConstraint addRangeActionRelativeSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking) {
        return solver.makeConstraint(lb, ub, rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking));
    }

    public FaraoMPConstraint getRangeActionRelativeSetpointConstraint(RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking) {
        return solver.getConstraint(rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking));
    }

    public FaraoMPVariable addRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.makeBoolVar(rangeActionBinaryVariableId(rangeAction, state));
    }

    public FaraoMPVariable getRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionBinaryVariableId(rangeAction, state));
    }

    public FaraoMPVariable addPstTapVariationVariable(double lb, double ub, PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeIntVar(lb, ub, pstTapVariableVariationId(rangeAction, state, variation));
    }

    public FaraoMPVariable getPstTapVariationVariable(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapVariableVariationId(rangeAction, state, variation));
    }

    public FaraoMPVariable addPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeBoolVar(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public FaraoMPVariable getPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public FaraoMPConstraint addTapToAngleConversionConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tapToAngleConversionConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint getTapToAngleConversionConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(tapToAngleConversionConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint addUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint getUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint addIsVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, isVariationConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint getIsVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(isVariationConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint addIsVariationInDirectionConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.makeConstraint(lb, ub, isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public FaraoMPConstraint getIsVariationInDirectionConstraint(RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.getConstraint(isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public FaraoMPVariable addRangeActionGroupSetpointVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public FaraoMPVariable getRangeActionGroupSetpointVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public FaraoMPVariable addPstGroupTapVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public FaraoMPVariable getPstGroupTapVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public FaraoMPConstraint addRangeActionGroupSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint getRangeActionGroupSetpointConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint addPstGroupTapConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, pstGroupTapConstraintId(rangeAction, state));
    }

    public FaraoMPConstraint getPstGroupTapConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(pstGroupTapConstraintId(rangeAction, state));
    }

    public FaraoMPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public FaraoMPVariable getAbsoluteRangeActionVariationVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public FaraoMPConstraint addAbsoluteRangeActionVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public FaraoMPConstraint getAbsoluteRangeActionVariationConstraint(RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.getConstraint(absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public FaraoMPConstraint addMinimumMarginConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint getMinimumMarginConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint addMinimumRelMarginSignDefinitionConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelMarginSignDefinitionConstraintId());
    }

    public FaraoMPConstraint getMinimumRelMarginSignDefinitionConstraint() {
        return solver.getConstraint(minimumRelMarginSignDefinitionConstraintId());
    }

    public FaraoMPConstraint addMinimumRelMarginSetToZeroConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginSetToZeroConstraintId());
    }

    public FaraoMPConstraint getMinimumRelMarginSetToZeroConstraint() {
        return solver.getConstraint(minimumRelativeMarginSetToZeroConstraintId());
    }

    public FaraoMPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint getMinimumRelativeMarginConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPVariable addMinimumMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumMarginVariableId());
    }

    public FaraoMPVariable getMinimumMarginVariable() {
        return solver.getVariable(minimumMarginVariableId());
    }

    public FaraoMPVariable addMinimumRelativeMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumRelativeMarginVariableId());
    }

    public FaraoMPVariable getMinimumRelativeMarginVariable() {
        return solver.getVariable(minimumRelativeMarginVariableId());
    }

    public FaraoMPVariable addMinimumRelativeMarginSignBinaryVariable() {
        return solver.makeBoolVar(minimumRelativeMarginSignBinaryVariableId());
    }

    public FaraoMPVariable getMinimumRelativeMarginSignBinaryVariable() {
        return solver.getVariable(minimumRelativeMarginSignBinaryVariableId());
    }

    //Begin MaxLoopFlowFiller section
    public FaraoMPConstraint addMaxLoopFlowConstraint(double lb, double ub, FlowCnec cnec, Side side, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, side, lbOrUb));
    }

    public FaraoMPConstraint getMaxLoopFlowConstraint(FlowCnec cnec, Side side, BoundExtension lbOrUb) {
        return solver.getConstraint(maxLoopFlowConstraintId(cnec, side, lbOrUb));
    }

    public FaraoMPVariable addLoopflowViolationVariable(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec, side));
    }

    public FaraoMPVariable getLoopflowViolationVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(loopflowViolationVariableId(cnec, side));
    }

    public FaraoMPVariable addMnecViolationVariable(double lb, double ub, FlowCnec mnec, Side side) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec, side));
    }

    public FaraoMPVariable getMnecViolationVariable(FlowCnec mnec, Side side) {
        return solver.getVariable(mnecViolationVariableId(mnec, side));
    }

    public FaraoMPConstraint addMnecFlowConstraint(double lb, double ub, FlowCnec mnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint getMnecFlowConstraint(FlowCnec mnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(mnecFlowConstraintId(mnec, side, belowOrAboveThreshold));
    }

    public FaraoMPVariable addOptimizeCnecBinaryVariable(FlowCnec cnec, Side side) {
        return solver.makeIntVar(0, 1, optimizeCnecBinaryVariableId(cnec, side));
    }

    public FaraoMPVariable getOptimizeCnecBinaryVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(optimizeCnecBinaryVariableId(cnec, side));
    }

    public FaraoMPConstraint addDontOptimizeCnecConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint getDontOptimizeCnecConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public FaraoMPConstraint addMaxRaConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxRaConstraintId(state));
    }

    public FaraoMPConstraint getMaxRaConstraint(State state) {
        return solver.getConstraint(maxRaConstraintId(state));
    }

    public FaraoMPConstraint addMaxTsoConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxTsoConstraintId(state));
    }

    public FaraoMPConstraint getMaxTsoConstraint(State state) {
        return solver.getConstraint(maxTsoConstraintId(state));
    }

    public FaraoMPConstraint addMaxRaPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxRaPerTsoConstraintId(operator, state));
    }

    public FaraoMPConstraint getMaxRaPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxRaPerTsoConstraintId(operator, state));
    }

    public FaraoMPConstraint addMaxPstPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxPstPerTsoConstraintId(operator, state));
    }

    public FaraoMPConstraint getMaxPstPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxPstPerTsoConstraintId(operator, state));
    }

    public FaraoMPVariable addTsoRaUsedVariable(double lb, double ub, String operator, State state) {
        return solver.makeNumVar(lb, ub, tsoRaUsedVariableId(operator, state));
    }

    public FaraoMPVariable getTsoRaUsedVariable(String operator, State state) {
        return solver.getVariable(tsoRaUsedVariableId(operator, state));
    }

    public FaraoMPConstraint addTsoRaUsedConstraint(double lb, double ub, String operator, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public FaraoMPConstraint getTsoRaUsedConstraint(String operator, RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public static double infinity() {
        return 1e10;
    }

}
