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
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.systematic_sensitivity.SystematicSensitivityComputation;
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

    private final RaoData raoData;
    private final String initialVariantId;
    private String optimizedVariantId;
    private SystematicSensitivityComputation systematicSensitivityComputation;

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

    enum Status {
        CREATED("Created"),
        ERROR("Error"),
        EVALUATED("Evaluated"),
        OPTIMIZED("Optimzed");

        private String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Status of the leaf's Network Action evaluation
     */
    private Status status;

    /**
     * Root Leaf constructor
     */
    Leaf(RaoData raoData) { //! constructor only for Root Leaf
        this.parentLeaf = null;
        this.networkActions = new ArrayList<>(); //! root leaf has no network action
        this.raoData = raoData;
        initialVariantId = raoData.getInitialVariantId();
        initStatus();
    }

    /**
     * Leaf constructor
     */
    Leaf(Leaf parentLeaf, NetworkAction networkAction, Network network, Crac crac) {
        this.parentLeaf = parentLeaf;
        List<NetworkAction> networkActionList = new ArrayList<>(parentLeaf.networkActions);
        networkActionList.add(networkAction);
        this.networkActions = networkActionList;
        // apply Network Actions on initial network
        networkActions.forEach(na -> na.apply(network));
        // It creates a new CRAC variant
        raoData = new RaoData(network, crac);
        initialVariantId = raoData.getInitialVariantId();
        raoData.fillRangeActionResultsWithNetworkValues();
        updateCracResultWithNetworkActions(initialVariantId);
        initStatus();
    }

    private void initStatus() {
        if (raoData.hasSensitivityValues()) {
            status = Status.EVALUATED;
        } else {
            status = Status.CREATED;
        }
    }

    RaoData getRaoData() {
        return raoData;
    }

    Status getStatus() {
        return status;
    }

    String getInitialVariantId() {
        return initialVariantId;
    }

    String getBestVariantId() {
        if (optimizedVariantId != null) {
            return optimizedVariantId;
        } else {
            return initialVariantId;
        }
    }

    double getBestCost() {
        return raoData.getCracResult(getBestVariantId()).getCost();
    }

    void evaluate(RaoParameters raoParameters) {
        if (status.equals(Status.CREATED)) { // This computation has to be performed only if it's not already done
            try {
                LOGGER.debug("Evaluating leaf...");
                systematicSensitivityComputation = new SystematicSensitivityComputation(raoParameters);
                systematicSensitivityComputation.run(raoData);
                status = Status.EVALUATED;
            } catch (FaraoException e) {
                LOGGER.error(String.format("Fail to evaluate leaf: %s", e.getMessage()));
                status = Status.ERROR;
            }
        }
    }

    /**
     * Evaluate the impact of Network Actions (from the current Leaf and
     * its parents).
     * This method takes a network variant which we switch too, since we may
     * not generate new variants while multithreading.
     */
    void optimize(RaoParameters raoParameters) {
        // This computation has to be performed only if sensis have already been computed
        if (status.equals(Status.EVALUATED)) {
            // Try to optimize preOptim variant in postOptim variant
            if (!raoData.getCrac().getRangeActions().isEmpty()) {
                LOGGER.debug("Optimizing leaf...");
                optimizedVariantId = IteratingLinearOptimizer.optimize(raoData, systematicSensitivityComputation, raoParameters);
                updateCracResultWithNetworkActions(optimizedVariantId);
            } else {
                LOGGER.info("No linear optimization to be performed because no range actions are available");
                optimizedVariantId = initialVariantId;
            }
            status = Status.OPTIMIZED;
        } else if (status.equals(Status.ERROR)) {
            LOGGER.warn(String.format("Impossible to optimize leaf: %s%n because evaluation failed", toString()));
        } else if (status.equals(Status.CREATED)) {
            LOGGER.warn(String.format("Impossible to optimize leaf: %s%n because evaluation has not been performed", toString()));
        }
    }

    /**
     * Extend the tree from the current Leaf with N new children Leaves
     * for the N Network Actions given in argument
     */
    Set<NetworkAction> bloom() {
        return raoData.getCrac().getNetworkActions(raoData.getNetwork(), raoData.getCrac().getPreventiveState(), UsageMethod.AVAILABLE)
            .stream()
            .filter(na -> !networkActions.contains(na))
            .collect(Collectors.toSet());
    }

    void cleanVariants() {
        if (optimizedVariantId != null && !initialVariantId.equals(optimizedVariantId)) {
            raoData.deleteVariant(initialVariantId, false);
        }
    }

    void clearVariants() {
        raoData.clear();
    }

    private void updateCracResultWithNetworkActions(String variantId) {
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        for (NetworkAction networkAction : networkActions) {
            networkAction.getExtension(NetworkActionResultExtension.class).getVariant(variantId).activate(preventiveState);
        }
    }

    @Override
    public String toString() {
        String info = parentLeaf == null ? "Root leaf" :
            "Network action(s): " + networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
        info += String.format(", Cost: %.2f", getBestCost());
        info += ", Status: " + status.getMessage();
        return info;
    }
}
