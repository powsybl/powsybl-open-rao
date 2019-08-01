/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.fillers;

import com.farao_community.farao.closed_optimisation_rao.AbstractOptimisationProblemFiller;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.nameEstimatedFlowVariable;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class BranchMarginsPositivityConstraintFiller extends AbstractOptimisationProblemFiller {

    @Override
    public List<String> variablesExpected() {
        List<String> returnList = new ArrayList<>();
        returnList.addAll(cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> nameEstimatedFlowVariable(branch.getId())).collect(Collectors.toList()));
        returnList.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .map(branch -> nameEstimatedFlowVariable(branch.getId())).collect(Collectors.toList()));
        return returnList;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> {
            MPVariable branchFlowVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameEstimatedFlowVariable(branch.getId())));
            double maximumFlow = branch.getFmax();
            MPConstraint branchMarginPositivityConstraint = solver.makeConstraint(-maximumFlow, maximumFlow);
            branchMarginPositivityConstraint.setCoefficient(branchFlowVariable, 1);
        });

        cracFile.getContingencies().stream()
            .flatMap(contingency -> contingency.getMonitoredBranches().stream())
            .forEach(branch -> {
                MPVariable branchFlowVariable = Objects.requireNonNull(solver.lookupVariableOrNull(nameEstimatedFlowVariable(branch.getId())));
                double maximumFlow = branch.getFmax();
                MPConstraint branchMarginPositivityConstraint = solver.makeConstraint(-maximumFlow, maximumFlow);
                branchMarginPositivityConstraint.setCoefficient(branchFlowVariable, 1);
            });
    }
}
