/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.mocks;

import com.google.ortools.linearsolver.MPVariable;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MPVariableMock extends MPVariable {

    private String name;
    private double lb;
    private double ub;
    private boolean isBoolVariable;
    private double solutionValue;

    protected MPVariableMock() {
        super(0, false);
    }

    protected MPVariableMock(String pName, double pLb, double pUb, boolean pIsBoolVariable) {
        super(0, false);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
        this.isBoolVariable = pIsBoolVariable;
    }

    @Override
    public double lb() {
        return lb;
    }

    @Override
    public double ub() {
        return ub;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public double solutionValue() {
        return solutionValue;
    }

    public void setRandomSolutionValue() {
        if (isBoolVariable) {
            this.solutionValue = 1;
        } else {
            this.solutionValue = Math.random() * (ub - lb) + lb;
        }
    }
}
