/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.loopflow_computation.LoopFlowComputation;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoProvider;
import com.farao_community.farao.rao_api.RaoResult;
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
        // quality check
        List<String> configQualityCheck = SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(parameters);
        if (!configQualityCheck.isEmpty()) {
            throw new FaraoException("There are some issues in RAO parameters:" + System.lineSeparator() + String.join(System.lineSeparator(), configQualityCheck));
        }
        crac.generateValidityReport(network);

        if (useLoopFlowExtension(parameters) && !Objects.isNull(crac.getExtension(CracLoopFlowExtension.class))) {
            computeInitialLoopflowAndUpdateCnecLoopflowConstraint(network, crac, parameters);
        }

        // run optimisation
        RaoResult result = Tree.search(network, crac, variantId, parameters).join();
        return CompletableFuture.completedFuture(result);
    }

    public void computeInitialLoopflowAndUpdateCnecLoopflowConstraint(Network network, Crac crac, RaoParameters parameters) {
        LoopFlowComputation initialLoopFlowComputation = new LoopFlowComputation(crac);
        Map<Cnec, Double> frefResults = initialLoopFlowComputation.computeRefFlowOnCurrentNetwork(network); //get reference flow
        Map<Cnec, Double> loopFlowShifts = initialLoopFlowComputation.buildZeroBalanceFlowShift(network); //compute PTDF * NetPosition
        Map<String, Double> loopFlows = initialLoopFlowComputation.buildLoopFlowsFromReferenceFlowAndLoopflowShifts(frefResults, loopFlowShifts);
        updateCnecsLoopFlowConstraint(crac, loopFlows, loopFlowShifts);
    }

    public void updateCnecsLoopFlowConstraint(Crac crac, Map<String, Double> loopflows, Map<Cnec, Double> loopflowShifts) {
        crac.getCnecs(crac.getPreventiveState()).forEach(cnec -> {
            CnecLoopFlowExtension cnecLoopFlowExtension = cnec.getExtension(CnecLoopFlowExtension.class);
            if (!Objects.isNull(cnecLoopFlowExtension)) {
                double initialLoopFlow = Math.abs(loopflows.get(cnec.getId()));
                double loopFlowThreshold = Math.abs(cnecLoopFlowExtension.getInputLoopFlow());
                cnecLoopFlowExtension.setLoopFlowConstraint(Math.max(initialLoopFlow, loopFlowThreshold));
                cnecLoopFlowExtension.setLoopflowShift(loopflowShifts.get(cnec));
            }
        });
    }

    private static boolean useLoopFlowExtension(RaoParameters parameters) {
        return parameters.isRaoWithLoopFlowLimitation();
    }
}
