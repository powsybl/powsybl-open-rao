/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.post_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPostProcessor;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.ra_optimisation.ContingencyResult;
import com.farao_community.farao.ra_optimisation.MonitoredBranchResult;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.google.auto.service.AutoService;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPostProcessor.class)
public class BranchResultsPostProcessor implements OptimisationPostProcessor {
    private static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";
    private static final String ESTIMATED_FLOW_POSTFIX = "_estimated_flow";

    @Override
    public Map<String, Class> dataNeeded() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(REFERENCE_FLOWS_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public void fillResults(Network network, CracFile cracFile, MPSolver solver, Map<String, Object> data, RaoComputationResult result) {
        Map<String, Double> referenceFlows = (Map<String, Double>) data.get(REFERENCE_FLOWS_DATA_NAME);

        List<MonitoredBranchResult> branchResultList = cracFile.getPreContingency().getMonitoredBranches().stream()
            .map(branch -> {
                MPVariable branchFlowVariable = Objects.requireNonNull(solver.lookupVariableOrNull(branch.getId() + ESTIMATED_FLOW_POSTFIX));
                return new MonitoredBranchResult(
                    branch.getId(),
                    branch.getName(),
                    branch.getBranchId(),
                    branch.getFmax(),
                    referenceFlows.get(branch.getId()),
                    branchFlowVariable.solutionValue()
                );
            })
            .collect(Collectors.toList());
        result.getPreContingencyResult().getMonitoredBranchResults().addAll(branchResultList);

        cracFile.getContingencies().stream()
            .forEach(contingency -> {
                ContingencyResult contingencyResult = new ContingencyResult(
                    contingency.getId(),
                    contingency.getName()
                );
                List<MonitoredBranchResult> contingencyBranchesResultList = contingency.getMonitoredBranches().stream()
                    .map(branch -> {
                        MPVariable branchFlowVariable = Objects.requireNonNull(solver.lookupVariableOrNull(branch.getId() + ESTIMATED_FLOW_POSTFIX));
                        return new MonitoredBranchResult(
                            branch.getId(),
                            branch.getName(),
                            branch.getBranchId(),
                            branch.getFmax(),
                            referenceFlows.get(branch.getId()),
                            branchFlowVariable.solutionValue()
                        );
                    })
                    .collect(Collectors.toList());
                contingencyResult.getMonitoredBranchResults().addAll(contingencyBranchesResultList);
                result.getContingencyResults().add(contingencyResult);
            });
    }
}
