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

    public static final double PENALTY_COST = 1;

    private MPSolver solver;
    private MPObjective objective;
    private List<MPVariable> flowVariables;
    private List<MPConstraint> flowConstraints;
    private List<MPConstraint> minimumMarginConstraints;
    private List<MPVariable> rangeActionValueVariables;
    private List<MPVariable> absoluteRangeActionVariationVariables;
    private List<MPConstraint> absoluteRangeActionVariationConstraints;
    public static final String POS_MIN_MARGIN = "pos-min-margin";

    public LinearRaoProblem(MPSolver mpSolver) {
        solver = mpSolver;
        flowVariables = new ArrayList<>();
        flowConstraints = new ArrayList<>();
        minimumMarginConstraints = new ArrayList<>();
        rangeActionValueVariables = new ArrayList<>();
        absoluteRangeActionVariationVariables = new ArrayList<>();
        absoluteRangeActionVariationConstraints = new ArrayList<>();
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

    public List<MPVariable> getRangeActionValueVariables() {
        return rangeActionValueVariables;
    }

    public MPVariable getRangeActionValueVariable(String rangeActionId) {
        return rangeActionValueVariables.stream()
            .filter(variable -> variable.name().equals(getRangeActionValueVariableId(rangeActionId)))
            .findFirst()
            .orElse(null);
    }

    /**
     * @param cnecId id of the considered cnec
     * @param minMax can take two values: "min" or "max" depending on the expected MPConstraint
     * @return the MPConstraint ensuring that the minimal margin is smaller than the
     * minimum or maximum margin of the considered cnec, or null if this MPConstraint doesn't exist
     */
    public MPConstraint getMinimumMarginConstraint(String cnecId, String minMax) {
        return minimumMarginConstraints.stream()
            .filter(constraint -> constraint.name().equals(getMinimumMarginConstraintId(cnecId, minMax)))
            .findFirst()
            .orElse(null);
    }

    public MPVariable getMinimumMarginVariable() {
        return solver.lookupVariableOrNull(POS_MIN_MARGIN);
    }

    public List<MPVariable> getAboluteRangeActionVariationVariables() {
        return absoluteRangeActionVariationVariables;
    }

    public MPVariable getAboluteRangeActionVariationVariable(String rangeActionId) {
        return absoluteRangeActionVariationVariables.stream()
                .filter(variable -> variable.name().equals(getAboluteRangeActionVariationVariableId(rangeActionId)))
                .findFirst()
                .orElse(null);
    }

    public MPConstraint getAboluteRangeActionVariationConstraint(String rangeActionId, String minMax) {
        return absoluteRangeActionVariationConstraints.stream()
                .filter(constraint -> constraint.name().equals(getAboluteRangeActionVariationConstraintId(rangeActionId, minMax)))
                .findFirst()
                .orElse(null);
    }

    public MPObjective getObjective() {
        return objective;
    }

    /**
     * Creates a flow variable corresponding to the flow on the considered Cnec and add it to the flowVariables attribute.
     * Creates a flow constraint so that the flow on this Cnec is equal to the reference flow
     * @param cnecId id of the considered Cnec
     * @param referenceFlow initial active flow on the cnec (prior to linear optimization)
     */
    public void addCnec(String cnecId, double referenceFlow) {
        MPVariable flowVariable = solver.makeNumVar(-infinity(), infinity(), getFlowVariableId(cnecId));
        flowVariables.add(flowVariable);
        MPConstraint flowConstraint = solver.makeConstraint(referenceFlow, referenceFlow, getFlowConstraintId(cnecId));
        flowConstraints.add(flowConstraint);
        flowConstraint.setCoefficient(flowVariable, 1);
    }

    /**
     * Creates three {@link MPVariable}s corresponding to the RangeAction and its NetworkElement considered:
     * <ul>
     *     <li>one for the positive variation of the Range Action compared to the previous iteration of the
     *     LinearModeller, which is between 0 and maxPositiveVariation</li>
     *     <li>one for the negative variation of the Range Action compared to the previous iteration of the
     *     LinearModeller, which is between 0 and maxNegativeVariation</li>
     *     <li>one for the absolute variation compared to the initial situation provided to the LinearModeller</li>
     * </ul>
     * @param rangeActionId id of the range action considered
     * @param maxNegativeVariation maximum value that the negative variation of this range can take (positive or null)
     * @param maxPositiveVariation maximum value that the positive variation of this range can take (positive or null)
     */
    public void addRangeActionVariable(String rangeActionId, double initialRangeActionValue, double maxNegativeVariation, double maxPositiveVariation) {
        MPVariable valueVariable = solver.makeNumVar(initialRangeActionValue - maxNegativeVariation, initialRangeActionValue + maxPositiveVariation, getRangeActionValueVariableId(rangeActionId));
        MPVariable absoluteVariationVariable = solver.makeNumVar(0, infinity(), getAboluteRangeActionVariationVariableId(rangeActionId));

        MPConstraint absoluteVariationConstraintNegative = solver.makeConstraint(-initialRangeActionValue, infinity(), getAboluteRangeActionVariationConstraintId(rangeActionId, "min"));
        MPConstraint absoluteVariationConstraintPositive = solver.makeConstraint(initialRangeActionValue, infinity(), getAboluteRangeActionVariationConstraintId(rangeActionId, "max"));

        absoluteVariationConstraintNegative.setCoefficient(absoluteVariationVariable, 1);
        absoluteVariationConstraintNegative.setCoefficient(valueVariable, -1);

        absoluteVariationConstraintPositive.setCoefficient(absoluteVariationVariable, 1);
        absoluteVariationConstraintPositive.setCoefficient(valueVariable, 1);

        rangeActionValueVariables.add(valueVariable);
        absoluteRangeActionVariationVariables.add(absoluteVariationVariable);
        absoluteRangeActionVariationConstraints.add(absoluteVariationConstraintNegative);
        absoluteRangeActionVariationConstraints.add(absoluteVariationConstraintPositive);
    }

    /**
     * Updates the cnec flow constraint with two coefficients related to the range action:
     * <ul>
     *     <li>one for the positive range action variable</li>
     *     <li>one for the negative range action variable</li>
     * </ul>
     * @param cnecId id of the cnec considered
     * @param rangeActionId if of the range action considered
     * @param sensitivity sensitivity coefficient of the flow on the cnec after a variation of the range action (TODO: clarify unit in the javadoc)
     */
    public void updateFlowConstraintsWithRangeAction(String cnecId, String rangeActionId, double sensitivity, double currentRangeActionValue) {
        MPConstraint flowConstraint = getFlowConstraint(cnecId);
        if (flowConstraint == null) {
            throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnecId));
        }
        MPVariable rangeActionValueVariable = getRangeActionValueVariable(rangeActionId);
        if (rangeActionValueVariable == null) {
            throw new FaraoException(String.format("Range action variable for %s has not been defined yet.", rangeActionId));
        }

        flowConstraint.setLb(flowConstraint.lb() - sensitivity * currentRangeActionValue);
        flowConstraint.setUb(flowConstraint.ub() - sensitivity * currentRangeActionValue);

        flowConstraint.setCoefficient(
                rangeActionValueVariable,
                -sensitivity);
    }

    public void addMinimumMarginVariable() {
        solver.makeNumVar(-infinity(), infinity(), POS_MIN_MARGIN);
    }

    /**
     * Adds two constraints to the minimum margin variable, related to a Cnec:
     * <ul>
     *     <li>one related to the minimal flow on the Cnec</li>
     *     <li>one related to the maximal flow on the Cnec</li>
     * </ul>
     * @param cnecId id of the Cnec considered
     * @param minFlow minimal flow on the Cnec considered
     * @param maxFlow maximal flow on the Cnec considered
     */
    public void addMinimumMarginConstraints(String cnecId, double minFlow, double maxFlow) {
        MPVariable flowVariable = getFlowVariable(cnecId);
        MPConstraint minimumMarginByMax = solver.makeConstraint(-infinity(), maxFlow, getMinimumMarginConstraintId(cnecId, "max"));
        minimumMarginByMax.setCoefficient(getMinimumMarginVariable(), 1);
        minimumMarginByMax.setCoefficient(flowVariable, 1);
        minimumMarginConstraints.add(minimumMarginByMax);
        MPConstraint minimumMarginByMin = solver.makeConstraint(-infinity(), -minFlow, getMinimumMarginConstraintId(cnecId, "min"));
        minimumMarginByMin.setCoefficient(getMinimumMarginVariable(), 1);
        minimumMarginByMin.setCoefficient(flowVariable, -1);
        minimumMarginConstraints.add(minimumMarginByMin);
    }

    /**
     * Creates the objective function which maximizes the minimum margin variable taking into account
     * penalty costs associated to the use of range actions.
     */
    public void addPosMinObjective() {
        objective = solver.objective();
        objective.setCoefficient(getMinimumMarginVariable(), 1);
        getAboluteRangeActionVariationVariables().forEach(negativeRangeActionVariable -> objective.setCoefficient(negativeRangeActionVariable, -PENALTY_COST));
        objective.setMaximization();
    }

    private String getFlowVariableId(String cnecId) {
        return String.format("%s-variable", cnecId);
    }

    private String getFlowConstraintId(String cnecId) {
        return String.format("%s-constraint", cnecId);
    }

    private String getRangeActionValueVariableId(String rangeActionId) {
        return String.format("%s-value-variable", rangeActionId);
    }

    /**
     * @param cnecId id of the considered cnec
     * @param minMax can take two values: "min" or "max" depending on the expected MPConstraint
     * @return the id of the MPConstraint ensuring that the minimal margin is smaller than the
     * minimum or maximum margin of the considered cnec.
     */
    private String getMinimumMarginConstraintId(String cnecId, String minMax) {
        return String.format("%s-%s-%s", POS_MIN_MARGIN, cnecId, minMax);
    }

    public String getAboluteRangeActionVariationVariableId(String rangeActionId) {
        return String.format("%s-absolute-variation-variable", rangeActionId);
    }

    private String getAboluteRangeActionVariationConstraintId(String rangeActionId, String minMax) {
        return String.format("%s-%s-absolute-variation-constraint", rangeActionId, minMax);
    }

    public void updateReferenceFlow(String cnecId, double referenceFlow) {
        getFlowConstraint(cnecId).setBounds(referenceFlow, referenceFlow);
    }

    public Enum solve() {
        return solver.solve();
    }
}
