/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.util.NativeLibraryLoader;
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

    static {
        NativeLibraryLoader.loadNativeLibraries();
    }

    private MPSolver solver;
    private List<MPVariable> flowVariables;
    private List<MPConstraint> flowConstraints;
    private List<MPVariable> negativePstShiftVariables;
    private List<MPVariable> positivePstShiftVariables;
    private double penaltyCost = 1;

    public LinearRaoProblem(MPSolver mpSolver) {
        solver = mpSolver;
        flowVariables = new ArrayList<>();
        flowConstraints = new ArrayList<>();
        negativePstShiftVariables = new ArrayList<>();
        positivePstShiftVariables = new ArrayList<>();
    }

    protected LinearRaoProblem() {
        this(new MPSolver("linear rao", MPSolver.OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING));
    }

    MPSolver getSolver() {
        return solver;
    }

    public MPVariable getFlowVariable(String cnecId) {
        return flowVariables.stream()
            .filter(variable -> variable.name().equals(getFlowVariableId(cnecId)))
            .findFirst()
            .orElse(null);
    }

    public MPConstraint getFlowConstraint(String cnecId) {
        return flowConstraints.stream()
            .filter(variable -> variable.name().equals(getFlowConstraintId(cnecId)))
            .findFirst()
            .orElse(null);
    }

    public List<MPVariable> getNegativePstShiftVariables() {
        return negativePstShiftVariables;
    }

    public MPVariable getNegativePstShiftVariable(String rancgeActionId, String networkElementId) {
        return negativePstShiftVariables.stream()
            .filter(variable -> variable.name().equals(getNegativeRangeActionVariableId(rancgeActionId, networkElementId)))
            .findFirst()
            .orElse(null);
    }

    public List<MPVariable> getPositivePstShiftVariables() {
        return positivePstShiftVariables;
    }

    public MPVariable getPositivePstShiftVariable(String rancgeActionId, String networkElementId) {
        return positivePstShiftVariables.stream()
            .filter(variable -> variable.name().equals(getPositiveRangeActionVariableId(rancgeActionId, networkElementId)))
            .findFirst()
            .orElse(null);
    }

    public void addCnec(String cnecId, double referenceFlow, double minFlow, double maxFlow) {
        MPVariable flowVariable = solver.makeNumVar(minFlow, maxFlow, getFlowVariableId(cnecId));
        flowVariables.add(flowVariable);
        MPConstraint flowConstraint = solver.makeConstraint(referenceFlow, referenceFlow, getFlowConstraintId(cnecId));
        flowConstraints.add(flowConstraint);
        flowConstraint.setCoefficient(flowVariable, 1);
    }

    private static String getFlowVariableId(String cnecId) {
        return String.format("%s-variable", cnecId);
    }

    private static String getFlowConstraintId(String cnecId) {
        return String.format("%s-constraint", cnecId);
    }

    public void addRangeActionVariable(String rangeActionId, String networkElementId, double maxNegativeVariation, double maxPositiveVariation) {
        String negativeVariableName = getNegativeRangeActionVariableId(rangeActionId, networkElementId);
        String positiveVariableName = getPositiveRangeActionVariableId(rangeActionId, networkElementId);

        solver.makeNumVar(0, maxNegativeVariation, negativeVariableName);
        solver.makeNumVar(0, maxPositiveVariation, positiveVariableName);

        negativePstShiftVariables.add(solver.lookupVariableOrNull(negativeVariableName));
        positivePstShiftVariables.add(solver.lookupVariableOrNull(positiveVariableName));
    }

    private static String getPositiveRangeActionVariableId(String rangeActionId, String networkElementId) {
        return String.format("positive-%s-%s-variable", rangeActionId, networkElementId);
    }

    private static String getNegativeRangeActionVariableId(String rangeActionId, String networkElementId) {
        return String.format("negative-%s-%s-variable", rangeActionId, networkElementId);
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

    public void getMinPosMargin() {
        solver.makeNumVar(-MPSolver.infinity(), MPSolver.infinity(), "min-pos-margin");
    }

    public String getMinPosMarginId(String branch, String minMax) {
        return String.format("min-pos-margin-%s-%s", branch, minMax);
    }

    public void addMinPosMargin(String cnecId, double min, double max) {
        MPVariable flowVariable = solver.lookupVariableOrNull(getFlowVariableId(cnecId));
        MPConstraint flowConstraintMax = solver.makeConstraint(-MPSolver.infinity(), max, getMinPosMarginId(cnecId, "max"));
        flowConstraintMax.setCoefficient(flowVariable, 1);

        MPConstraint flowConstraintMin = solver.makeConstraint(-MPSolver.infinity(), -min, getMinPosMarginId(cnecId, "min"));
        flowConstraintMin.setCoefficient(flowVariable, -1);
    }

    public void getMinPosObjective() {
        MPObjective objective = solver.objective();
        objective.setCoefficient(solver.lookupVariableOrNull("min-pos-margin"), 1);
        getNegativePstShiftVariables().forEach(negativePstShiftVariable -> objective.setCoefficient(negativePstShiftVariable, -penaltyCost));
        getPositivePstShiftVariables().forEach(positivePstShiftVariable -> objective.setCoefficient(positivePstShiftVariable, -penaltyCost));
        objective.setMaximization();
    }
}
