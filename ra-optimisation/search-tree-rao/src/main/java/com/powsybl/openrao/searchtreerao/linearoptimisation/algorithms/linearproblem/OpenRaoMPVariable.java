/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPVariable;

import java.util.Locale;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPVariable {
    private final MPVariable mpVariable;

    protected OpenRaoMPVariable(MPVariable mpVariable) {
        this.mpVariable = mpVariable;
    }

    public String name() {
        return mpVariable.name();
    }

    public double lb() {
        return mpVariable.lb();
    }

    public double ub() {
        return mpVariable.ub();
    }

    public void setLb(double lb) {
        mpVariable.setLb(OpenRaoMPSolver.roundDouble(lb));
        String var = String.format(Locale.ENGLISH, "solver.LookupVariableOrNull(\"%s\")", mpVariable.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetLB(%.6f);", var, OpenRaoMPSolver.roundDouble(lb)));
    }

    public void setUb(double ub) {
        mpVariable.setUb(OpenRaoMPSolver.roundDouble(ub));
        String var = String.format(Locale.ENGLISH, "solver.LookupVariableOrNull(\"%s\")", mpVariable.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetUB(%.6f);", var, OpenRaoMPSolver.roundDouble(ub)));
    }

    public void setBounds(double lb, double ub) {
        mpVariable.setBounds(OpenRaoMPSolver.roundDouble(lb), OpenRaoMPSolver.roundDouble(ub));
        String var = String.format(Locale.ENGLISH, "solver.LookupVariableOrNull(\"%s\")", mpVariable.name());
        System.out.println(String.format(Locale.ENGLISH, "%s->SetBounds(%.6f);", var, OpenRaoMPSolver.roundDouble(lb), OpenRaoMPSolver.roundDouble(ub)));
    }

    MPVariable getMPVariable() {
        return mpVariable;
    }

    public double solutionValue() {
        return mpVariable.solutionValue();
    }
}
