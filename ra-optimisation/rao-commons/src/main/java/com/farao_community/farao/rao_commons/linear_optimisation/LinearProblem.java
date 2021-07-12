/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.farao_community.farao.rao_commons.result_api.LinearProblemStatus;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.RangeActionResult;
import com.farao_community.farao.rao_commons.result_api.SensitivityResult;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
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
    private LinearProblemStatus status;

    public LinearProblem(List<ProblemFiller> fillers, MPSolver mpSolver) {
        solver = mpSolver;
        solver.objective().setMinimization();
        this.fillers = fillers;
    }

    private LinearProblem(List<ProblemFiller> fillers) {
        this(fillers, new MPSolver("linear rao", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING));
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

    public MPVariable addRangeActionSetPointVariable(double lb, double ub, RangeAction rangeAction) {
        rangeActions.add(rangeAction);
        return solver.makeNumVar(lb, ub, rangeActionSetPointVariableId(rangeAction));
    }

    public MPVariable getRangeActionSetPointVariable(RangeAction rangeAction) {
        return solver.lookupVariableOrNull(rangeActionSetPointVariableId(rangeAction));
    }

    public MPVariable addRangeActionGroupSetPointVariable(double lb, double ub, String rangeActionGroupId) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetPointVariableId(rangeActionGroupId));
    }

    public MPVariable getRangeActionGroupSetPointVariable(String rangeActionGroupId) {
        return solver.lookupVariableOrNull(rangeActionGroupSetPointVariableId(rangeActionGroupId));
    }

    public MPConstraint addRangeActionGroupSetPointConstraint(double lb, double ub, RangeAction rangeAction) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetPointConstraintId(rangeAction));
    }

    public MPConstraint getRangeActionGroupSetPointConstraint(RangeAction rangeAction) {
        return solver.lookupConstraintOrNull(rangeActionGroupSetPointConstraintId(rangeAction));
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
        status = convertResultStatus(solver.solve());
        return status;
    }

    public RangeActionResult getResults() {
        return new LinearProblemResult(this);
    }

    public void fill(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillers.forEach(problemFiller -> problemFiller.fill(this, flowResult, sensitivityResult));
    }

    public void update(FlowResult flowResult, SensitivityResult sensitivityResult) {
        fillers.forEach(problemFiller -> problemFiller.update(this, flowResult, sensitivityResult));
    }

    public static LinearProblemBuilder create() {
        return new LinearProblemBuilder();
    }

    public static class LinearProblemBuilder {
        private final List<ProblemFiller> problemFillers = new ArrayList<>();
        private FlowResult flowResult;
        private SensitivityResult sensitivityResult;

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

        public LinearProblem build() {
            LinearProblem linearProblem = new LinearProblem(problemFillers);
            // TODO: add checks on fillers consistency
            linearProblem.fill(flowResult, sensitivityResult);
            return linearProblem;
        }
    }
}
