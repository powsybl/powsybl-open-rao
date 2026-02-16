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
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
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
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
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

    public enum GeneratorState {
        ON, OFF
    }

    public static LinearProblemBuilder create() {
        return new LinearProblemBuilder();
    }

    LinearProblem(List<ProblemFiller> fillerList, RangeActionActivationResult raActivationFromParentLeaf, SearchTreeRaoRangeActionsOptimizationParameters.Solver solver, double relativeMipGap, String solverSpecificParameters) {
        this.solver = new OpenRaoMPSolver(OPT_PROBLEM_NAME, solver);
        this.fillerList = fillerList;
        this.raActivationFromParentLeaf = raActivationFromParentLeaf;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
        this.solver.setMinimization();
    }

    public void reset() {
        solver.resetModel();
    }

    public List<ProblemFiller> getFillers() {
        return fillerList;
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult, raActivationFromParentLeaf));
    }

    public void updateBetweenSensiIteration(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {
        // TODO: only reset if failed states have changed? Then we need access to all CRAC states in order to query the sensitivity result
        reset();
        fillerList.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult, rangeActionActivationResult));
    }

    public void updateBetweenMipIteration(RangeActionActivationResult rangeActionActivationResult) {
        fillerList.forEach(problemFiller -> problemFiller.updateBetweenMipIteration(this, rangeActionActivationResult));
    }

    public LinearProblemStatus solve() {
        solver.setRelativeMipGap(relativeMipGap);
        solver.setSolverSpecificParametersAsString(solverSpecificParameters);
        LinearProblemStatus status = solver.solve();
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

    public OpenRaoMPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction<?> rangeAction, State state) {
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction, state));
    }

    public OpenRaoMPVariable getRangeActionSetpointVariable(RangeAction<?> rangeAction, State state) {
        return solver.getVariable(rangeActionSetpointVariableId(rangeAction, state));
    }

    public OpenRaoMPConstraint addRangeActionRelativeSetpointConstraint(double lb, double ub, RangeAction<?> rangeAction, State state, RaRangeShrinking raRangeShrinking) {
        return solver.makeConstraint(lb, ub, rangeActionRelativeSetpointConstraintId(rangeAction, state, raRangeShrinking));
    }

    public OpenRaoMPConstraint addPstRelativeTapConstraint(double lb, double ub, PstRangeAction pstRangeAction, State state) {
        return solver.makeConstraint(lb, ub, pstRangeActionRelativeTapConstraintId(pstRangeAction, state));
    }

    public OpenRaoMPConstraint getPstRelativeTapConstraint(PstRangeAction pstRangeAction, State state) {
        return solver.getConstraint(pstRangeActionRelativeTapConstraintId(pstRangeAction, state));
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

    public OpenRaoMPVariable addPstAbsoluteVariationFromInitialTapVariable(PstRangeAction pstRangeAction, State state) {
        return solver.makeIntVar(0, infinity(), pstAbsoluteVariationFromInitialTapVariableId(pstRangeAction, state));
    }

    public OpenRaoMPVariable getPstAbsoluteVariationFromInitialTapVariable(PstRangeAction pstRangeAction, State state) {
        return solver.getVariable(pstAbsoluteVariationFromInitialTapVariableId(pstRangeAction, state));
    }

    public OpenRaoMPConstraint addPstAbsoluteVariationFromInitialTapConstraint(double lb, double ub, PstRangeAction pstRangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, pstAbsoluteVariationFromInitialTapConstraintId(pstRangeAction, state, positiveOrNegative));
    }

    public OpenRaoMPConstraint getPstAbsoluteVariationFromInitialTapConstraint(PstRangeAction pstRangeAction, State state, AbsExtension positiveOrNegative) {
        return solver.getConstraint(pstAbsoluteVariationFromInitialTapConstraintId(pstRangeAction, state, positiveOrNegative));
    }

    public OpenRaoMPConstraint addTsoMaxElementaryActionsConstraint(double lb, double ub, String operator, State state) {
        return solver.makeConstraint(lb, ub, maxElementaryActionsPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPConstraint getTsoMaxElementaryActionsConstraint(String operator, State state) {
        return solver.getConstraint(maxElementaryActionsPerTsoConstraintId(operator, state));
    }

    public OpenRaoMPVariable addRangeActionVariationVariable(double ub, RangeAction<?> rangeAction, State state, VariationDirectionExtension variationDirection) {
        return solver.makeNumVar(0.0, ub, rangeActionVariationVariableId(rangeAction, state, variationDirection));
    }

    public OpenRaoMPVariable getRangeActionVariationVariable(RangeAction<?> rangeAction, State state, VariationDirectionExtension variationDirection) {
        return solver.getVariable(rangeActionVariationVariableId(rangeAction, state, variationDirection));
    }

    public OpenRaoMPConstraint addRangeActionSetPointVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.makeConstraint(0.0, 0.0, rangeActionSetPointVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint getRangeActionSetPointVariationConstraint(RangeAction<?> rangeAction, State state) {
        return solver.getConstraint(rangeActionSetPointVariationConstraintId(rangeAction, state));
    }

    public OpenRaoMPConstraint addInjectionBalanceConstraint(State state) {
        return solver.makeConstraint(0.0, 0.0, injectionBalanceConstraintId(state));
    }

    public OpenRaoMPConstraint getInjectionBalanceConstraint(State state) {
        return solver.getConstraint(injectionBalanceConstraintId(state));
    }

    public OpenRaoMPVariable addTotalPstRangeActionTapVariationVariable(PstRangeAction pstRangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection) {
        return solver.makeIntVar(0, infinity(), totalPstRangeActionTapVariationVariableId(pstRangeAction, state, variationDirection));
    }

    public OpenRaoMPVariable getTotalPstRangeActionTapVariationVariable(PstRangeAction pstRangeAction, State state, LinearProblem.VariationDirectionExtension variationDirection) {
        return solver.getVariable(totalPstRangeActionTapVariationVariableId(pstRangeAction, state, variationDirection));
    }

    public OpenRaoMPConstraint addTotalPstRangeActionTapVariationConstraint(PstRangeAction pstRangeAction, State state) {
        return solver.makeConstraint(0, 0, totalPstRangeActionTapVariationConstraintId(pstRangeAction, state));
    }

    public OpenRaoMPVariable addTapVariable(PstRangeAction pstRangeAction, State state) {
        int minTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().min(Integer::compareTo).orElseThrow();
        int maxTap = pstRangeAction.getTapToAngleConversionMap().keySet().stream().max(Integer::compareTo).orElseThrow();
        return solver.makeIntVar(minTap, maxTap, tapVariableId(pstRangeAction, state));
    }

    public OpenRaoMPVariable getTapVariable(PstRangeAction pstRangeAction, State state) {
        return solver.getVariable(tapVariableId(pstRangeAction, state));
    }

    public OpenRaoMPConstraint addTapConstraint(PstRangeAction pstRangeAction, State state) {
        return solver.makeConstraint(0, 0, tapConstraintId(pstRangeAction, state));
    }

    public OpenRaoMPConstraint getTapConstraint(PstRangeAction pstRangeAction, State state) {
        return solver.getConstraint(tapConstraintId(pstRangeAction, state));
    }

    public OpenRaoMPVariable addGeneratorPowerVariable(String generatorId, double pMax, OffsetDateTime timestamp) {
        return solver.makeNumVar(-solver.infinity(), pMax, generatorPowerVariableId(generatorId, timestamp));
    }

    public OpenRaoMPVariable getGeneratorPowerVariable(String generatorId, OffsetDateTime timestamp) {
        return solver.getVariable(generatorPowerVariableId(generatorId, timestamp));
    }

    public OpenRaoMPVariable addMinMarginShiftedViolationVariable(Optional<OffsetDateTime> timestamp) {
        return solver.makeNumVar(0, infinity(), minMarginShiftedViolationVariableId(timestamp));
    }

    public OpenRaoMPVariable getMinMarginShiftedViolationVariable(Optional<OffsetDateTime> timestamp) {
        return solver.getVariable(minMarginShiftedViolationVariableId(timestamp));
    }

    public OpenRaoMPConstraint addMinMarginShiftedViolationConstraint(Optional<OffsetDateTime> timestamp, double minMarginUpperBound) {
        return solver.makeConstraint(minMarginUpperBound, infinity(), minMarginShiftedViolationConstraintId(timestamp));
    }

    public OpenRaoMPVariable addGeneratorStateVariable(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorState) {
        return solver.makeBoolVar(generatorStateVariableId(generatorId, generatorState, timestamp));
    }

    public OpenRaoMPVariable getGeneratorStateVariable(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorState) {
        return solver.getVariable(generatorStateVariableId(generatorId, generatorState, timestamp));
    }

    public OpenRaoMPVariable addGeneratorStateTransitionVariable(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorStateFrom, LinearProblem.GeneratorState generatorStateTo) {
        return solver.makeBoolVar(generatorStateTransitionVariableId(generatorId, generatorStateFrom, generatorStateTo, timestamp));
    }

    public OpenRaoMPVariable getGeneratorStateTransitionVariable(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorStateFrom, LinearProblem.GeneratorState generatorStateTo) {
        return solver.getVariable(generatorStateTransitionVariableId(generatorId, generatorStateFrom, generatorStateTo, timestamp));
    }

    public OpenRaoMPConstraint addUniqueGeneratorStateConstraint(String generatorId, OffsetDateTime timestamp) {
        return solver.makeConstraint(1, 1, uniqueGeneratorStateConstraintId(generatorId, timestamp));
    }

    public OpenRaoMPConstraint addGeneratorStateFromTransitionConstraint(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorStateFrom) {
        return solver.makeConstraint(0, 0, generatorStateFromTransitionConstraintId(generatorId, generatorStateFrom, timestamp));
    }

    public OpenRaoMPConstraint addGeneratorStateToTransitionConstraint(String generatorId, OffsetDateTime timestamp, LinearProblem.GeneratorState generatorStateTo) {
        return solver.makeConstraint(0, 0, generatorStateToTransitionConstraintId(generatorId, generatorStateTo, timestamp));
    }

    public OpenRaoMPConstraint addGeneratorPowerOnOffConstraint(String generatorId, OffsetDateTime timestamp, double lb, double ub, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, generatorPowerOnOffConstraintId(generatorId, timestamp, positiveOrNegative));
    }

    public OpenRaoMPConstraint addGeneratorPowerTransitionConstraint(String generatorId, double lb, double ub, OffsetDateTime timestamp, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, generatorPowerTransitionConstraintId(generatorId, timestamp, positiveOrNegative));
    }

    public OpenRaoMPConstraint getGeneratorPowerTransitionConstraint(String generatorId, OffsetDateTime timestamp, AbsExtension positiveOrNegative) {
        return solver.getConstraint(generatorPowerTransitionConstraintId(generatorId, timestamp, positiveOrNegative));
    }

    public OpenRaoMPConstraint addGeneratorToInjectionConstraint(String generatorId, InjectionRangeAction injectionRangeAction, OffsetDateTime timestamp) {
        return solver.makeConstraint(0.0, 0.0, generatorToInjectionConstraintId(generatorId, injectionRangeAction, timestamp));
    }

    public OpenRaoMPConstraint getGeneratorToInjectionConstraint(String generatorId, InjectionRangeAction injectionRangeAction, OffsetDateTime timestamp) {
        return solver.getConstraint(generatorToInjectionConstraintId(generatorId, injectionRangeAction, timestamp));
    }

    public OpenRaoMPConstraint addGeneratorStartingUpConstraint(String generatorId, OffsetDateTime stateChangingTimestamp, OffsetDateTime otherTimestamp) {
        return solver.makeConstraint(-infinity(), 0.0, generatorStartingUpConstraintId(generatorId, stateChangingTimestamp, otherTimestamp));
    }

    public OpenRaoMPConstraint addGeneratorShuttingDownConstraint(String generatorId, OffsetDateTime stateChangingTimestamp, OffsetDateTime otherTimestamp) {
        return solver.makeConstraint(-infinity(), 0.0, generatorShuttingDownConstraintId(generatorId, stateChangingTimestamp, otherTimestamp));
    }

    public double infinity() {
        return solver.infinity();
    }
}
