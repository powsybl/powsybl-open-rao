/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.farao_community.farao.closed_optimisation_rao.LoadFlowService;
import com.farao_community.farao.closed_optimisation_rao.OptimisationPreProcessor;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPreProcessor.class)
public class ReferenceFlowsPreProcessor implements OptimisationPreProcessor {
    private static final String REFERENCE_FLOWS_DATA_NAME = "reference_flows";

    @Override
    public Map<String, Class> dataProvided() {
        Map<String, Class> returnMap = new HashMap<>();
        returnMap.put(REFERENCE_FLOWS_DATA_NAME, Map.class);
        return returnMap;
    }

    @Override
    public void fillData(Network network, CracFile cracFile, ComputationManager computationManager, Map<String, Object> data) {
        Map<String, Double> referenceFlows = new ConcurrentHashMap<>();

        // Pre-contingency load-flow
        runLoadFlow(
                network,
                cracFile.getPreContingency().getMonitoredBranches(),
                referenceFlows
        );


        // Post-contingency loadflow calculation
        // State creation and deletion not thread safe, out of parallel stream
        String initialStateId = network.getStateManager().getWorkingStateId();
        cracFile.getContingencies().forEach(contingency -> {

            // Create contingency state
            String contingencyStateId = initialStateId + "+" + contingency.getId();
            network.getStateManager().cloneState(initialStateId, contingencyStateId);
            network.getStateManager().setWorkingState(contingencyStateId);

            // Apply contingency
            applyContingency(network, computationManager, contingency);
        });

        cracFile.getContingencies().parallelStream().forEach(contingency -> {
            // Create contingency state
            String contingencyStateId = initialStateId + "+" + contingency.getId();
            network.getStateManager().setWorkingState(contingencyStateId);

            // Run sensitivity computation
            runLoadFlow(
                    network,
                    contingency.getMonitoredBranches(),
                    referenceFlows
            );
        });

        network.getStateManager().setWorkingState(initialStateId);

        cracFile.getContingencies().forEach(contingency -> {
            // Remove contingency state
            String contingencyStateId = initialStateId + "+" + contingency.getId();
            network.getStateManager().removeState(contingencyStateId);
        });

        data.put(REFERENCE_FLOWS_DATA_NAME, referenceFlows);
    }

    private void runLoadFlow(
            Network network,
            List<MonitoredBranch> monitoredBranches,
            Map<String, Double> referenceFlows) {

        LoadFlowResult results = LoadFlowService.runLoadFlow(network, network.getStateManager().getWorkingStateId());

        if (!results.isOk()) {
            throw new FaraoException("Divergence in loadflow computation");
        }

        monitoredBranches.forEach(branch -> referenceFlows.put(branch.getId(), network.getBranch(branch.getBranchId()).getTerminal1().getP()));
    }

    private void applyContingency(Network network, ComputationManager computationManager, Contingency contingency) {
        contingency.getContingencyElements().forEach(contingencyElement -> applyContingencyElement(network, computationManager, contingencyElement));
    }

    private void applyContingencyElement(Network network, ComputationManager computationManager, ContingencyElement contingencyElement) {
        if (contingencyElement instanceof Branch) {
            BranchContingency contingency = new BranchContingency(contingencyElement.getElementId());
            contingency.toTask().modify(network, computationManager);
        }
    }
}
