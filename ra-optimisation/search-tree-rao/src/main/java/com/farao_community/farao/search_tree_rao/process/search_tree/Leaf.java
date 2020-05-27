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
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
import com.powsybl.commons.PowsyblException;
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

    private String preOptimVariantId;
    private String postOptimVariantId;

    public String getPreOptimVariantId() {
        return preOptimVariantId;
    }

    public String getPostOptimVariantId() {
        return postOptimVariantId;
    }

    public RaoResult.Status getLeafStatus() {
        if (!Objects.isNull(postOptimVariantId)) {
            return RaoResult.Status.FAILURE;
        } else {
            return RaoResult.Status.SUCCESS;
        }
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
     * its parents).
     * This method takes a network variant which we switch too, since we may
     * not generate new variants while multithreading.
     */
    void evaluate(RaoData raoData, String networkVariant, RaoParameters raoParameters) {
        this.status = Status.EVALUATION_RUNNING;

        if (isRoot()) {
            LOGGER.info("SearchTreeRao: evaluate root leaf");
        } else {
            String logInfo = "SearchTreeRao: evaluate network action(s)";
            logInfo = logInfo.concat(networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", ")));
            LOGGER.info(logInfo);
        }

        // apply Network Actions
        try {
            raoData.getNetwork().getVariantManager().setWorkingVariant(networkVariant);
            networkActions.forEach(na -> na.apply(raoData.getNetwork()));
        } catch (FaraoException | PowsyblException e) {
            LOGGER.error(e.getMessage());
            this.status = Status.EVALUATION_ERROR;
            return;
        }

        // Optimize the use of Range Actions
        try {
            preOptimVariantId = raoData.getWorkingVariantId();
            raoData.fillRangeActionResultsWithNetworkValues();
            SystematicSensitivityComputation systematicSensitivityComputation = new SystematicSensitivityComputation(raoParameters);
            systematicSensitivityComputation.run(raoData);
            postOptimVariantId = IteratingLinearOptimizer.optimize(raoData, systematicSensitivityComputation, raoParameters);
            this.status = Status.EVALUATION_SUCCESS;
            updateRaoResultWithNetworkActions(raoData.getCrac());
        } catch (FaraoException e) {
            LOGGER.error(e.getMessage());
            this.status = Status.EVALUATION_ERROR;
        }
    }

    void deletePostOptimResultVariant(Crac crac) {
        Objects.requireNonNull(preOptimVariantId);
        Objects.requireNonNull(postOptimVariantId);
        if (isRoot() && postOptimVariantId.equals(preOptimVariantId)) {
            return;
        }
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager.getVariants().contains(postOptimVariantId)) {
            resultVariantManager.deleteVariant(postOptimVariantId);
        }
    }

    void deletePreOptimResultVariant(Crac crac) {
        Objects.requireNonNull(preOptimVariantId);
        Objects.requireNonNull(postOptimVariantId);
        if (!isRoot() && postOptimVariantId.equals(preOptimVariantId)) {
            return;
        }
        ResultVariantManager resultVariantManager = crac.getExtension(ResultVariantManager.class);
        if (resultVariantManager.getVariants().contains(preOptimVariantId)) {
            resultVariantManager.deleteVariant(preOptimVariantId);
        }
    }

    public double getCost(Crac crac) {
        Objects.requireNonNull(postOptimVariantId);
        return crac.getExtension(CracResultExtension.class).getVariant(postOptimVariantId).getCost();
    }

    private void updateRaoResultWithNetworkActions(Crac crac) {
        String preventiveState = crac.getPreventiveState().getId();
        for (NetworkAction networkAction : networkActions) {
            networkAction.getExtension(NetworkActionResultExtension.class).getVariant(postOptimVariantId).activate(preventiveState);
        }
    }
}
