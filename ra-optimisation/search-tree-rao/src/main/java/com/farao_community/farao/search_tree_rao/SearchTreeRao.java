/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.flowbased_computation.FlowBasedComputationParameters;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowExtensionInCrac;
import com.farao_community.farao.flowbased_computation.impl.LoopFlowUtil;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.search_tree_rao.config.SearchTreeConfigurationUtil;
import com.farao_community.farao.search_tree_rao.process.search_tree.Tree;
import com.google.auto.service.AutoService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRao implements RaoProvider {
    @Override
    public String getName() {
        return "SearchTreeRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoComputationResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {

        // quality check
        List<String> configQualityCheck = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }

        // set Crac reference values
        crac.setReferenceValues(network);

        // compute maximum loop flow value F_(0,all)_MAX, and update it for each Cnec in Crac
        calculateLoopFlowConstraintAndUpdateAllCnec(network, crac, computationManager);

        // run optimisation
        RaoComputationResult result = Tree.search(network, crac, variantId, parameters).join();
        return CompletableFuture.completedFuture(result);
    }

    private void calculateLoopFlowConstraintAndUpdateAllCnec(Network network, Crac crac, ComputationManager computationManager) {
        // compute maximum loop flow value F_(0,all)_MAX, and update it for Cnec in Crac

        // 1. For the initial Network, compute the F_(0,all)_init
        LoopFlowExtensionInCrac loopFlowExtensionInCrac = crac.getExtension(LoopFlowExtensionInCrac.class);
        if (Objects.isNull(loopFlowExtensionInCrac)) {
            throw new FaraoException("LoopFlowExtensionInCrac not available");
        }
        FlowBasedComputationParameters flowBasedComputationParameters = FlowBasedComputationParameters.load();
        Map<String, Double> fZeroAll = LoopFlowUtil.calculateLoopFlows(network, crac, loopFlowExtensionInCrac.getGlskProvider(),
                loopFlowExtensionInCrac.getCountriesForLoopFlow(), computationManager, flowBasedComputationParameters);

        // 2. For each Cnec, get the maximum F_(0,all)_MAX = Math.max(F_(0,all)_init, loop flow threshold
        crac.getCnecs().forEach(cnec -> {
            double initialLoopFlow = fZeroAll.get(cnec.getNetworkElement().getId());
            double inputLoopFlow = cnec.getInputLoopFlow();
            cnec.setLoopFlowConstraint(Math.max(initialLoopFlow, inputLoopFlow));
        });
    }

}
