/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoInput;
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
    public CompletableFuture<RaoResult> run(Network network, Crac crac, String variantId, ComputationManager computationManager, RaoParameters parameters) {
        RaoInput.synchronize(crac, network);
        RaoInput.cleanCrac(crac, network);

        // quality check
        List<String> configQualityCheck = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }

        // compute maximum loop flow value F_(0,all)_MAX, and update it for each Cnec in Crac
        CracLoopFlowExtension cracLoopFlowExtension = crac.getExtension(CracLoopFlowExtension.class);
        if (useLoopFlowExtension(parameters) && !Objects.isNull(cracLoopFlowExtension)) {
            //For the initial Network, compute the F_(0,all)_init
            LoopFlowComputation initialLoopFlowComputation = new LoopFlowComputation(crac, cracLoopFlowExtension);
            Map<String, Double> loopFlows = initialLoopFlowComputation.calculateLoopFlows(network);
            updateCnecsLoopFlowConstraint(crac, loopFlows); //todo: cnec loop flow extension need to be based on ResultVariantManger
        }

        // run optimisation
        RaoResult result = Tree.search(network, crac, variantId, parameters).join();
        return CompletableFuture.completedFuture(result);
    }

    public void updateCnecsLoopFlowConstraint(Crac crac, Map<String, Double> fZeroAll) {
        // For each Cnec, get the maximum F_(0,all)_MAX = Math.max(F_(0,all)_init, loop flow threshold
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            if (!Objects.isNull(cnecLoopFlowExtension)) {
                //!!! note here we use the result of branch flow of preventive state for all cnec of all states
                //this could be ameliorated by re-calculating loopflow for each cnec in curative state: [network + cnec's contingencies + current applied remedial actions]
                double initialLoopFlow = fZeroAll.get(cnec.getNetworkElement().getId());
                double loopFlowThreshold = cnecLoopFlowExtension.getInputLoopFlow();
                cnecLoopFlowExtension.setLoopFlowConstraint(Math.max(initialLoopFlow, loopFlowThreshold)); //todo: cnec loop flow extension need to be based on ResultVariantManger
            }
        });
    }

    private static boolean useLoopFlowExtension(RaoParameters parameters) {
        return parameters.isRaoWithLoopFlowLimitation();
    }
}
