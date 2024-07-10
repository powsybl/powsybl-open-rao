/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPConstraint;

import java.util.Locale;

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
        String var = String.format(Locale.ENGLISH, "solver.LookupVariableOrNull(\"%s\")", variable.getMPVariable().name());
        String cons = String.format(Locale.ENGLISH, "solver.LookupConstraintOrNull(\"%s\")", mpConstraint.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetCoefficient(%s, %.6f);", cons, var, OpenRaoMPSolver.roundDouble(coeff)));
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
        String cons = String.format(Locale.ENGLISH, "solver.LookupConstraintOrNull(\"%s\")", mpConstraint.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetLB(%.6f);", cons, OpenRaoMPSolver.roundDouble(lb)));
    }

    public void setUb(double ub) {
        mpConstraint.setUb(OpenRaoMPSolver.roundDouble(ub));
        String cons = String.format(Locale.ENGLISH, "solver.LookupConstraintOrNull(\"%s\")", mpConstraint.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetUB(%.6f);", cons, OpenRaoMPSolver.roundDouble(ub)));
    }

    public void setBounds(double lb, double ub) {
        mpConstraint.setBounds(OpenRaoMPSolver.roundDouble(lb), OpenRaoMPSolver.roundDouble(ub));
        String cons = String.format(Locale.ENGLISH, "solver.LookupConstraintOrNull(\"%s\")", mpConstraint.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetBounds(%.6f, %.6f);", cons, OpenRaoMPSolver.roundDouble(lb), OpenRaoMPSolver.roundDouble(ub)));
    }
}
