/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_commons.RaoUtil;
import com.google.ortools.linearsolver.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPSolver extends MPSolver {

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    Map<String, FaraoMPConstraint> constraints = new HashMap<>();
    Map<String, FaraoMPVariable> variables = new HashMap<>();
    FaraoMPObjective objective;

    public FaraoMPSolver(long cptr, boolean cMemoryOwn) {
        super(cptr, cMemoryOwn);
    }

    public FaraoMPSolver(String name, OptimizationProblemType problemType) {
        super(name, problemType);
    }

    public FaraoMPConstraint getConstraint(String name) {
        return constraints.get(name);
    }

    public FaraoMPVariable getVariable(String name) {
        return variables.get(name);
    }

    public FaraoMPObjective getObjective() {
        return objective;
    }

    @Override
    public MPObjective objective() {
        long cPtr = mainJNI.MPSolver_objective(getCPtr(this), this);
        objective = cPtr == 0L ? null : new FaraoMPObjective(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        return objective;
    }

    @Override
    public MPVariable makeNumVar(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeNumVar(getCPtr(this), this, RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        FaraoMPVariable v = cPtr == 0L ? null : new FaraoMPVariable(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        variables.put(name, v);
        return v;
    }

    @Override
    public MPVariable makeIntVar(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeIntVar(getCPtr(this), this, RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        FaraoMPVariable v = cPtr == 0L ? null : new FaraoMPVariable(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        variables.put(name, v);
        return v;
    }

    @Override
    public MPVariable makeBoolVar(String name) {
        long cPtr = mainJNI.MPSolver_makeBoolVar(getCPtr(this), this, name);
        FaraoMPVariable v = cPtr == 0L ? null : new FaraoMPVariable(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        variables.put(name, v);
        return v;
    }

    @Override
    public MPConstraint makeConstraint(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeConstraint__SWIG_2(getCPtr(this), this, RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        FaraoMPConstraint c = cPtr == 0L ? null : new FaraoMPConstraint(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        constraints.put(name, c);
        return c;
    }

    @Override
    public MPConstraint makeConstraint(String name) {
        long cPtr = mainJNI.MPSolver_makeConstraint__SWIG_3(getCPtr(this), this, name);
        FaraoMPConstraint c = cPtr == 0L ? null : new FaraoMPConstraint(cPtr, false, NUMBER_OF_BITS_TO_ROUND_OFF);
        constraints.put(name, c);
        return c;
    }
}
