/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.List;

import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemIdGenerator.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class LinearProblem {

    public static final double LP_INFINITY = 1e10;
    private static final String OPT_PROBLEM_NAME = "RangeActionOptProblem";

    private final OpenRaoMPSolver solver;
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

    public static LinearProblemBuilderMultiTS createMultiTS() {
        return new LinearProblemBuilderMultiTS();
    }

    LinearProblem(List<ProblemFiller> fillerList, RangeActionsOptimizationParameters.Solver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = new OpenRaoMPSolver(OPT_PROBLEM_NAME, solver);
        this.fillerList = fillerList;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
        this.solver.setMinimization();
    }

    public List<ProblemFiller> getFillers() {
        return fillerList;
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult));
    }

    public void updateBetweenSensiIteration(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // TODO: only reset if failed states have changed? Then we need access to all CRAC states in order to query the sensitivity result
        this.solver.resetModel();
        fill(flowResult, sensitivityResult);
        // TODO: remove "update" when "rangeActionActivationResult" can be used by "fill"
        // (used in discrete PST fillers & for RA range shrinking in CoreProblemFiller)
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

    public OpenRaoMPObjective getObjective() {
        return solver.getObjective();
    }

    public boolean minimization() {
        return solver.isMinimization();
    }

    public int numVariables() {
        return solver.numVariables();
    }

    public int numConstraints() {
        return solver.numConstraints();
    }

    public OpenRaoMPVariable addFlowVariable(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeNumVar(lb, ub, flowVariableId(cnec, side));
    }

    public OpenRaoMPVariable getFlowVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(flowVariableId(cnec, side));
    }

    public OpenRaoMPConstraint addFlowConstraint(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec, side));
    }

    public OpenRaoMPConstraint getFlowConstraint(FlowCnec cnec, Side side) {
        return solver.getConstraint(flowConstraintId(cnec, side));
    }

    public OpenRaoMPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable getRangeActionSetpointVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionSetpointVariableId(rangeAction, state));
    }

    public OpenRaoMPConstraint addRangeActionRelativeSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking) {
        return solver.makeConstraint(lb, ub, rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking));
    }

    public OpenRaoMPConstraint getRangeActionRelativeSetpointConstraint(RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking) {
        return solver.getConstraint(rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking));
    }

    public OpenRaoMPVariable addRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.makeBoolVar(rangeActionBinaryVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable getRangeActionVariationBinary(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionBinaryVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable addPstTapVariationVariable(double lb, double ub, PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeIntVar(lb, ub, pstTapVariableVariationId(rangeAction, state, variation));
    }

    public OpenRaoMPVariable getPstTapVariationVariable(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapVariableVariationId(rangeAction, state, variation));
    }

    public OpenRaoMPVariable addPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.makeBoolVar(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public OpenRaoMPVariable getPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation) {
        return solver.getVariable(pstTapBinaryVariationInDirectionId(rangeAction, state, variation));
    }

    public OpenRaoMPConstraint addTapToAngleConversionConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tapToAngleConversionConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getTapToAngleConversionConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(tapToAngleConversionConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(upOrDownPstVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addIsVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, isVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getIsVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(isVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addIsVariationInDirectionConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.makeConstraint(lb, ub, isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public OpenRaoMPConstraint getIsVariationInDirectionConstraint(RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction) {
        return solver.getConstraint(isVariationInDirectionConstraintId(rangeAction, state, reference, direction));
    }

    public OpenRaoMPVariable addRangeActionGroupSetpointVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public OpenRaoMPVariable getRangeActionGroupSetpointVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(rangeActionGroupSetpointVariableId(rangeActionGroupId, state));
    }

    public OpenRaoMPVariable addPstGroupTapVariable(double lb, double ub, String rangeActionGroupId, State state) {
        return solver.makeNumVar(lb, ub, pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public OpenRaoMPVariable getPstGroupTapVariable(String rangeActionGroupId, State state) {
        return solver.getVariable(pstGroupTapVariableId(rangeActionGroupId, state));
    }

    public OpenRaoMPConstraint addRangeActionGroupSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getRangeActionGroupSetpointConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(rangeActionGroupSetpointConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addPstGroupTapConstraint(double lb, double ub, PstRangeAction rangeAction, State state) {
        return solver.makeConstraint(lb, ub, pstGroupTapConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getPstGroupTapConstraint(PstRangeAction rangeAction, State state) {
        return solver.getConstraint(pstGroupTapConstraintId(rangeAction, state));
    }

    public OpenRaoMPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable getAbsoluteRangeActionVariationVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(absoluteRangeActionVariationVariableId(rangeAction, state));
    }

    public OpenRaoMPConstraint addAbsoluteRangeActionVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public OpenRaoMPConstraint getAbsoluteRangeActionVariationConstraint(RangeAction<?> rangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.getConstraint(absoluteRangeActionVariationConstraintId(rangeAction, state, positiveOrNegative));
    }

    public OpenRaoMPVariable addSignedRangeActionVariationVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, signedRangeActionVariationVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable getSignedRangeActionVariationVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(signedRangeActionVariationVariableId(rangeAction, state));
    }

    public OpenRaoMPConstraint addSignedRangeActionVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, signedRangeActionVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getSignedRangeActionVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(signedRangeActionVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addInjectionBalanceVariationConstraint(double lb, double ub, State state, int timeStepIndex) {
        String constraintName = injectionBalanceVariationConstraintId(state, timeStepIndex);
        return solver.makeConstraint(lb, ub, constraintName);
    }

    public OpenRaoMPConstraint getInjectionBalanceVariationConstraint(State state, int timeStepIndex) {
        return solver.getConstraint(injectionBalanceVariationConstraintId(state, timeStepIndex));
    }

    public OpenRaoMPConstraint addMinimumMarginConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint getMinimumMarginConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint addMinimumRelMarginSignDefinitionConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelMarginSignDefinitionConstraintId());
    }

    public OpenRaoMPConstraint getMinimumRelMarginSignDefinitionConstraint() {
        return solver.getConstraint(minimumRelMarginSignDefinitionConstraintId());
    }

    public OpenRaoMPConstraint addMinimumRelMarginSetToZeroConstraint(double lb, double ub) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginSetToZeroConstraintId());
    }

    public OpenRaoMPConstraint getMinimumRelMarginSetToZeroConstraint() {
        return solver.getConstraint(minimumRelativeMarginSetToZeroConstraintId());
    }

    public OpenRaoMPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint getMinimumRelativeMarginConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPVariable addMinimumMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumMarginVariableId());
    }

    public OpenRaoMPVariable getMinimumMarginVariable() {
        return solver.getVariable(minimumMarginVariableId());
    }

    public OpenRaoMPVariable addMinimumRelativeMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumRelativeMarginVariableId());
    }

    public OpenRaoMPVariable getMinimumRelativeMarginVariable() {
        return solver.getVariable(minimumRelativeMarginVariableId());
    }

    public OpenRaoMPVariable addMinimumRelativeMarginSignBinaryVariable() {
        return solver.makeBoolVar(minimumRelativeMarginSignBinaryVariableId());
    }

    public OpenRaoMPVariable getMinimumRelativeMarginSignBinaryVariable() {
        return solver.getVariable(minimumRelativeMarginSignBinaryVariableId());
    }

    //Begin MaxLoopFlowFiller section
    public OpenRaoMPConstraint addMaxLoopFlowConstraint(double lb, double ub, FlowCnec cnec, Side side, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, side, lbOrUb));
    }

    public OpenRaoMPConstraint getMaxLoopFlowConstraint(FlowCnec cnec, Side side, BoundExtension lbOrUb) {
        return solver.getConstraint(maxLoopFlowConstraintId(cnec, side, lbOrUb));
    }

    public OpenRaoMPVariable addLoopflowViolationVariable(double lb, double ub, FlowCnec cnec, Side side) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec, side));
    }

    public OpenRaoMPVariable getLoopflowViolationVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(loopflowViolationVariableId(cnec, side));
    }

    public OpenRaoMPVariable addMnecViolationVariable(double lb, double ub, FlowCnec mnec, Side side) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec, side));
    }

    public OpenRaoMPVariable getMnecViolationVariable(FlowCnec mnec, Side side) {
        return solver.getVariable(mnecViolationVariableId(mnec, side));
    }

    public OpenRaoMPConstraint addMnecFlowConstraint(double lb, double ub, FlowCnec mnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint getMnecFlowConstraint(FlowCnec mnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(mnecFlowConstraintId(mnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPVariable addOptimizeCnecBinaryVariable(FlowCnec cnec, Side side) {
        return solver.makeIntVar(0, 1, optimizeCnecBinaryVariableId(cnec, side));
    }

    public OpenRaoMPVariable getOptimizeCnecBinaryVariable(FlowCnec cnec, Side side) {
        return solver.getVariable(optimizeCnecBinaryVariableId(cnec, side));
    }

    public OpenRaoMPConstraint addDontOptimizeCnecConstraint(double lb, double ub, FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint getDontOptimizeCnecConstraint(FlowCnec cnec, Side side, MarginExtension belowOrAboveThreshold) {
        return solver.getConstraint(dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold));
    }

    public OpenRaoMPConstraint addMaxRaConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxRaConstraintId(state));
    }

    public OpenRaoMPConstraint getMaxRaConstraint(State state) {
        return solver.getConstraint(maxRaConstraintId(state));
    }

    public OpenRaoMPConstraint addMaxTsoConstraint(double lb, double ub, State state) {
        return solver.makeConstraint(lb, ub, maxTsoConstraintId(state));
    }

    public OpenRaoMPConstraint getMaxTsoConstraint(State state) {
        return solver.getConstraint(maxTsoConstraintId(state));
    }

    public OpenRaoMPConstraint addMaxRaPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxRaPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPConstraint getMaxRaPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxRaPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPConstraint addMaxPstPerTsoConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxPstPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPConstraint getMaxPstPerTsoConstraint(String operator, State state) {
        return solver.getConstraint(maxPstPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPVariable addTsoRaUsedVariable(double lb, double ub, String operator, State state) {
        return solver.makeNumVar(lb, ub, tsoRaUsedVariableId(operator, state));
    }

    public OpenRaoMPVariable getTsoRaUsedVariable(String operator, State state) {
        return solver.getVariable(tsoRaUsedVariableId(operator, state));
    }

    public OpenRaoMPConstraint addTsoRaUsedConstraint(double lb, double ub, String operator, RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(lb, ub, tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public OpenRaoMPConstraint getTsoRaUsedConstraint(String operator, RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(tsoRaUsedConstraintId(operator, rangeAction, state));
    }

    public static double infinity() {
        return LP_INFINITY;
    }

}
