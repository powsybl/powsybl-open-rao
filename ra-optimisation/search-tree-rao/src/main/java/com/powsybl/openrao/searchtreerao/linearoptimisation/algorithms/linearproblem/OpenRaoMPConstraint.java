/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPConstraint;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPConstraint {
    private final MPConstraint mpConstraint;

    protected OpenRaoMPConstraint(MPConstraint mpConstraint) {
        this.mpConstraint = mpConstraint;
    }

    public String name() {
        return mpConstraint.name();
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return mpConstraint.getCoefficient(variable.getMPVariable());
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        mpConstraint.setCoefficient(variable.getMPVariable(), OpenRaoMPSolver.roundDouble(coeff));
    }

    public double lb() {
        return mpConstraint.lb();
    }

    public double ub() {
        return mpConstraint.ub();
    }

    public void setLb(double lb) {
        mpConstraint.setLb(OpenRaoMPSolver.roundDouble(lb));
    }

    public void setUb(double ub) {
        mpConstraint.setUb(OpenRaoMPSolver.roundDouble(ub));
    }

    public void setBounds(double lb, double ub) {
        mpConstraint.setBounds(OpenRaoMPSolver.roundDouble(lb), OpenRaoMPSolver.roundDouble(ub));
    }

    public boolean isLazy() {
        return mpConstraint.isLazy();
    }

    public void setIsLazy(boolean isLazy) {
        mpConstraint.setIsLazy(isLazy);
    }
}
