/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.mathopt.Objective;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPObjective {
    private final Objective objective;

    protected OpenRaoMPObjective(Objective objective) {
        this.objective = objective;
    }

    public double getCoefficient(OpenRaoMPVariable variable) {
        return objective.getLinearTerm(variable.getVariable());
    }

    public void setCoefficient(OpenRaoMPVariable variable, double coeff) {
        objective.setLinearTerm(variable.getVariable(), OpenRaoMPSolver.roundDouble(coeff));
    }
}
