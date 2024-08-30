/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.modelbuilder.Variable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPVariable {
    private final Variable mpVariable;

    protected OpenRaoMPVariable(Variable mpVariable) {
        this.mpVariable = mpVariable;
    }

    public String name() {
        return mpVariable.getName();
    }

    public double lb() {
        return mpVariable.getLowerBound();
    }

    public double ub() {
        return mpVariable.getUpperBound();
    }

    public void setLb(double lb) {
        mpVariable.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
    }

    public void setUb(double ub) {
        mpVariable.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    public void setBounds(double lb, double ub) {
        setLb(lb);
        setUb(ub);
    }

    Variable getMPVariable() {
        return mpVariable;
    }

    void setSolutionValue(double value) {
        solutionValue = value;
    }

    private double solutionValue;

    public double solutionValue() {
        return solutionValue;
    }
}
