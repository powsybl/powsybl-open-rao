/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.powsybl.open_rao.search_tree_rao.commons.RaoUtil;
import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class OpenRaoMPVariable {
    private final MPVariable mpVariable;
    private final int numberOfBitsToRoundOff;

    protected OpenRaoMPVariable(MPVariable mpVariable, int numberOfBitsToRoundOff) {
        this.mpVariable = mpVariable;
        this.numberOfBitsToRoundOff = numberOfBitsToRoundOff;
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
        mpVariable.setLb(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff));
    }

    public void setUb(double ub) {
        mpVariable.setUb(RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }

    public void setBounds(double lb, double ub) {
        mpVariable.setBounds(RaoUtil.roundDouble(lb, numberOfBitsToRoundOff), RaoUtil.roundDouble(ub, numberOfBitsToRoundOff));
    }

    MPVariable getMPVariable() {
        return mpVariable;
    }

    public double solutionValue() {
        return mpVariable.solutionValue();
    }
}
