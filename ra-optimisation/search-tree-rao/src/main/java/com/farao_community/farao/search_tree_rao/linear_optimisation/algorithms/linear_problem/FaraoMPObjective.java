/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.google.ortools.linearsolver.MPObjective;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPObjective {
    private final MPObjective mpObjective;
    private final int numberOfBitsToRoundOff;

    protected FaraoMPObjective(MPObjective mpObjective, int numberOfBitsToRoundOff) {
        this.mpObjective = mpObjective;
        this.numberOfBitsToRoundOff = numberOfBitsToRoundOff;
    }

    public double getCoefficient(FaraoMPVariable variable) {
        return mpObjective.getCoefficient(variable.getMPVariable());
    }

    public void setCoefficient(FaraoMPVariable variable, double coeff) {
        mpObjective.setCoefficient(variable.getMPVariable(), RaoUtil.roundDouble(coeff, numberOfBitsToRoundOff));
    }

    public boolean minimization() {
        return mpObjective.minimization();
    }

    public void setMinimization() {
        mpObjective.setMinimization();
    }

    public boolean maximization() {
        return mpObjective.maximization();
    }

    public void setMaximization() {
        mpObjective.setMaximization();
    }
}
