/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers.ProblemFiller;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.LinearProblemStatus;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblemIdGenerator.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class LinearProblem {

    private static final String OPT_PROBLEM_NAME = "RangeActionOptProblem";

    private final OpenRaoMPSolver solver;
    private final List<ProblemFiller> fillerList;
    private final RangeActionActivationResult raActivationFromParentLeaf;
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

    LinearProblem(List<ProblemFiller> fillerList, RangeActionActivationResult raActivationFromParentLeaf, RangeActionsOptimizationParameters.Solver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = new OpenRaoMPSolver(OPT_PROBLEM_NAME, solver);
        this.fillerList = fillerList;
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
        this.solver.setMinimization();
    }

    public List<ProblemFiller> getFillers() {
        return fillerList;
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult, raActivationFromParentLeaf));
    }

    public void updateBetweenSensiIteration(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // TODO: only reset if failed states have changed? Then we need access to all CRAC states in order to query the sensitivity result
        this.solver.resetModel();
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult, rangeActionActivationResult));
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

    public OpenRaoMPVariable addFlowVariable(double lb, double ub, FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, flowVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPVariable getFlowVariable(FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(flowVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPConstraint addFlowConstraint(double lb, double ub, FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec, side, timestamp));
    }

    public OpenRaoMPConstraint getFlowConstraint(FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(flowConstraintId(cnec, side, timestamp));
    }

    public OpenRaoMPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable getRangeActionSetpointVariable(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(rangeActionSetpointVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addRangeActionRelativeSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking, timestamp));
    }

    public OpenRaoMPConstraint addPstRelativeTapConstraint(double lb, double ub, PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, pstRangeActionRelativeTapConstraintId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getPstRelativeTapConstraint(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(pstRangeActionRelativeTapConstraintId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getRangeActionRelativeSetpointConstraint(RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking, timestamp));
    }

    public OpenRaoMPVariable addRangeActionVariationBinary(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeBoolVar(rangeActionBinaryVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable getRangeActionVariationBinary(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(rangeActionBinaryVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable addPstTapVariationVariable(double lb, double ub, PstRangeAction rangeAction, State state, VariationDirectionExtension variation, Optional<OffsetDateTime> timestamp) {
        return solver.makeIntVar(lb, ub, pstTapVariableVariationId(rangeAction, state, variation, timestamp));
    }

    public OpenRaoMPVariable getPstTapVariationVariable(PstRangeAction rangeAction, State state, VariationDirectionExtension variation, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(pstTapVariableVariationId(rangeAction, state, variation, timestamp));
    }

    public OpenRaoMPVariable addPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation, Optional<OffsetDateTime> timestamp) {
        return solver.makeBoolVar(pstTapBinaryVariationInDirectionId(rangeAction, state, variation, timestamp));
    }

    public OpenRaoMPVariable getPstTapVariationBinary(PstRangeAction rangeAction, State state, VariationDirectionExtension variation, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(pstTapBinaryVariationInDirectionId(rangeAction, state, variation, timestamp));
    }

    public OpenRaoMPConstraint addTapToAngleConversionConstraint(double lb, double ub, PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, tapToAngleConversionConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getTapToAngleConversionConstraint(PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(tapToAngleConversionConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(upOrDownPstVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getUpOrDownPstVariationConstraint(PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(upOrDownPstVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addIsVariationConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, isVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getIsVariationConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(isVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addIsVariationInDirectionConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, isVariationInDirectionConstraintId(rangeAction, state, reference, direction, timestamp));
    }

    public OpenRaoMPConstraint getIsVariationInDirectionConstraint(RangeAction<?> rangeAction, State state, VariationReferenceExtension reference, VariationDirectionExtension direction, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(isVariationInDirectionConstraintId(rangeAction, state, reference, direction, timestamp));
    }

    public OpenRaoMPVariable addRangeActionGroupSetpointVariable(double lb, double ub, String rangeActionGroupId, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetpointVariableId(rangeActionGroupId, state, timestamp));
    }

    public OpenRaoMPVariable getRangeActionGroupSetpointVariable(String rangeActionGroupId, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(rangeActionGroupSetpointVariableId(rangeActionGroupId, state, timestamp));
    }

    public OpenRaoMPVariable addPstGroupTapVariable(double lb, double ub, String rangeActionGroupId, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, pstGroupTapVariableId(rangeActionGroupId, state, timestamp));
    }

    public OpenRaoMPVariable getPstGroupTapVariable(String rangeActionGroupId, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(pstGroupTapVariableId(rangeActionGroupId, state, timestamp));
    }

    public OpenRaoMPConstraint addRangeActionGroupSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetpointConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getRangeActionGroupSetpointConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(rangeActionGroupSetpointConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addPstGroupTapConstraint(double lb, double ub, PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, pstGroupTapConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getPstGroupTapConstraint(PstRangeAction rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(pstGroupTapConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable getAbsoluteRangeActionVariationVariable(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(absoluteRangeActionVariationVariableId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addMinimumMarginConstraint(double lb, double ub, FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint getMinimumMarginConstraint(FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(minimumMarginConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint addMinimumRelMarginSignDefinitionConstraint(double lb, double ub, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, minimumRelMarginSignDefinitionConstraintId(timestamp));
    }

    public OpenRaoMPConstraint getMinimumRelMarginSignDefinitionConstraint(Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(minimumRelMarginSignDefinitionConstraintId(timestamp));
    }

    public OpenRaoMPConstraint addMinimumRelMarginSetToZeroConstraint(double lb, double ub, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginSetToZeroConstraintId(timestamp));
    }

    public OpenRaoMPConstraint getMinimumRelMarginSetToZeroConstraint(Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(minimumRelativeMarginSetToZeroConstraintId(timestamp));
    }

    public OpenRaoMPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint getMinimumRelativeMarginConstraint(FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(minimumRelativeMarginConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPVariable addMinimumMarginVariable(double lb, double ub, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, minimumMarginVariableId(timestamp));
    }

    public OpenRaoMPVariable getMinimumMarginVariable(Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(minimumMarginVariableId(timestamp));
    }

    public OpenRaoMPVariable addMinimumRelativeMarginVariable(double lb, double ub, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, minimumRelativeMarginVariableId(timestamp));
    }

    public OpenRaoMPVariable getMinimumRelativeMarginVariable(Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(minimumRelativeMarginVariableId(timestamp));
    }

    public OpenRaoMPVariable addMinimumRelativeMarginSignBinaryVariable(Optional<OffsetDateTime> timestamp) {
        return solver.makeBoolVar(minimumRelativeMarginSignBinaryVariableId(timestamp));
    }

    public OpenRaoMPVariable getMinimumRelativeMarginSignBinaryVariable(Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(minimumRelativeMarginSignBinaryVariableId(timestamp));
    }

    //Begin MaxLoopFlowFiller section
    public OpenRaoMPConstraint addMaxLoopFlowConstraint(double lb, double ub, FlowCnec cnec, TwoSides side, BoundExtension lbOrUb, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, side, lbOrUb, timestamp));
    }

    public OpenRaoMPConstraint getMaxLoopFlowConstraint(FlowCnec cnec, TwoSides side, BoundExtension lbOrUb, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxLoopFlowConstraintId(cnec, side, lbOrUb, timestamp));
    }

    public OpenRaoMPVariable addLoopflowViolationVariable(double lb, double ub, FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPVariable getLoopflowViolationVariable(FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(loopflowViolationVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPVariable addMnecViolationVariable(double lb, double ub, FlowCnec mnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec, side, timestamp));
    }

    public OpenRaoMPVariable getMnecViolationVariable(FlowCnec mnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(mnecViolationVariableId(mnec, side, timestamp));
    }

    public OpenRaoMPConstraint addMnecFlowConstraint(double lb, double ub, FlowCnec mnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint getMnecFlowConstraint(FlowCnec mnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(mnecFlowConstraintId(mnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPVariable addOptimizeCnecBinaryVariable(FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.makeIntVar(0, 1, optimizeCnecBinaryVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPVariable getOptimizeCnecBinaryVariable(FlowCnec cnec, TwoSides side, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(optimizeCnecBinaryVariableId(cnec, side, timestamp));
    }

    public OpenRaoMPConstraint addDontOptimizeCnecConstraint(double lb, double ub, FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint getDontOptimizeCnecConstraint(FlowCnec cnec, TwoSides side, MarginExtension belowOrAboveThreshold, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(dontOptimizeCnecConstraintId(cnec, side, belowOrAboveThreshold, timestamp));
    }

    public OpenRaoMPConstraint addMaxRaConstraint(double lb, double ub, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxRaConstraintId(state, timestamp));
    }

    public OpenRaoMPConstraint getMaxRaConstraint(State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxRaConstraintId(state, timestamp));
    }

    public OpenRaoMPConstraint addMaxTsoConstraint(double lb, double ub, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxTsoConstraintId(state, timestamp));
    }

    public OpenRaoMPConstraint getMaxTsoConstraint(State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxTsoConstraintId(state, timestamp));
    }

    public OpenRaoMPConstraint addMaxRaPerTsoConstraint(double lb, double ub, String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxRaPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPConstraint getMaxRaPerTsoConstraint(String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxRaPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPConstraint addMaxPstPerTsoConstraint(double lb, double ub, String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxPstPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPConstraint getMaxPstPerTsoConstraint(String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxPstPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPVariable addTsoRaUsedVariable(double lb, double ub, String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(lb, ub, tsoRaUsedVariableId(operator, state, timestamp));
    }

    public OpenRaoMPVariable getTsoRaUsedVariable(String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(tsoRaUsedVariableId(operator, state, timestamp));
    }

    public OpenRaoMPConstraint addTsoRaUsedConstraint(double lb, double ub, String operator, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, tsoRaUsedConstraintId(operator, rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getTsoRaUsedConstraint(String operator, RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(tsoRaUsedConstraintId(operator, rangeAction, state, timestamp));
    }

    public OpenRaoMPVariable addPstAbsoluteVariationFromInitialTapVariable(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeIntVar(0, infinity(), pstAbsoluteVariationFromInitialTapVariableId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPVariable getPstAbsoluteVariationFromInitialTapVariable(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(pstAbsoluteVariationFromInitialTapVariableId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addPstAbsoluteVariationFromInitialTapConstraint(double lb, double ub, PstRangeAction pstRangeAction, State state, AbsExtension positiveOrNegative, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, pstAbsoluteVariationFromInitialTapConstraintId(pstRangeAction, state, positiveOrNegative, timestamp));
    }

    public OpenRaoMPConstraint getPstAbsoluteVariationFromInitialTapConstraint(PstRangeAction pstRangeAction, State state, AbsExtension positiveOrNegative, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(pstAbsoluteVariationFromInitialTapConstraintId(pstRangeAction, state, positiveOrNegative, timestamp));
    }

    public OpenRaoMPConstraint addTsoMaxElementaryActionsConstraint(double lb, double ub, String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(lb, ub, maxElementaryActionsPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPConstraint getTsoMaxElementaryActionsConstraint(String operator, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(maxElementaryActionsPerTsoConstraintId(operator, state, timestamp));
    }

    public OpenRaoMPVariable addRangeActionVariationVariable(double ub, RangeAction<?> rangeAction, State state, VariationDirectionExtension variationDirection, Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(0.0, ub, rangeActionVariationVariableId(rangeAction, state, variationDirection, timestamp));
    }

    public OpenRaoMPVariable getRangeActionVariationVariable(RangeAction<?> rangeAction, State state, VariationDirectionExtension variationDirection, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(rangeActionVariationVariableId(rangeAction, state, variationDirection, timestamp));
    }

    public OpenRaoMPConstraint addRangeActionSetPointVariationConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(0.0, 0.0, rangeActionSetPointVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getRangeActionSetPointVariationConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(rangeActionSetPointVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addRangeActionAbsoluteVariationConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(0.0, 0.0, rangeActionAbsoluteVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getRangeActionAbsoluteVariationConstraint(RangeAction<?> rangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(rangeActionAbsoluteVariationConstraintId(rangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addInjectionBalanceConstraint(State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(0.0, 0.0, injectionBalanceConstraintId(state, timestamp));
    }

    public OpenRaoMPConstraint getInjectionBalanceConstraint(State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(injectionBalanceConstraintId(state, timestamp));
    }

    public OpenRaoMPVariable addTotalPstRangeActionTapVariationVariable(PstRangeAction pstRangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection, Optional<OffsetDateTime> timestamp) {
        return solver.makeIntVar(0, infinity(), totalPstRangeActionTapVariationVariableId(pstRangeAction, state, variationDirection, timestamp));
    }

    public OpenRaoMPVariable getTotalPstRangeActionTapVariationVariable(PstRangeAction pstRangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(totalPstRangeActionTapVariationVariableId(pstRangeAction, state, variationDirection, timestamp));
    }

    public OpenRaoMPConstraint addTotalPstRangeActionTapVariationConstraint(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(0, 0, totalPstRangeActionTapVariationConstraintId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPVariable addTapVariable(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        int minTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().min(Integer::compareTo).orElseThrow();
        int maxTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        return solver.makeIntVar(minTap, maxTap, tapVariableId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPVariable getTapVariable(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(tapVariableId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint addTapConstraint(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.makeConstraint(0, 0, tapConstraintId(pstRangeAction, state, timestamp));
    }

    public OpenRaoMPConstraint getTapConstraint(PstRangeAction pstRangeAction, State state, Optional<OffsetDateTime> timestamp) {
        return solver.getConstraint(tapConstraintId(pstRangeAction, state, timestamp));
    }

    public double infinity() {
        return solver.infinity();
    }
}
