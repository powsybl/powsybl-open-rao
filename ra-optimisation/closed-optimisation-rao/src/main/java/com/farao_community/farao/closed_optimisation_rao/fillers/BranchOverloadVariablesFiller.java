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
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;
import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoUtil.getAllMonitoredBranches;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class BranchOverloadVariablesFiller extends AbstractOptimisationProblemFiller {

    private static final Logger LOGGER = LoggerFactory.getLogger(BranchOverloadVariablesFiller.class);
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
    public List<String> constraintsExpected() {
        List<String> flowConstraints = new ArrayList<>();
        monitoredBranches.forEach(branch -> {
            flowConstraints.add(namePositiveMaximumFlowConstraint(branch));
            flowConstraints.add(nameNegativeMaximumFlowConstraint(branch));
        });
        return flowConstraints;
    }

    @Override
    public List<String> variablesProvided() {
        return monitoredBranches.stream()
                .map(ClosedOptimisationRaoNames::nameOverloadVariable).collect(Collectors.toList());
    }

    @Override
    public void fillProblem(MPSolver solver) {
        LOGGER.info("Filling problem using plugin '{}'", getClass().getSimpleName());

        monitoredBranches.forEach(branch -> {
            addOverloadVariable(solver, branch);
        });
    }

    private void addOverloadVariable(MPSolver solver, MonitoredBranch branch) {
        double infinity = MPSolver.infinity();

        MPVariable overloadVariable = solver.makeNumVar(0, infinity, nameOverloadVariable(branch));

        MPConstraint ubConstraint = Objects.requireNonNull(solver.lookupConstraintOrNull(namePositiveMaximumFlowConstraint(branch)));
        MPConstraint lbConstraint = Objects.requireNonNull(solver.lookupConstraintOrNull(nameNegativeMaximumFlowConstraint(branch)));

        ubConstraint.setCoefficient(overloadVariable, -1.0);
        lbConstraint.setCoefficient(overloadVariable, 1.0);
    }

}
