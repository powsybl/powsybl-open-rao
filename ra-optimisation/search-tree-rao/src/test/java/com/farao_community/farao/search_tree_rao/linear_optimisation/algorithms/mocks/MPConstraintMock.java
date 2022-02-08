/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.mocks;

import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.FaraoMPConstraint;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class MPConstraintMock extends FaraoMPConstraint {

    private String name;
    private double lb;
    private double ub;
    private Map<String, Double> coefficients;

    public MPConstraintMock(String pName, double pLb, double pUb) {
        super(0, false, 0);
        this.name = pName;
        this.lb = pLb;
        this.ub = pUb;
        this.coefficients = new HashMap<>();
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
    public void setCoefficient(MPVariable var, double coeff) {
        coefficients.put(var.name(), coeff);
    }

    @Override
    public void setUb(double ub) {
        this.ub = ub;
    }

    @Override
    public void setLb(double lb) {
        this.lb = lb;
    }

    @Override
    public void setBounds(double lb, double ub) {
        this.lb = lb;
        this.ub = ub;
    }

    @Override
    public double getCoefficient(MPVariable var) {
        return coefficients.getOrDefault(var.name(), 0.);
    }
}
