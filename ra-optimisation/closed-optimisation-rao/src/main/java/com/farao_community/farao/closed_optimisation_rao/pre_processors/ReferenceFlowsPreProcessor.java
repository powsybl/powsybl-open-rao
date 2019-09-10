/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.farao_community.farao.closed_optimisation_rao.OptimisationPreProcessor;
import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_file.Contingency;
import com.farao_community.farao.data.crac_file.ContingencyElement;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.MonitoredBranch;
import com.farao_community.farao.util.LoadFlowService;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(OptimisationPreProcessor.class)
public class ReferenceFlowsPreProcessor implements OptimisationPreProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceFlowsPreProcessor.class);
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

        LOGGER.info("Running pre contingency loadflow");
        runLoadFlow(
                network,
                cracFile.getPreContingency().getMonitoredBranches(),
                referenceFlows
        );

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        try (FaraoVariantsPool variantsPool = new FaraoVariantsPool(network, initialVariantId)) {
            variantsPool.submit(() -> cracFile.getContingencies().parallelStream().forEach(contingency -> {
                // Create contingency variant
                try {
                    LOGGER.info("Running post contingency loadflow for contingency'{}'", contingency.getId());
                    String workingVariant = variantsPool.getAvailableVariant();
                    network.getVariantManager().setWorkingVariant(workingVariant);
                    applyContingency(network, computationManager, contingency);

                    runLoadFlow(
                            network,
                            contingency.getMonitoredBranches(),
                            referenceFlows
                    );
                    variantsPool.releaseUsedVariant(workingVariant);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            })).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new FaraoException(e);
        }

        network.getVariantManager().setWorkingVariant(initialVariantId);

        data.put(REFERENCE_FLOWS_DATA_NAME, referenceFlows);
    }

    private void runLoadFlow(
            Network network,
            List<MonitoredBranch> monitoredBranches,
            Map<String, Double> referenceFlows) {

        LoadFlowResult results = LoadFlowService.runLoadFlow(network, network.getVariantManager().getWorkingVariantId());

        if (!results.isOk()) {
            throw new FaraoException("Divergence in loadflow computation");
        }

        monitoredBranches.forEach(branch -> {
            double flow = network.getBranch(branch.getBranchId()).getTerminal1().getP();
            referenceFlows.put(branch.getId(), Double.isNaN(flow) ? 0. : flow);
        });
    }

    private void applyContingency(Network network, ComputationManager computationManager, Contingency contingency) {
        contingency.getContingencyElements().forEach(contingencyElement -> applyContingencyElement(network, computationManager, contingencyElement));
    }

    private void applyContingencyElement(Network network, ComputationManager computationManager, ContingencyElement contingencyElement) {
        Identifiable element = network.getIdentifiable(contingencyElement.getElementId());
        if (element instanceof Branch) {
            BranchContingency contingency = new BranchContingency(contingencyElement.getElementId());
            contingency.toTask().modify(network, computationManager);
        } else if (element instanceof Switch) {
            // TODO: convert into a PowSyBl ContingencyElement ?
            Switch switchElement = (Switch) element;
            switchElement.setOpen(true);
        } else {
            throw new FaraoException("Unable to apply contingency element " + contingencyElement.getElementId());
        }
    }
}
