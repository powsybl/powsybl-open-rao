/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.LinearProblemStatus;
import com.farao_community.farao.rao_api.results.RangeActionResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.farao_community.farao.rao_commons.linear_optimisation.fillers.ProblemFiller;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblem {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinearProblem.class);
    private static final String VARIABLE_SUFFIX = "variable";
    private static final String CONSTRAINT_SUFFIX = "constraint";
    private static final String SEPARATOR = "_";

    private static final String FLOW = "flow";
    private static final String SET_POINT = "setpoint";
    private static final String VIRTUAL_SET_POINT = "virtualsetpoint";
    private static final String ABSOLUTE_VARIATION = "absolutevariation";
    private static final String MIN_MARGIN = "minmargin";
    private static final String MIN_RELATIVE_MARGIN = "minrelmargin";
    private static final String MAX_LOOPFLOW = "maxloopflow";
    private static final String LOOPFLOWVIOLATION = "loopflowviolation";
    private static final String MNEC_VIOLATION = "mnecviolation";
    private static final String MNEC_FLOW = "mnecflow";
    private static final String MARGIN_DECREASE = "margindecrease";

    private final List<ProblemFiller> fillers;
    private LinearProblemStatus status;
    private final Set<BranchCnec> cnecs = new HashSet<>();
    private final Set<RangeAction> rangeActions = new HashSet<>();

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

    private MPSolver solver;

    public LinearProblem(List<ProblemFiller> fillers, MPSolver mpSolver) {
        solver = mpSolver;
        solver.objective().setMinimization();
        this.fillers = fillers;
    }

    public LinearProblem(List<ProblemFiller> fillers) {
        this(fillers, new MPSolver("linear rao", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING));
    }

    public LinearProblemStatus getStatus() {
        return status;
    }

    public final Set<BranchCnec> getCnecs() {
        return Collections.unmodifiableSet(cnecs);
    }

    public final Set<RangeAction> getRangeActions() {
        return Collections.unmodifiableSet(rangeActions);
    }

    public MPObjective getObjective() {
        return solver.objective();
    }

    private String flowVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addFlowVariable(double lb, double ub, BranchCnec cnec) {
        cnecs.add(cnec);
        return solver.makeNumVar(lb, ub, flowVariableId(cnec));
    }

    public MPVariable getFlowVariable(BranchCnec cnec) {
        return solver.lookupVariableOrNull(flowVariableId(cnec));
    }

    private String flowConstraintId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addFlowConstraint(double lb, double ub, BranchCnec cnec) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec));
    }

    public MPConstraint getFlowConstraint(BranchCnec cnec) {
        return solver.lookupConstraintOrNull(flowConstraintId(cnec));
    }

    private String rangeActionSetPointVariableId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addRangeActionSetPointVariable(double lb, double ub, RangeAction rangeAction) {
        rangeActions.add(rangeAction);
        return solver.makeNumVar(lb, ub, rangeActionSetPointVariableId(rangeAction));
    }

    public MPVariable getRangeActionSetPointVariable(RangeAction rangeAction) {
        return solver.lookupVariableOrNull(rangeActionSetPointVariableId(rangeAction));
    }

    private String rangeActionGroupSetPointVariableId(String rangeActionGroupId) {
        return rangeActionGroupId + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addRangeActionGroupSetPointVariable(double lb, double ub, String rangeActionGroupId) {
        return solver.makeNumVar(lb, ub, rangeActionGroupSetPointVariableId(rangeActionGroupId));
    }

    public MPVariable getRangeActionGroupSetPointVariable(String rangeActionGroupId) {
        return solver.lookupVariableOrNull(rangeActionGroupSetPointVariableId(rangeActionGroupId));
    }

    public String rangeActionGroupSetPointConstraintId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + rangeAction.getGroupId().orElseThrow() + SEPARATOR + VIRTUAL_SET_POINT + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addRangeActionGroupSetPointConstraint(double lb, double ub, RangeAction rangeAction) {
        return solver.makeConstraint(lb, ub, rangeActionGroupSetPointConstraintId(rangeAction));
    }

    public MPConstraint getRangeActionGroupSetPointConstraint(RangeAction rangeAction) {
        return solver.lookupConstraintOrNull(rangeActionGroupSetPointConstraintId(rangeAction));
    }

    public String absoluteRangeActionVariationVariableId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addAbsoluteRangeActionVariationVariable(double lb, double ub, RangeAction rangeAction) {
        return solver.makeNumVar(lb, ub, absoluteRangeActionVariationVariableId(rangeAction));
    }

    public MPVariable getAbsoluteRangeActionVariationVariable(RangeAction rangeAction) {
        return solver.lookupVariableOrNull(absoluteRangeActionVariationVariableId(rangeAction));
    }

    private String absoluteRangeActionVariationConstraintId(RangeAction rangeAction, AbsExtension positiveOrNegative) {
        return rangeAction.getId() + SEPARATOR + ABSOLUTE_VARIATION + positiveOrNegative.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addAbsoluteRangeActionVariationConstraint(double lb, double ub, RangeAction rangeAction, AbsExtension positiveOrNegative) {
        return solver.makeConstraint(lb, ub, absoluteRangeActionVariationConstraintId(rangeAction, positiveOrNegative));
    }

    public MPConstraint getAbsoluteRangeActionVariationConstraint(RangeAction rangeAction, AbsExtension positiveOrNegative) {
        return solver.lookupConstraintOrNull(absoluteRangeActionVariationConstraintId(rangeAction, positiveOrNegative));
    }

    private String minimumMarginConstraintId(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    private String minimumRelativeMarginConstraintId(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_RELATIVE_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMinimumMarginConstraint(double lb, double ub, BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumMarginConstraint(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumRelativeMarginConstraint(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    private String minimumMarginVariableId() {
        return MIN_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
    }

    private String minimumRelativeMarginVariableId() {
        return MIN_RELATIVE_MARGIN + SEPARATOR + VARIABLE_SUFFIX;
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
    public MPConstraint addMaxLoopFlowConstraint(double lb, double ub, BranchCnec cnec, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    private String maxLoopFlowConstraintId(BranchCnec cnec, BoundExtension lbOrUb) {
        return cnec.getId() + SEPARATOR + MAX_LOOPFLOW + lbOrUb.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint getMaxLoopFlowConstraint(BranchCnec cnec, BoundExtension lbOrUb) {
        return solver.lookupConstraintOrNull(maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPVariable addLoopflowViolationVariable(double lb, double ub, BranchCnec cnec) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec));
    }

    public MPVariable getLoopflowViolationVariable(BranchCnec cnec) {
        return solver.lookupVariableOrNull(loopflowViolationVariableId(cnec));
    }

    private String loopflowViolationVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + LOOPFLOWVIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    private String mnecViolationVariableId(BranchCnec mnec) {
        return mnec.getId() + SEPARATOR + MNEC_VIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addMnecViolationVariable(double lb, double ub, BranchCnec mnec) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec));
    }

    public MPVariable getMnecViolationVariable(BranchCnec mnec) {
        return solver.lookupVariableOrNull(mnecViolationVariableId(mnec));
    }

    private String mnecFlowConstraintId(BranchCnec mnec, MarginExtension belowOrAboveThreshold) {
        return mnec.getId() + SEPARATOR + MNEC_FLOW + belowOrAboveThreshold.toString().toLowerCase()  + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMnecFlowConstraint(double lb, double ub, BranchCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPConstraint getMnecFlowConstraint(BranchCnec mnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    private String marginDecreaseVariableId(BranchCnec cnec) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addMarginDecreaseBinaryVariable(BranchCnec cnec) {
        return solver.makeIntVar(0, 1, marginDecreaseVariableId(cnec));
    }

    public MPVariable getMarginDecreaseBinaryVariable(BranchCnec cnec) {
        return solver.lookupVariableOrNull(marginDecreaseVariableId(cnec));
    }

    private String marginDecreaseConstraintId(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMarginDecreaseConstraint(double lb, double ub, BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMarginDecreaseConstraint(BranchCnec cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public double infinity() {
        return MPSolver.infinity();
    }

    public LinearProblemStatus solve() {
        status = convertResultStatus(solver.solve());
        return status;
    }

    public RangeActionResult getResults() {
        return new LinearProblemResult(this);
    }

    public MPSolver getSolver() {
        return solver;
    }

    public static LinearProblemBuilder create() {
        return new LinearProblemBuilder();
    }

    public void update(BranchResult branchResult, SensitivityResult sensitivityResult) {
        fillers.forEach(problemFiller -> problemFiller.update(this, branchResult, sensitivityResult));
    }

    public static class LinearProblemBuilder {
        private final List<ProblemFiller> problemFillers = new ArrayList<>();
        private BranchResult branchResult;
        private SensitivityResult sensitivityResult;

        public LinearProblemBuilder withProblemFiller(ProblemFiller problemFiller) {
            problemFillers.add(problemFiller);
            return this;
        }

        public LinearProblemBuilder withBranchResult(BranchResult branchResult) {
            this.branchResult = branchResult;
            return this;
        }

        public LinearProblemBuilder withSensitivityResult(SensitivityResult sensitivityResult) {
            this.sensitivityResult = sensitivityResult;
            return this;
        }

        public LinearProblem build() {
            LinearProblem linearProblem = new LinearProblem(problemFillers);
            // TODO: add checks on fillers consistency
            problemFillers.forEach(problemFiller -> problemFiller.fill(linearProblem, branchResult, sensitivityResult));
            return linearProblem;
        }
    }
}
