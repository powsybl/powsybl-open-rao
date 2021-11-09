/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.rao_commons.result_api.LinearProblemStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.google.ortools.linearsolver.*;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.rao_commons.linear_optimisation.LinearProblemIdGenerator.*;
import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class LinearProblem {
    private static final String OPT_PROBLEM_NAME = "range action opt problem";
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearProblem.class);

    static {
        try {
            NativeLibraryLoader.loadNativeLibrary("jniortools");
        } catch (Exception e) {
            LOGGER.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
        }
    }

    public enum AbsExtension {
        POSITIVE,
        NEGATIVE
    }

    public enum VariationExtension {
        UPWARD,
        DOWNWARD
    }

    public enum MarginExtension {
        BELOW_THRESHOLD,
        ABOVE_THRESHOLD
    }

    public enum BoundExtension {
        LOWER_BOUND,
        UPPER_BOUND
    }

    private final List<ProblemFiller> fillers;
    private final Set<FlowCnec> cnecs = new HashSet<>();
    private final Set<RangeAction> rangeActions = new HashSet<>();
    private final MPSolver solver;
    private final double relativeMipGap;
    private final String solverSpecificParameters;
    private LinearProblemStatus status;

    public LinearProblem(List<ProblemFiller> fillers, MPSolver mpSolver) {
        solver = mpSolver;
        solver.objective().setMinimization();
        this.fillers = fillers;
        this.relativeMipGap = RaoParameters.DEFAULT_RELATIVE_MIP_GAP;
        this.solverSpecificParameters = RaoParameters.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;
    }

    private LinearProblem(List<ProblemFiller> fillers, RaoParameters.Solver solverName, double relativeMipGap, String solverSpecificParameters) {
        switch (solverName) {
            case CBC:
                this.solver = new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
                break;

            case SCIP:
                this.solver = new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.SCIP_MIXED_INTEGER_PROGRAMMING);
                break;

            case XPRESS:
                this.solver = new MPSolver(OPT_PROBLEM_NAME, MPSolver.OptimizationProblemType.XPRESS_MIXED_INTEGER_PROGRAMMING);
                break;

            default:
                throw new FaraoException(format("unknown solver %s in RAO parameters", solverName));
        }
        this.solver.objective().setMinimization();
        this.fillers = fillers;
        this.relativeMipGap = relativeMipGap;
        this.solverSpecificParameters = solverSpecificParameters;
    }

    final List<ProblemFiller> getFillers() {
        return Collections.unmodifiableList(fillers);
    }

    public final Set<FlowCnec> getCnecs() {
        return Collections.unmodifiableSet(cnecs);
    }

    public final Set<RangeAction> getRangeActions() {
        return Collections.unmodifiableSet(rangeActions);
    }

    public LinearProblemStatus getStatus() {
        return status;
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
        return solver.objective();
    }

    public int numVariables() {
        return solver.numVariables();
    }

    public int numConstraints() {
        return solver.numConstraints();
    }

    public MPVariable addFlowVariable(double lb, double ub, FlowCnec cnec) {
        cnecs.add(cnec);
        return solver.makeNumVar(lb, ub, flowVariableId(cnec));
    }

    public MPVariable getFlowVariable(FlowCnec cnec) {
        return solver.lookupVariableOrNull(flowVariableId(cnec));
    }

    public MPConstraint addFlowConstraint(double lb, double ub, FlowCnec cnec) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec));
    }

    public MPConstraint getFlowConstraint(FlowCnec cnec) {
        return solver.lookupConstraintOrNull(flowConstraintId(cnec));
    }

    public MPVariable addRangeActionSetpointVariable(double lb, double ub, RangeAction rangeAction) {
        rangeActions.add(rangeAction);
        return solver.makeNumVar(lb, ub, rangeActionSetpointVariableId(rangeAction));
    }

    public MPVariable getRangeActionSetpointVariable(RangeAction rangeAction) {
        return solver.lookupVariableOrNull(rangeActionSetpointVariableId(rangeAction));
    }

    public MPVariable addPstTapVariationVariable(double lb, double ub, PstRangeAction rangeAction, VariationExtension variation) {
        rangeActions.add(rangeAction);
        return solver.makeIntVar(lb, ub, pstTapVariableVariationId(rangeAction, variation));
    }

    public MPVariable getPstTapVariationVariable(PstRangeAction rangeAction, VariationExtension variation) {
        return solver.lookupVariableOrNull(pstTapVariableVariationId(rangeAction, variation));
    }

    public MPVariable addPstTapVariationBinary(PstRangeAction rangeAction, VariationExtension variation) {
        rangeActions.add(rangeAction);
        return solver.makeBoolVar(pstTapBinaryVariationId(rangeAction, variation));
    }

    public MPVariable getPstTapVariationBinary(PstRangeAction rangeAction, VariationExtension variation) {
        return solver.lookupVariableOrNull(pstTapBinaryVariationId(rangeAction, variation));
    }

    public MPConstraint addTapToAngleConversionConstraint(double lb, double ub, PstRangeAction rangeAction) {
        rangeActions.add(rangeAction);
        return solver.makeConstraint(lb, ub, tapToAngleConversionConstraintId(rangeAction));
    }

    public MPConstraint getTapToAngleConversionConstraint(PstRangeAction rangeAction) {
        return solver.lookupConstraintOrNull(tapToAngleConversionConstraintId(rangeAction));
    }

    public MPConstraint addUpOrDownPstVariationConstraint(PstRangeAction rangeAction) {
        rangeActions.add(rangeAction);
        return solver.makeConstraint(upOrDownPstVariationConstraintId(rangeAction));
    }

    public MPConstraint getUpOrDownPstVariationConstraint(PstRangeAction rangeAction) {
        return solver.lookupConstraintOrNull(upOrDownPstVariationConstraintId(rangeAction));
    }

    public MPConstraint addIsVariationInDirectionConstraint(PstRangeAction rangeAction, VariationExtension variation) {
        rangeActions.add(rangeAction);
        return solver.makeConstraint(isVariationInDirectionConstraintId(rangeAction, variation));
    }

    public MPConstraint getIsVariationInDirectionConstraint(PstRangeAction rangeAction, VariationExtension variation) {
        return solver.lookupConstraintOrNull(isVariationInDirectionConstraintId(rangeAction, variation));
    }

    public MPVariable addRangeActionGroupSetpointVariable(double lb, double ub, String rangeActionGroupId) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetpointVariableId(rangeActionGroupId));
    }

    public MPVariable getRangeActionGroupSetpointVariable(String rangeActionGroupId) {
        return solver.lookupVariableOrNull(rangeActionGroupSetpointVariableId(rangeActionGroupId));
    }

    public MPVariable addPstGroupTapVariable(double lb, double ub, String rangeActionGroupId) {
        return solver.makeNumVar(lb, ub, pstGroupTapVariableId(rangeActionGroupId));
    }

    public MPVariable getPstGroupTapVariable(String rangeActionGroupId) {
        return solver.lookupVariableOrNull(pstGroupTapVariableId(rangeActionGroupId));
    }

    public MPConstraint addRangeActionGroupSetpointConstraint(double lb, double ub, RangeAction rangeAction) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetpointConstraintId(rangeAction));
    }

    public MPConstraint getRangeActionGroupSetpointConstraint(RangeAction rangeAction) {
        return solver.lookupConstraintOrNull(rangeActionGroupSetpointConstraintId(rangeAction));
    }

    public MPConstraint addPstGroupTapConstraint(double lb, double ub, PstRangeAction rangeAction) {
        return solver.makeConstraint(lb, ub, pstGroupTapConstraintId(rangeAction));
    }

    public MPConstraint getPstGroupTapConstraint(PstRangeAction rangeAction) {
        return solver.lookupConstraintOrNull(pstGroupTapConstraintId(rangeAction));
    }

    public MPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction rangeAction) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction));
    }

    public MPVariable getAbsoluteRangeActionVariationVariable(RangeAction rangeAction) {
        return solver.lookupVariableOrNull(absoluteRangeActionVariationVariableId(rangeAction));
    }

    public MPConstraint addAbsoluteRangeActionVariationConstraint(double lb, double ub, RangeAction rangeAction, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, absoluteRangeActionVariationConstraintId(rangeAction, positiveOrNegative));
    }

    public MPConstraint getAbsoluteRangeActionVariationConstraint(RangeAction rangeAction, AbsExtension positiveOrNegative) {
        return solver.lookupConstraintOrNull(absoluteRangeActionVariationConstraintId(rangeAction, positiveOrNegative));
    }

    public MPConstraint addMinimumMarginConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumMarginConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumRelativeMarginConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPVariable addMinimumMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumMarginVariableId());
    }

    public MPVariable getMinimumMarginVariable() {
        return solver.lookupVariableOrNull(minimumMarginVariableId());
    }

    public MPVariable addMinimumRelativeMarginVariable(double lb, double ub) {
        return solver.makeNumVar(lb, ub, minimumRelativeMarginVariableId());
    }

    public MPVariable getMinimumRelativeMarginVariable() {
        return solver.lookupVariableOrNull(minimumRelativeMarginVariableId());
    }

    //Begin MaxLoopFlowFiller section
    public MPConstraint addMaxLoopFlowConstraint(double lb, double ub, FlowCnec cnec, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPConstraint getMaxLoopFlowConstraint(FlowCnec cnec, BoundExtension lbOrUb) {
        return solver.lookupConstraintOrNull(maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPVariable addLoopflowViolationVariable(double lb, double ub, FlowCnec cnec) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec));
    }

    public MPVariable getLoopflowViolationVariable(FlowCnec cnec) {
        return solver.lookupVariableOrNull(loopflowViolationVariableId(cnec));
    }

    public MPVariable addMnecViolationVariable(double lb, double ub, FlowCnec mnec) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec));
    }

    public MPVariable getMnecViolationVariable(FlowCnec mnec) {
        return solver.lookupVariableOrNull(mnecViolationVariableId(mnec));
    }

    public MPConstraint addMnecFlowConstraint(double lb, double ub, FlowCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPConstraint getMnecFlowConstraint(FlowCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPVariable addMarginDecreaseBinaryVariable(FlowCnec cnec) {
        return solver.makeIntVar(0, 1, marginDecreaseVariableId(cnec));
    }

    public MPVariable getMarginDecreaseBinaryVariable(FlowCnec cnec) {
        return solver.lookupVariableOrNull(marginDecreaseVariableId(cnec));
    }

    public MPConstraint addMarginDecreaseConstraint(double lb, double ub, FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMarginDecreaseConstraint(FlowCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public static double infinity() {
        return MPSolver.infinity();
    }

    public LinearProblemStatus solve() {
        MPSolverParameters solveConfiguration = new MPSolverParameters();
        solveConfiguration.setDoubleParam(MPSolverParameters.DoubleParam.RELATIVE_MIP_GAP, relativeMipGap);
        if (solverSpecificParameters != null) {
            this.solver.setSolverSpecificParametersAsString(solverSpecificParameters);
        }
        status = convertResultStatus(solver.solve(solveConfiguration));
        return status;
    }

    public RangeActionResult getResults() {
        return new LinearProblemResult(this);
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillers.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult));
    }

    public void update(FlowResult flowResult, SensitivityResult sensitivityResult, RangeActionResult rangeActionResult) {
        fillers.forEach(problemFiller -> problemFiller.update(this, flowResult, sensitivityResult, rangeActionResult));
    }

    public static LinearProblemBuilder create() {
        return new LinearProblemBuilder();
    }

    public static class LinearProblemBuilder {
        private final List<ProblemFiller> problemFillers = new ArrayList<>();
        private FlowResult flowResult;
        private SensitivityResult sensitivityResult;
        private RaoParameters.Solver solverName = RaoParameters.DEFAULT_SOLVER;
        private double relativeMipGap = RaoParameters.DEFAULT_RELATIVE_MIP_GAP;
        private String solverSpecificParameters = RaoParameters.DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

        public LinearProblemBuilder withProblemFiller(ProblemFiller problemFiller) {
            problemFillers.add(problemFiller);
            return this;
        }

        public LinearProblemBuilder withBranchResult(FlowResult flowResult) {
            this.flowResult = flowResult;
            return this;
        }

        public LinearProblemBuilder withSensitivityResult(SensitivityResult sensitivityResult) {
            this.sensitivityResult = sensitivityResult;
            return this;
        }

        public LinearProblemBuilder withSolver(RaoParameters.Solver solverName) {
            this.solverName = solverName;
            return this;
        }

        public LinearProblemBuilder withRelativeMipGap(double relativeMipGap) {
            this.relativeMipGap = relativeMipGap;
            return this;
        }

        public LinearProblemBuilder withSolverSpecificParameters(String solverSpecificParameters) {
            this.solverSpecificParameters = solverSpecificParameters;
            return this;
        }

        public LinearProblem build() {
            LinearProblem linearProblem = new LinearProblem(problemFillers, solverName, relativeMipGap, solverSpecificParameters);
            // TODO: add checks on fillers consistency
            linearProblem.fill(flowResult, sensitivityResult);
            return linearProblem;
        }
    }
}
