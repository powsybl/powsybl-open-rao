/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.mocks;

import com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.FaraoMPObjective;
import com.google.ortools.linearsolver.MPVariable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class MPObjectiveMock extends FaraoMPObjective {

    private Map<String, Double> coefficients;
    private boolean isMinimization;
    private boolean isMaximization;

    protected MPObjectiveMock() {
        super(0, true, 0);
        coefficients = new HashMap<>();
        isMinimization = false;
        isMaximization = false;
    }

    @Override
    public void clear() {
        coefficients.clear();
    }

    @Override
    public void setCoefficient(MPVariable var, double coeff) {
        coefficients.put(var.name(), coeff);
    }

    @Override
    public double getCoefficient(MPVariable var) {
        return coefficients.get(var.name());
    }

    @Override
    public void setMinimization() {
        isMinimization = true;
    }

    @Override
    public void setMaximization() {
        isMaximization = true;
    }

    @Override
    public boolean maximization() {
        return isMaximization;
    }

    @Override
    public boolean minimization() {
        return isMinimization;
    }

}
