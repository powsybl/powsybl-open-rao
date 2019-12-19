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
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.search_tree_rao.config.SearchTreeConfigurationUtil;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A "leaf" is a junction of the search tree
 * Each leaf contains a Network Action, which should be tested in combination with
 * it's parent Leaves' Network Actions
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-international.com>}
 */
class Leaf {

    /**
     * Parent Leaf or null for initial Leaf
     */
    private final Leaf parentLeaf;

    /**
     * Network Action which will be tested, can be null
     */
    private final NetworkAction networkAction;

    /**
     * Name of the network variant associated with this Leaf
     */
    private String networkVariant;

    /**
     * Impact of the network action
     */
    private RaoComputationResult raoResult;

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
     * Initial Leaf constructor
     */
    Leaf(String networkVariant) {
        this.parentLeaf = null;
        this.networkAction = null;
        this.networkVariant = networkVariant;
        this.raoResult = null;
        this.status = Status.CREATED;
    }

    /**
     * Leaf constructor
     */
    private Leaf(Leaf parentLeaf, NetworkAction networkAction) {
        this.parentLeaf = parentLeaf;
        this.networkAction = networkAction;
        this.networkVariant = null;
        this.raoResult = null;
        this.status = Status.CREATED;
    }

    /**
     * Parent Leaf getter
     */
    Leaf getParent() {
        return parentLeaf;
    }

    NetworkAction getNetworkAction() {
        return networkAction;
    }

    /**
     * Action impact getter
     */
    RaoComputationResult getRaoResult() {
        return raoResult;
    }

    /**
     * Leaf status getter
     */
    Status getStatus() {
        return status;
    }

    /**
     * Leaf Variant getter
     */
    String getNetworkVariant() {
        return networkVariant;
    }

    /**
     * Is this Leaf the initial one of the tree
     */
    boolean isRoot() {
        return parentLeaf == null;
    }

    /**
     * Get the list of Network Actions from the current leaf, and from its
     * parent leaves
     */
    List<NetworkAction> getNetworkActionLegacy() {
        Leaf leaf = this;
        List<NetworkAction> naList = new ArrayList<>();
        while (!leaf.isRoot()) {
            naList.add(leaf.getNetworkAction());
            leaf = leaf.getParent();
        }
        return naList;
    }

    /**
     * Extend the tree from the current Leaf with N new children Leaves
     * for the N Network Actions given in argument
     */
    List<Leaf> bloom(Set<NetworkAction> availableNetworkActions) {
        List<NetworkAction> legacy = getNetworkActionLegacy();
        return availableNetworkActions.stream().
                filter(na -> !legacy.contains(na)).
                map(na -> new Leaf(this, na)).collect(Collectors.toList());
    }

    /**
     * Evaluate the impact of Network Actions (from the current Leaf and
     * its parents)
     */
    void evaluate(Network network, Crac crac, RaoParameters parameters) {
        this.status = Status.EVALUATION_RUNNING;

        try {
            // apply Network Action
            if (!isRoot()) {
                Objects.requireNonNull(networkAction);
                this.networkVariant = createVariant(network, getParent().getNetworkVariant());
                network.getVariantManager().setWorkingVariant(this.networkVariant);
                networkAction.apply(network);
            } else {
                network.getVariantManager().setWorkingVariant(this.networkVariant);
            }

            // Optimize the use of Range Actions
            RaoComputationResult results = Rao.find(getRangeActionRaoName(parameters)).run(network, crac);

            // Get results
            this.raoResult = results;
            this.status = buildStatus(results);

        } catch (FaraoException e) {
            this.status = Status.EVALUATION_ERROR;
        }
    }

    private String getUniqueVariantId(Network network) {
        String uniqueId;
        do {
            uniqueId = UUID.randomUUID().toString();
        } while (network.getVariantManager().getVariantIds().contains(uniqueId));
        return uniqueId;
    }

    private String createVariant(Network network, String referenceNetworkVariant) {
        Objects.requireNonNull(referenceNetworkVariant);
        if (!network.getVariantManager().getVariantIds().contains(referenceNetworkVariant)) {
            throw new FaraoException(String.format("Unknown network variant %s", referenceNetworkVariant));
        }
        String uniqueId = getUniqueVariantId(network);
        network.getVariantManager().cloneVariant(referenceNetworkVariant, uniqueId);
        return uniqueId;
    }

    private String getRangeActionRaoName(RaoParameters parameters) {
        return SearchTreeConfigurationUtil.getSearchTreeParameters(parameters).getRangeActionRao();
    }

    private Status buildStatus(RaoComputationResult results) {
        if (results.getStatus().equals(RaoComputationResult.Status.SUCCESS)) {
            return Status.EVALUATION_SUCCESS;
        } else {
            return Status.EVALUATION_ERROR;
        }
    }
}
