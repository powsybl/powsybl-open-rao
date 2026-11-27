/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.mathopt.Variable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPVariable {
    private final OpenRaoMPSolver solver;
    private final Variable variable;

    protected OpenRaoMPVariable(OpenRaoMPSolver solver, Variable variable) {
        this.solver = solver;
        this.variable = variable;
    }

    public String name() {
        return variable.getName();
    }

    public double lb() {
        return variable.getLowerBound();
    }

    public double ub() {
        return variable.getUpperBound();
    }

    public void setLb(double lb) {
        variable.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
    }

    public void setUb(double ub) {
        variable.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    public void setBounds(double lb, double ub) {
        variable.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
        variable.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    Variable getVariable() {
        return variable;
    }

    public double solutionValue() {
        return solver.solutionValueOf(variable);
    }
}
