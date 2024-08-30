/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.modelbuilder.LinearConstraint;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPConstraint {
    private final String name;
    private final LinearConstraint mpConstraint;
    private Map<OpenRaoMPVariable, Double> coefficients;

    protected OpenRaoMPConstraint(String name, LinearConstraint mpConstraint) {
        this.name = name;
        this.mpConstraint = mpConstraint;
        coefficients = new HashMap<>();
    }

    public String name() {
        return name;
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return coefficients.getOrDefault(variable, 0.);
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        coefficients.put(variable, coeff);
        mpConstraint.setCoefficient(variable.getMPVariable(), OpenRaoMPSolver.roundDouble(coeff));
    }

    public double lb() {
        return mpConstraint.getLowerBound();
    }

    public double ub() {
        return mpConstraint.getUpperBound();
    }

    public void setLb(double lb) {
        mpConstraint.setLowerBound(OpenRaoMPSolver.roundDouble(lb));
    }

    public void setUb(double ub) {
        mpConstraint.setUpperBound(OpenRaoMPSolver.roundDouble(ub));
    }

    public void setBounds(double lb, double ub) {
        mpConstraint.setLowerBound(lb);
        mpConstraint.setUpperBound(ub);
    }
}
