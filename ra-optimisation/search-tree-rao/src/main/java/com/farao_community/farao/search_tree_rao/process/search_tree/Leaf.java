/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_result_extensions.CracResultExtension;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.ResultVariantManager;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.search_tree_rao.config.SearchTreeConfigurationUtil;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A "leaf" is a node of the search tree.
 * Each leaf contains a Network Action, which should be tested in combination with
 * it's parent Leaves' Network Actions
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class Leaf {

    private static final Logger LOGGER = LoggerFactory.getLogger(Leaf.class);

    /**
     * Parent Leaf or null for root Leaf
     */
    private final Leaf parentLeaf;

    /**
     * Network Actions which will be tested (including the
     * network actions from the parent leaves as well as from
     * this leaf), can be empty
     */
    private final List<NetworkAction> networkActions;

    /**
     * Impact of the network action
     */
    private RaoResult raoResult;

    /**
     * Status of the leaf's Network Action evaluation
     */
    private Status status;

    enum Status {
        CREATED,
        EVALUATION_RUNNING,
        EVALUATION_SUCCESS,
        EVALUATION_ERROR
    }

    /**
     * Root Leaf constructor
     */
    Leaf() { //! constructor only for Root Leaf
        this.parentLeaf = null;
        this.networkActions = new ArrayList<>(); //! root leaf has no network action
        this.raoResult = null;
        this.status = Status.CREATED;
    }

    /**
     * Leaf constructor
     */
    private Leaf(Leaf parentLeaf, NetworkAction networkAction) {
        this.parentLeaf = parentLeaf;

        List<NetworkAction> networkActionList = new ArrayList<>(parentLeaf.getNetworkActions());
        networkActionList.add(networkAction);
        this.networkActions = networkActionList;

        this.raoResult = null;
        this.status = Status.CREATED;
    }

    /**
     * Parent Leaf getter
     */
    Leaf getParent() {
        return parentLeaf;
    }

    List<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    /**
     * Rao results getter
     */
    RaoResult getRaoResult() {
        return raoResult;
    }

    /**
     * Leaf status getter
     */
    Status getStatus() {
        return status;
    }

    /**
     * Is this Leaf the initial one of the tree
     */
    boolean isRoot() {
        return parentLeaf == null;
    }

    /**
     * Extend the tree from the current Leaf with N new children Leaves
     * for the N Network Actions given in argument
     */
    List<Leaf> bloom(Set<NetworkAction> availableNetworkActions) {
        return availableNetworkActions.stream().
                filter(na -> !networkActions.contains(na)).
                map(na -> new Leaf(this, na)).collect(Collectors.toList());
    }

    /**
     * Evaluate the impact of Network Actions (from the current Leaf and
     * its parents)
     */
    void evaluate(Network network, Crac crac, String referenceNetworkVariant, RaoParameters parameters) {
        this.status = Status.EVALUATION_RUNNING;
        String leafNetworkVariant;

        if (isRoot()) {
            LOGGER.info("SearchTreeRao: evaluate root leaf");
        } else {
            String logInfo = "SearchTreeRao: evaluate network action(s)";
            logInfo = logInfo.concat(networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", ")));
            LOGGER.info(logInfo);
        }

        // apply Network Actions
        try {
            leafNetworkVariant = createAndSwitchToNewVariant(network, referenceNetworkVariant);
            networkActions.forEach(na -> na.apply(network));
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            this.status = Status.EVALUATION_ERROR;
            return;
        }

        // Optimize the use of Range Actions
        try {
            RaoResult results = Rao.find(getRangeActionRaoName(parameters)).run(network, crac, leafNetworkVariant, parameters);
            this.raoResult = results;
            this.status = buildStatus(results);
            if (this.status == Status.EVALUATION_SUCCESS) {
                updateRaoResultWithNetworkActions(crac);
            }
            deleteNetworkVariant(network, leafNetworkVariant);

        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            this.status = Status.EVALUATION_ERROR;
            deleteNetworkVariant(network, leafNetworkVariant);
        }
    }

    private String getUniqueVariantId(Network network) {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    private String createAndSwitchToNewVariant(Network network, String referenceNetworkVariant) {
        Objects.requireNonNull(referenceNetworkVariant);
        if (!network.getVariantManager().getVariantIds().contains(referenceNetworkVariant)) {
            throw new FaraoException(String.format("Unknown network variant %s", referenceNetworkVariant));
        }
        String uniqueId = getUniqueVariantId(network);
        network.getVariantManager().cloneVariant(referenceNetworkVariant, uniqueId);
        network.getVariantManager().setWorkingVariant(uniqueId);
        return uniqueId;
    }

    private String getRangeActionRaoName(RaoParameters parameters) {
        return SearchTreeConfigurationUtil.getSearchTreeParameters(parameters).getRangeActionRao();
    }

    private Status buildStatus(RaoResult results) {
        if (results.isSuccessful()) {
            return Status.EVALUATION_SUCCESS;
        } else {
            return Status.EVALUATION_ERROR;
        }
    }

    private void deleteNetworkVariant(Network network, String leafNetworkVariant) {
        if (network.getVariantManager().getVariantIds().contains(leafNetworkVariant)) {
            network.getVariantManager().removeVariant(leafNetworkVariant);
        }
    }

    void deletePostOptimResultVariant(Crac crac) {
        if (isRoot() && raoResult.getPostOptimVariantId().equals(raoResult.getPreOptimVariantId())) {
            return;
        }
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager.getVariants().contains(raoResult.getPostOptimVariantId())) {
            resultVariantManager.deleteVariant(raoResult.getPostOptimVariantId());
        }
    }

    void deletePreOptimResultVariant(Crac crac) {
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager.getVariants().contains(raoResult.getPreOptimVariantId())) {
            resultVariantManager.deleteVariant(raoResult.getPreOptimVariantId());
        }
    }

    public double getCost(Crac crac) {
        Objects.requireNonNull(raoResult);
        return crac.getExtension(CracResultExtension.class).getVariant(raoResult.getPostOptimVariantId()).getCost();
    }

    private void updateRaoResultWithNetworkActions(Crac crac) {
        String variantId = raoResult.getPostOptimVariantId();
        String preventiveState = crac.getPreventiveState().getId();
        for (NetworkAction networkAction : networkActions) {
            networkAction.getExtension(NetworkActionResultExtension.class).getVariant(variantId).activate(preventiveState);
        }
    }
}
