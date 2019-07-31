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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.closed_optimisation_rao.ClosedOptimisationRaoNames.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(AbstractOptimisationProblemFiller.class)
public class BranchMarginsVariablesFiller extends AbstractOptimisationProblemFiller {

    @Override
    public Map<String, Class> dataExpected() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(REFERENCE_FLOWS_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public List<String> variablesProvided() {
        List<String> returnList = new ArrayList<>();
        returnList.addAll(cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> (branch.getId())).collect(Collectors.toList()));
        returnList.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .map(branch -> nameEstimatedFlowVariable(branch.getId())).collect(Collectors.toList()));
        return returnList;
    }

    @Override
    public List<String> constraintsProvided() {
        List<String> returnList = new ArrayList<>();
        returnList.addAll(cracFile.getPreContingency().getMonitoredBranches().stream()
                .map(branch -> nameEstimatedFlowConstraint(branch.getId())).collect(Collectors.toList()));
        returnList.addAll(cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .map(branch -> nameEstimatedFlowConstraint(branch.getId())).collect(Collectors.toList()));
        return returnList;
    }

    @Override
    public void fillProblem(MPSolver solver) {
        //TODO : find a way to use solver.infinity() which work with the tests
        double infinity = Double.POSITIVE_INFINITY;
        Map<String, Double> referenceFlows = (Map<String, Double>) data.get(REFERENCE_FLOWS_DATA_NAME);

        cracFile.getPreContingency().getMonitoredBranches().forEach(branch -> {
            MPVariable branchFlowVariable = solver.makeNumVar(-infinity, infinity, nameEstimatedFlowVariable(branch.getId()));
            double referenceFlow = referenceFlows.get(branch.getId());
            MPConstraint branchFlowEquation = solver.makeConstraint(referenceFlow, referenceFlow, nameEstimatedFlowConstraint(branch.getId()));
            branchFlowEquation.setCoefficient(branchFlowVariable, 1);
        });

        cracFile.getContingencies().stream()
                .flatMap(contingency -> contingency.getMonitoredBranches().stream())
                .forEach(branch -> {
                    MPVariable branchFlowVariable = solver.makeNumVar(-infinity, infinity, nameEstimatedFlowVariable(branch.getId()));
                    double referenceFlow = referenceFlows.get(branch.getId());
                    MPConstraint branchFlowEquation = solver.makeConstraint(referenceFlow, referenceFlow, nameEstimatedFlowConstraint(branch.getId()));
                    branchFlowEquation.setCoefficient(branchFlowVariable, 1);
                });
    }
}
