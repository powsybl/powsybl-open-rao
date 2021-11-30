/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_commons.linear_optimisation.mocks;

import com.farao_community.farao.rao_commons.linear_optimisation.FaraoMPVariable;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MPVariableMock extends FaraoMPVariable {

    private String name;
    private double lb;
    private double ub;
    private boolean isIntVariable;
    private double solutionValue;

    protected MPVariableMock() {
        super(0, false, 0);
    }

    public MPVariableMock(String pName, double pLb, double pUb, boolean pIsIntVariable) {
        super(0, false, 0);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
        this.isIntVariable = pIsIntVariable;
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
    public void setLb(double lb) {
        this.lb = lb;
    }

    @Override
    public void setUb(double ub) {
        this.ub = ub;
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
        if (isIntVariable) {
            this.solutionValue = 1;
        } else {
            this.solutionValue = Math.random() * (ub - lb) + lb;
        }
    }
}
