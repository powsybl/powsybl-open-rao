/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.linear_rao;

import com.farao_community.farao.commons.FaraoException;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class LinearRaoProblem extends MPSolver {

    static {
        System.loadLibrary("jniortools");
    }

    protected LinearRaoProblem() {
        super("linear rao", OptimizationProblemType.CBC_MIXED_INTEGER_PROGRAMMING);
    }

    public void addCnec(String cnecId, double referenceFlow, double minFlow, double maxFlow) {
        MPVariable flowVariable = makeNumVar(minFlow, maxFlow, getFlowVariableId(cnecId));
        MPConstraint flowConstraint = makeConstraint(referenceFlow, referenceFlow, getFlowConstraintId(cnecId));
        flowConstraint.setCoefficient(flowVariable, 1);
    }

    private static String getFlowVariableId(String cnecId) {
        return String.format("%s-variable", cnecId);
    }

    private static String getFlowConstraintId(String cnecId) {
        return String.format("%s-constraint", cnecId);
    }

    public void addRangeActionVariable(String rangeActionId, String networkElementId, double maxNegativeVariation, double maxPositiveVariation) {
        makeNumVar(0, maxNegativeVariation, getNegativeRangeActionVariable(rangeActionId, networkElementId));
        makeNumVar(0, maxPositiveVariation, getPositiveRangeActionVariable(rangeActionId, networkElementId));
    }

    private static String getPositiveRangeActionVariable(String rangeActionId, String networkElementId) {
        return String.format("positive-%s-%s-variable", rangeActionId, networkElementId);
    }

    private static String getNegativeRangeActionVariable(String rangeActionId, String networkElementId) {
        return String.format("negative-%s-%s-variable", rangeActionId, networkElementId);
    }

    public void addRangeActionFlowOnBranch(String cnecId, String rangeActionId, String networkElementId, double sensitivity) {
        MPConstraint flowConstraint = lookupConstraintOrNull(getFlowConstraintId(cnecId));
        if (flowConstraint == null) {
            throw new FaraoException(String.format("Flow variable on %s has not been defined yet.", cnecId));
        }
        MPVariable positiveRangeActionVariable = lookupVariableOrNull(getPositiveRangeActionVariable(rangeActionId, networkElementId));
        MPVariable negativeRangeActionVariable = lookupVariableOrNull(getNegativeRangeActionVariable(rangeActionId, networkElementId));
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
}
