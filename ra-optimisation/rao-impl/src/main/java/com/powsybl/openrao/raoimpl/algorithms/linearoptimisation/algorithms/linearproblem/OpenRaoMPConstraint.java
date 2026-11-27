/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.mathopt.LinearConstraint;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPConstraint {
    private final LinearConstraint linearConstraint;

    protected OpenRaoMPConstraint(LinearConstraint linearConstraint) {
        this.linearConstraint = linearConstraint;
    }

    public String name() {
        return linearConstraint.getName();
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return linearConstraint.getTerm(variable.getVariable());
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        linearConstraint.setTerm(variable.getVariable(), OpenRaoMPSolver.roundDouble(coeff));
    }

    public double lb() {
        return linearConstraint.getLowerBound();
    }

    public double ub() {
        return linearConstraint.getUpperBound();
    }

    public void setLb(double lb) {
        linearConstraint.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
    }

    public void setUb(double ub) {
        linearConstraint.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    public void setBounds(double lb, double ub) {
        linearConstraint.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
        linearConstraint.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    LinearConstraint getLinearConstraint() {
        return linearConstraint;
    }
}
