/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.openrao.searchtreerao.commons.RaoUtil;
import com.google.ortools.linearsolver.MPConstraint;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPConstraint {
    // TODO: test this class

    private final MPConstraint mpConstraint;
    private final int numberOfBitsToRoundOff;

    protected OpenRaoMPConstraint(MPConstraint mpConstraint, int numberOfBitsToRoundOff) {
        this.mpConstraint = mpConstraint;
        this.numberOfBitsToRoundOff = numberOfBitsToRoundOff;
    }

    public String name() {
        return mpConstraint.name();
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return mpConstraint.getCoefficient(variable.getMPVariable());
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        mpConstraint.setCoefficient(variable.getMPVariable(), RaoUtil.roundDouble(coeff, numberOfBitsToRoundOff));
    }

    public double lb() {
        return mpConstraint.lb();
    }

    public double ub() {
        return mpConstraint.ub();
    }

    public void setLb(double lb) {
        mpConstraint.setLb(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff));
    }

    public void setUb(double ub) {
        mpConstraint.setUb(RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }

    public void setBounds(double lb, double ub) {
        mpConstraint.setBounds(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff), RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }

    void remove(MPSolver mpSolver) {
        // It is currently impossible to remove a constraint from an OR-Tools MPSolver
        // mpConstraint.delete(); does not remove the variable
        // Only (almost) equivalent workaround would be to force it to zero in order for it to be ignored
        setBounds(-LinearProblem.infinity(), LinearProblem.infinity());
        for (MPVariable var : mpSolver.variables()) {
            mpConstraint.setCoefficient(var, 0.);
        }
    }
}
