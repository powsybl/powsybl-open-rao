/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
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
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.getAllMonitoredBranches;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class BranchMarginsVariablesFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchMarginsVariablesFiller.class);
    private List<MonitoredBranch> monitoredBranches;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.monitoredBranches = getAllMonitoredBranches(cracFile);
    }

    @Override
    public Map<String, Class> dataExpected() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(REFERENCE_FLOWS_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public List<String> variablesProvided() {
        return monitoredBranches.stream()
                .map(ClosedOptimisationRaoNames::nameEstimatedFlowVariable).collect(Collectors.toList());
    }

    @Override
    public List<String> constraintsProvided() {
        return monitoredBranches.stream()
                .map(ClosedOptimisationRaoNames::nameEstimatedFlowConstraint).collect(Collectors.toList());
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());
        double infinity = MPSolver.infinity();

        Map<String, Double> referenceFlows = (Map<String, Double>) data.get(REFERENCE_FLOWS_DATA_NAME);

        monitoredBranches.forEach(branch -> {
            MPVariable branchFlowVariable = solver.makeNumVar(-infinity, infinity, nameEstimatedFlowVariable(branch));
            double referenceFlow = referenceFlows.get(branch.getId());
            MPConstraint branchFlowEquation = solver.makeConstraint(referenceFlow, referenceFlow, nameEstimatedFlowConstraint(branch));
            branchFlowEquation.setCoefficient(branchFlowVariable, 1);
        });
    }
}
