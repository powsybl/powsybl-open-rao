/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.OPTIMISATION_CONSTANTS_DATA_NAME;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.OVERLOAD_PENALTY_COST_NAME;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.getAllMonitoredBranches;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class OverloadPenaltyCostFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(OverloadPenaltyCostFiller.class);
    private List<MonitoredBranch> monitoredBranches;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.monitoredBranches = getAllMonitoredBranches(cracFile);
    }

    @Override
    public List<String> variablesExpected() {
        return monitoredBranches.stream()
                .map(ClosedOptimisationRaoNames::nameOverloadVariable).collect(Collectors.toList());
    }

    @Override
    public Map<String, Class>  dataExpected() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(OPTIMISATION_CONSTANTS_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public List<String> objectiveFunctionsProvided() {
        return Collections.singletonList("overload_penalty_cost");
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        double overloadPenaltyCost = ((Map<String, Double>) data.get(OPTIMISATION_CONSTANTS_DATA_NAME)).get(OVERLOAD_PENALTY_COST_NAME);
        MPObjective objective = solver.objective();
        monitoredBranches.forEach(branch -> {
            MPVariable overload = Objects.requireNonNull(solver.lookupVariableOrNull(ClosedOptimisationRaoNames.nameOverloadVariable(branch)));
            objective.setCoefficient(overload, overloadPenaltyCost);
        });
    }
}
