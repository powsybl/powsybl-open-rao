/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoProblem {

    public static double infinity() {
        return MPSolver.infinity();
    }

    public static final double PENALTY_COST = 0.1;

    private MPSolver solver;
    private MPObjective objective;
    private List<MPVariable> flowVariables;
    private List<MPConstraint> flowConstraints;
    private List<MPConstraint> minimumMarginConstraints;
    private List<MPVariable> negativeRangeActionVariables;
    private List<MPVariable> positiveRangeActionVariables;
    private static final String POS_MIN_MARGIN = "pos-min-margin";

    public LinearRaoProblem(MPSolver mpSolver) {
        solver = mpSolver;
        flowVariables = new ArrayList<>();
        flowConstraints = new ArrayList<>();
        minimumMarginConstraints = new ArrayList<>();
        negativeRangeActionVariables = new ArrayList<>();
        positiveRangeActionVariables = new ArrayList<>();
    }

    protected LinearRaoProblem() {
        this(new MPSolver("linear rao", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING));
    }

    public List<MPVariable> getFlowVariables() {
        return flowVariables;
    }

    public MPVariable getFlowVariable(String cnecId) {
        return flowVariables.stream()
            .filter(variable -> variable.name().equals(getFlowVariableId(cnecId)))
            .findFirst()
            .orElse(null);
    }

    public List<MPConstraint> getFlowConstraints() {
        return flowConstraints;
    }

    public MPConstraint getFlowConstraint(String cnecId) {
        return flowConstraints.stream()
            .filter(variable -> variable.name().equals(getFlowConstraintId(cnecId)))
            .findFirst()
            .orElse(null);
    }

    public List<MPVariable> getNegativeRangeActionVariables() {
        return negativeRangeActionVariables;
    }

    public MPVariable getNegativeRangeActionVariable(String rancgeActionId, String networkElementId) {
        return negativeRangeActionVariables.stream()
            .filter(variable -> variable.name().equals(getNegativeRangeActionVariableId(rancgeActionId, networkElementId)))
            .findFirst()
            .orElse(null);
    }

    public List<MPVariable> getPositiveRangeActionVariables() {
        return positiveRangeActionVariables;
    }

    public MPVariable getPositiveRangeActionVariable(String rancgeActionId, String networkElementId) {
        return positiveRangeActionVariables.stream()
            .filter(variable -> variable.name().equals(getPositiveRangeActionVariableId(rancgeActionId, networkElementId)))
            .findFirst()
            .orElse(null);
    }

    public MPConstraint getMinimumMarginConstraint(String cnecId, String minMax) {
        return minimumMarginConstraints.stream()
            .filter(constraint -> constraint.name().equals(getMinimumMarginConstraintId(cnecId, minMax)))
            .findFirst()
            .orElse(null);
    }

    public MPVariable getMinimumMarginVariable() {
        return solver.lookupVariableOrNull(POS_MIN_MARGIN);
    }

    public MPObjective getObjective() {
        return objective;
    }

    public void addCnec(String cnecId, double referenceFlow, double minFlow, double maxFlow) {
        MPVariable flowVariable = solver.makeNumVar(minFlow, maxFlow, getFlowVariableId(cnecId));
        flowVariables.add(flowVariable);
        MPConstraint flowConstraint = solver.makeConstraint(referenceFlow, referenceFlow, getFlowConstraintId(cnecId));
        flowConstraints.add(flowConstraint);
        flowConstraint.setCoefficient(flowVariable, 1);
    }

    public void addRangeActionVariable(String rangeActionId, String networkElementId, double maxNegativeVariation, double maxPositiveVariation) {
        String negativeVariableName = getNegativeRangeActionVariableId(rangeActionId, networkElementId);
        String positiveVariableName = getPositiveRangeActionVariableId(rangeActionId, networkElementId);

        solver.makeNumVar(0, maxNegativeVariation, negativeVariableName);
        solver.makeNumVar(0, maxPositiveVariation, positiveVariableName);

        negativeRangeActionVariables.add(solver.lookupVariableOrNull(negativeVariableName));
        positiveRangeActionVariables.add(solver.lookupVariableOrNull(positiveVariableName));
    }

    public void addRangeActionFlowOnBranch(String cnecId, String rangeActionId, String networkElementId, double sensitivity) {
        MPConstraint flowConstraint = solver.lookupConstraintOrNull(getFlowConstraintId(cnecId));
        if (flowConstraint == null) {
            throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnecId));
        }
        MPVariable positiveRangeActionVariable = solver.lookupVariableOrNull(getPositiveRangeActionVariableId(rangeActionId, networkElementId));
        MPVariable negativeRangeActionVariable = solver.lookupVariableOrNull(getNegativeRangeActionVariableId(rangeActionId, networkElementId));
        if (positiveRangeActionVariable == null || negativeRangeActionVariable == null) {
            throw new FaraoException(String.format("Range action variable for %s on %s has not been defined yet.", rangeActionId, networkElementId));
        }
        flowConstraint.setCoefficient(
            positiveRangeActionVariable,
            -sensitivity);
        flowConstraint.setCoefficient(
            negativeRangeActionVariable,
            sensitivity);
    }

    public void addMinimumMarginVariable() {
        solver.makeNumVar(-Double.MAX_VALUE, Double.MAX_VALUE, POS_MIN_MARGIN);
    }

    public void addMinimumMarginConstraints(String cnecId, double min, double max) {
        MPVariable flowVariable = solver.lookupVariableOrNull(getFlowVariableId(cnecId));
        MPConstraint minimumMarginByMax = solver.makeConstraint(-Double.MAX_VALUE, max, getMinimumMarginConstraintId(cnecId, "max"));
        minimumMarginByMax.setCoefficient(flowVariable, 1);
        minimumMarginConstraints.add(minimumMarginByMax);
        MPConstraint minimumMarginByMin = solver.makeConstraint(-Double.MAX_VALUE, -min, getMinimumMarginConstraintId(cnecId, "min"));
        minimumMarginByMin.setCoefficient(flowVariable, -1);
        minimumMarginConstraints.add(minimumMarginByMin);
    }

    public void addPosMinObjective() {
        objective = solver.objective();
        objective.setCoefficient(solver.lookupVariableOrNull(POS_MIN_MARGIN), 1);
        getNegativeRangeActionVariables().forEach(negativePstShiftVariable -> objective.setCoefficient(negativePstShiftVariable, -PENALTY_COST));
        getPositiveRangeActionVariables().forEach(positivePstShiftVariable -> objective.setCoefficient(positivePstShiftVariable, -PENALTY_COST));
        objective.setMaximization();
    }

    private String getFlowVariableId(String cnecId) {
        return String.format("%s-variable", cnecId);
    }

    private String getFlowConstraintId(String cnecId) {
        return String.format("%s-constraint", cnecId);
    }

    private String getPositiveRangeActionVariableId(String rangeActionId, String networkElementId) {
        return String.format("positive-%s-%s-variable", rangeActionId, networkElementId);
    }

    private String getNegativeRangeActionVariableId(String rangeActionId, String networkElementId) {
        return String.format("negative-%s-%s-variable", rangeActionId, networkElementId);
    }

    private String getMinimumMarginConstraintId(String branch, String minMax) {
        return String.format("%s-%s-%s", POS_MIN_MARGIN, branch, minMax);
    }
}
