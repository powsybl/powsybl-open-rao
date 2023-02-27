/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.algorithms.linear_problem;

import com.farao_community.farao.search_tree_rao.commons.RaoUtil;
import com.google.ortools.linearsolver.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-international.com>}
 */
public class FaraoMPSolver extends MPSolver {

    private static final int NUMBER_OF_BITS_TO_ROUND_OFF = 30;
    Map<String, MPConstraint> constraints = new HashMap<>();
    Map<String, MPVariable> variables = new HashMap<>();

    public FaraoMPSolver(long cptr, boolean cMemoryOwn) {
        super(cptr, cMemoryOwn);
    }

    public FaraoMPSolver(String name, OptimizationProblemType problemType) {
        super(name, problemType);
    }

    public MPConstraint getConstraint(String name) {
        return constraints.get(name);
    }

    public MPVariable getVariable(String name) {
        return variables.get(name);
    }

    public MPObjective getObjective() {
        return super.objective();
    }

    @Override
    public MPVariable makeNumVar(double lb, double ub, String name) {
        MPVariable mpVariable = super.makeNumVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        variables.put(name, mpVariable);
        return mpVariable;
    }

    @Override
    public MPVariable makeIntVar(double lb, double ub, String name) {
        MPVariable mpVariable = super.makeIntVar(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        variables.put(name, mpVariable);
        return mpVariable;
    }

    @Override
    public MPVariable makeBoolVar(String name) {
        MPVariable mpVariable = super.makeBoolVar(name);
        variables.put(name, mpVariable);
        return mpVariable;
    }

    @Override
    public MPConstraint makeConstraint(double lb, double ub, String name) {
        MPConstraint mpConstraint = super.makeConstraint(RaoUtil.roundDouble(lb, NUMBER_OF_BITS_TO_ROUND_OFF), RaoUtil.roundDouble(ub, NUMBER_OF_BITS_TO_ROUND_OFF), name);
        constraints.put(name, mpConstraint);
        return mpConstraint;
    }

    @Override
    public MPConstraint makeConstraint(String name) {
        MPConstraint mpConstraint = super.makeConstraint(name);
        constraints.put(name, mpConstraint);
        return mpConstraint;
    }
}
