/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.linearsolver.MPObjective;

import java.util.Locale;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPObjective {
    private final MPObjective mpObjective;

    protected OpenRaoMPObjective(MPObjective mpObjective) {
        this.mpObjective = mpObjective;
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return mpObjective.getCoefficient(variable.getMPVariable());
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        mpObjective.setCoefficient(variable.getMPVariable(), OpenRaoMPSolver.roundDouble(coeff));
        String var = String.format(Locale.ENGLISH, "solver.LookupVariableOrNull(\"%s\")", variable.getMPVariable().name());
        System.out.println(String.format(Locale.ENGLISH, "solver.MutableObjective()->SetCoefficient(%s, %.6f);", var, OpenRaoMPSolver.roundDouble(coeff)));
    }
}
