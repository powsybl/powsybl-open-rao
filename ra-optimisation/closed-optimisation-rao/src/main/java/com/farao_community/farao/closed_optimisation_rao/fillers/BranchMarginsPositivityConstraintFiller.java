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

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.getAllMonitoredBranches;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class BranchMarginsPositivityConstraintFiller extends AbstractOptimisationProblemFiller {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchMarginsPositivityConstraintFiller.class);

    private List<MonitoredBranch> monitoredBranches;

    @Override
    public void initFiller(Network network, CracFile cracFile, Map<String, Object> data) {
        super.initFiller(network, cracFile, data);
        this.monitoredBranches = getAllMonitoredBranches(cracFile);
    }

    @Override
    public List<String> variablesExpected() {
        return monitoredBranches.stream()
                .map(ClosedOptimisationRaoNames::nameEstimatedFlowVariable).collect(Collectors.toList());
    }

    @Override
    public List<String> constraintsProvided() {
        List<String> flowConstraints = new ArrayList<>();
        monitoredBranches.forEach(branch -> {
            flowConstraints.add(namePositiveMaximumFlowConstraint(branch));
            flowConstraints.add(nameNegativeMaximumFlowConstraint(branch));
        });
        return flowConstraints;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());

        monitoredBranches.forEach(branch -> {
            buildMarginsPositivityConstraints(solver, branch);
        });
    }

    private void buildMarginsPositivityConstraints(MPSolver solver, MonitoredBranch branch) {
        double maximumFlow = branch.getFmax();

        MPVariable branchFlowVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameEstimatedFlowVariable(branch)));

        MPConstraint ubConstraint = solver.makeConstraint(namePositiveMaximumFlowConstraint(branch));
        MPConstraint lbConstraint = solver.makeConstraint(nameNegativeMaximumFlowConstraint(branch));

        ubConstraint.setUb(maximumFlow);
        ubConstraint.setCoefficient(branchFlowVariable, 1.0);

        lbConstraint.setLb(-maximumFlow);
        lbConstraint.setCoefficient(branchFlowVariable, 1.0);
    }
}
