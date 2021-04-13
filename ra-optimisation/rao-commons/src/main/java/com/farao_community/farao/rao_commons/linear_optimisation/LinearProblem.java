/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.apache.commons.lang3.NotImplementedException;

import static java.lang.String.format;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LinearProblem {
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

    public enum SolveStatus {
        OPTIMAL,
        FEASIBLE,
        INFEASIBLE,
        UNBOUNDED,
        ABNORMAL,
        NOT_SOLVED
    }

    private static SolveStatus convertSolveStatus(MPSolver.ResultStatus status) {
        switch (status) {
            case OPTIMAL:
                return SolveStatus.OPTIMAL;
            case ABNORMAL:
                return SolveStatus.ABNORMAL;
            case FEASIBLE:
                return SolveStatus.FEASIBLE;
            case UNBOUNDED:
                return SolveStatus.UNBOUNDED;
            case INFEASIBLE:
                return SolveStatus.INFEASIBLE;
            case NOT_SOLVED:
                return SolveStatus.NOT_SOLVED;
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

    public LinearProblem(MPSolver mpSolver) {
        solver = mpSolver;
        solver.objective().setMinimization();
    }

    public LinearProblem() {
        this(new MPSolver("linear rao", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING));
    }

    public MPObjective getObjective() {
        return solver.objective();
    }

    private String flowVariableId(Cnec<?> cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addFlowVariable(double lb, double ub, Cnec<?> cnec) {
        return solver.makeNumVar(lb, ub, flowVariableId(cnec));
    }

    public MPVariable getFlowVariable(Cnec<?> cnec) {
        return solver.lookupVariableOrNull(flowVariableId(cnec));
    }

    private String flowConstraintId(Cnec<?> cnec) {
        return cnec.getId() + SEPARATOR + FLOW + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addFlowConstraint(double lb, double ub, Cnec<?> cnec) {
        return solver.makeConstraint(lb, ub, flowConstraintId(cnec));
    }

    public MPConstraint getFlowConstraint(Cnec<?> cnec) {
        return solver.lookupConstraintOrNull(flowConstraintId(cnec));
    }

    private String rangeActionSetPointVariableId(RangeAction rangeAction) {
        return rangeAction.getId() + SEPARATOR + SET_POINT + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addRangeActionSetPointVariable(double lb, double ub, RangeAction rangeAction) {
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

    private String minimumMarginConstraintId(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    private String minimumRelativeMarginConstraintId(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MIN_RELATIVE_MARGIN + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMinimumMarginConstraint(double lb, double ub, Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumMarginConstraint(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(minimumMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint addMinimumRelativeMarginConstraint(double lb, double ub, Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, minimumRelativeMarginConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMinimumRelativeMarginConstraint(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
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
    public MPConstraint addMaxLoopFlowConstraint(double lb, double ub, Cnec<?> cnec, BoundExtension lbOrUb) {
        return solver.makeConstraint(lb, ub, maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    private String maxLoopFlowConstraintId(Cnec<?> cnec, BoundExtension lbOrUb) {
        return cnec.getId() + SEPARATOR + MAX_LOOPFLOW + lbOrUb.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint getMaxLoopFlowConstraint(Cnec<?> cnec, BoundExtension lbOrUb) {
        return solver.lookupConstraintOrNull(maxLoopFlowConstraintId(cnec, lbOrUb));
    }

    public MPVariable addLoopflowViolationVariable(double lb, double ub, Cnec<?> cnec) {
        return solver.makeNumVar(lb, ub, loopflowViolationVariableId(cnec));
    }

    public MPVariable getLoopflowViolationVariable(Cnec<?> cnec) {
        return solver.lookupVariableOrNull(loopflowViolationVariableId(cnec));
    }

    private String loopflowViolationVariableId(Cnec<?> cnec) {
        return cnec.getId() + SEPARATOR + LOOPFLOWVIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    private String mnecViolationVariableId(Cnec<?> mnec) {
        return mnec.getId() + SEPARATOR + MNEC_VIOLATION + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addMnecViolationVariable(double lb, double ub, Cnec<?> mnec) {
        return solver.makeNumVar(lb, ub, mnecViolationVariableId(mnec));
    }

    public MPVariable getMnecViolationVariable(Cnec<?> mnec) {
        return solver.lookupVariableOrNull(mnecViolationVariableId(mnec));
    }

    private String mnecFlowConstraintId(Cnec<?> mnec, MarginExtension belowOrAboveThreshold) {
        return mnec.getId() + SEPARATOR + MNEC_FLOW + belowOrAboveThreshold.toString().toLowerCase()  + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMnecFlowConstraint(double lb, double ub, Cnec<?> mnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    public MPConstraint getMnecFlowConstraint(Cnec<?> mnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(mnecFlowConstraintId(mnec, belowOrAboveThreshold));
    }

    private String marginDecreaseVariableId(Cnec<?> cnec) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + SEPARATOR + VARIABLE_SUFFIX;
    }

    public MPVariable addMarginDecreaseBinaryVariable(Cnec<?> cnec) {
        return solver.makeIntVar(0, 1, marginDecreaseVariableId(cnec));
    }

    public MPVariable getMarginDecreaseBinaryVariable(Cnec<?> cnec) {
        return solver.lookupVariableOrNull(marginDecreaseVariableId(cnec));
    }

    private String marginDecreaseConstraintId(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return cnec.getId() + SEPARATOR + MARGIN_DECREASE + belowOrAboveThreshold.toString().toLowerCase() + SEPARATOR + CONSTRAINT_SUFFIX;
    }

    public MPConstraint addMarginDecreaseConstraint(double lb, double ub, Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return solver.makeConstraint(lb, ub, marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public MPConstraint getMarginDecreaseConstraint(Cnec<?> cnec, MarginExtension belowOrAboveThreshold) {
        return solver.lookupConstraintOrNull(marginDecreaseConstraintId(cnec, belowOrAboveThreshold));
    }

    public double infinity() {
        return MPSolver.infinity();
    }

    public SolveStatus solve() {
        return convertSolveStatus(solver.solve());
    }

    public MPSolver getSolver() {
        return solver;
    }
}
