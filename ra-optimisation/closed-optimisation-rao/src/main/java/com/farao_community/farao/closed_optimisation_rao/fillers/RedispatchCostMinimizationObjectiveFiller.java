/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.TOTAL_REDISPATCH_COST;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class RedispatchCostMinimizationObjectiveFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedispatchCostMinimizationObjectiveFiller.class);

    @Override
    public List<String> objectiveFunctionsProvided() {
        return Collections.singletonList("total_redispatch_cost_minimization");
    }

    @Override
    public List<String> variablesExpected() {
        return Collections.singletonList(TOTAL_REDISPATCH_COST);
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        MPVariable totalRedispatchCost = Objects.requireNonNull(solver.lookupVariableOrNull(TOTAL_REDISPATCH_COST));
        MPObjective objective = solver.objective();
        objective.setCoefficient(totalRedispatchCost, 1);
        /*
         objective.setMinimization() is now set by MinimizationObjectiveFiller,
         it has yet been kept in this filler to ensure the non-regression of
         "old" configs which did not use the MinimizationObjectiveFiller.
         objective.setMinimization() is not disturbing in this filler as long as there is no
         need for an optimisation problem which aims at maximizing the redispatching costs
         */
        objective.setMinimization();
    }
}
