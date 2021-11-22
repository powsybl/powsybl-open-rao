/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation;

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.linearsolver.mainJNI;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPSolver extends MPSolver {

    private static final double PRECISION = 1048576; //2^20

    public FaraoMPSolver(String name, OptimizationProblemType problemType) {
        super(name, problemType);
    }

    @Override
    public MPVariable makeNumVar(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeNumVar(getCPtr(this), this, lb, ub, name);
        return cPtr == 0L ? null : new FaraoMPVariable(cPtr, false, PRECISION);
    }

    @Override
    public MPVariable makeIntVar(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeIntVar(getCPtr(this), this, lb, ub, name);
        return cPtr == 0L ? null : new FaraoMPVariable(cPtr, false, PRECISION);
    }

    @Override
    public MPConstraint makeConstraint(double lb, double ub, String name) {
        long cPtr = mainJNI.MPSolver_makeConstraint__SWIG_2(getCPtr(this), this, lb, ub, name);
        return cPtr == 0L ? null : new FaraoMPConstraint(cPtr, false, PRECISION);
    }
}
