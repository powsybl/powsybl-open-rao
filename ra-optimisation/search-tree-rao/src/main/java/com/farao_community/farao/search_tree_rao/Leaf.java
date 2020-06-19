/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_api.RaoUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.SystematicSensitivityComputation;
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
    private final RaoParameters raoParameters;
    private final SystematicSensitivityComputation systematicSensitivityComputation;
    private final IteratingLinearOptimizer iteratingLinearOptimizer;

    /**
     * Network Actions which will be tested (including the
     * network actions from the parent leaves as well as from
     * this leaf), can be empty for root leaf
     */
    private final Set<NetworkAction> networkActions;

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
     * Root Leaf constructors
     *
     * It is built directly from a RaoData on which a systematic sensitivity analysis could hav already been run or not.
     */
    // This constructor is useful to mock SystematicSensitivityComputation in LeafTest
    Leaf(RaoData raoData, RaoParameters raoParameters, SystematicSensitivityComputation systematicSensitivityComputation, IteratingLinearOptimizer iteratingLinearOptimizer) {
        this.networkActions = new HashSet<>(); // Root leaf has no network action
        this.raoParameters = raoParameters;
        this.systematicSensitivityComputation = systematicSensitivityComputation;
        this.iteratingLinearOptimizer = iteratingLinearOptimizer;
        this.raoData = raoData;
        initialVariantId = raoData.getInitialVariantId();
        if (raoData.hasSensitivityValues()) {
            status = Status.EVALUATED;
        } else {
            status = Status.CREATED;
        }
    }

    Leaf(RaoData raoData, RaoParameters raoParameters) {
        this(raoData, raoParameters, new SystematicSensitivityComputation(
            raoParameters.getDefaultSensitivityComputationParameters(), raoParameters.getFallbackSensitivityComputationParameters()));
    }

    Leaf(RaoData raoData, RaoParameters raoParameters, SystematicSensitivityComputation systematicSensitivityComputation) {
        this(raoData, raoParameters, systematicSensitivityComputation,
            RaoUtil.createLinearOptimizerFromRaoParameters(raoParameters, systematicSensitivityComputation));
    }

    /**
     * Leaf constructorthis.iteratingLinearOptimizer = iteratingLinearOptimizer;
     */
    Leaf(Leaf parentLeaf, NetworkAction networkAction, Network network, RaoParameters raoParameters) {
        networkActions = new HashSet<>(parentLeaf.networkActions);
        networkActions.add(networkAction);
        this.raoParameters = raoParameters;
        this.systematicSensitivityComputation = new SystematicSensitivityComputation(
            parentLeaf.raoParameters.getDefaultSensitivityComputationParameters(), parentLeaf.raoParameters.getFallbackSensitivityComputationParameters());
        iteratingLinearOptimizer = RaoUtil.createLinearOptimizerFromRaoParameters(raoParameters, systematicSensitivityComputation);

        // apply Network Actions on initial network
        networkActions.forEach(na -> na.apply(network));
        // It creates a new CRAC variant
        raoData = new RaoData(network, parentLeaf.getRaoData().getCrac());
        initialVariantId = raoData.getInitialVariantId();
        activateNetworkActionInCracResult(initialVariantId);
        status = Status.CREATED;
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
        if (status.equals(Status.OPTIMIZED)) {
            return optimizedVariantId;
        } else {
            return initialVariantId;
        }
    }

    double getBestCost() {
        return raoData.getCracResult(getBestVariantId()).getCost();
    }

    Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    boolean isRoot() {
        return networkActions.isEmpty();
    }

    /**
     * This method performs a systematic sensitivity computation on the leaf only if it has not been done previously.
     * If the computation works fine status is updated to EVALUATED otherwise it is set to ERROR.
     */
    void evaluate() {
        if (status.equals(Status.CREATED)) {
            try {
                LOGGER.debug("Evaluating leaf...");
                systematicSensitivityComputation.run(raoData, raoParameters.getObjectiveFunction().getUnit());
                raoData.getRaoDataManager().fillCracResultsWithSensis(
                    RaoUtil.createCostEvaluatorFromRaoParameters(raoParameters).getCost(raoData),
                    systematicSensitivityComputation.isFallback() ? raoParameters.getFallbackOverCost() : 0);
                status = Status.EVALUATED;
            } catch (FaraoException e) {
                LOGGER.error(String.format("Fail to evaluate leaf: %s", e.getMessage()));
                status = Status.ERROR;
            }
        }
    }

    /**
     * This method tries to optimize range actions on an already evaluated leaf since range action optimization
     * requires computed sensitivity values. Therefore, the leaf is not optimized if leaf status is either ERROR
     * or CREATED (because it means no sensitivity values have already been computed). Once it is performed the status
     * is updated to OPTIMIZED. Besides, the optimization is not performed if no range actions are available
     * in the CRAC to spare computation time but status will still be set to OPTIMIZED meaning no optimization has to
     * be done on this leaf anymore. IteratingLinearOptimizer should never fail so the optimized variant ID in the end
     * is either the same as the initial variant ID if the optimization has not been efficient or a new ID
     * corresponding to a new variant created by the IteratingLinearOptimizer.
     */
    void optimize() {
        if (status.equals(Status.EVALUATED)) {
            if (!raoData.getCrac().getRangeActions().isEmpty()) {
                LOGGER.debug("Optimizing leaf...");
                optimizedVariantId = iteratingLinearOptimizer.optimize(raoData);
                activateNetworkActionInCracResult(optimizedVariantId);
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
     * This method generates a set a of network actions that would be available after this leaf inside the tree. It
     * means all the available network actions in the CRAC except the ones already used in this leaf.
     *
     * @return A set of available network actions after this leaf.
     */
    Set<NetworkAction> bloom() {
        return raoData.getCrac().getNetworkActions(raoData.getNetwork(), raoData.getCrac().getPreventiveState(), UsageMethod.AVAILABLE)
            .stream()
            .filter(na -> !networkActions.contains(na))
            .collect(Collectors.toSet());
    }

    /**
     * This method deletes completely the initial variant if the optimized variant has better results. So it can be
     * used only if the leaf is OPTIMIZED. This method should not be used on root leaf in the tree as long as it
     * is necessary to keep this variant for algorithm results purpose.
     */
    void cleanVariants() {
        if (status.equals(Status.OPTIMIZED) && !initialVariantId.equals(optimizedVariantId)) {
            raoData.deleteVariant(initialVariantId, false);
        }
    }

    /**
     * This method deletes all the variants of the leaf rao data meaning at least the initial variant and most the
     * initial variant and the optimized variant. It is a delegate method to avoid calling directly rao data as a leaf
     * user.
     */
    void clearVariants() {
        raoData.clear();
    }

    /**
     * This method applies on the rao data network the optimized positions of range actions. It is a delegate method
     * to avoid calling directly rao data as a leaf user.
     */
    void applyRangeActionResultsOnNetwork() {
        getRaoData().setWorkingVariant(getBestVariantId());
        getRaoData().getRaoDataManager().applyRangeActionResultsOnNetwork();
    }

    /**
     * This method activates network actions related to this leaf in the CRAC results. This action has to be done
     * every time a new variant is created inside this leaf to ensure results consistency.
     *
     * @param variantId: The ID of the variant to update.
     */
    private void activateNetworkActionInCracResult(String variantId) {
        String preventiveState = raoData.getCrac().getPreventiveState().getId();
        for (NetworkAction networkAction : networkActions) {
            networkAction.getExtension(NetworkActionResultExtension.class).getVariant(variantId).activate(preventiveState);
        }
    }

    @Override
    public String toString() {
        String info = isRoot() ? "Root leaf" :
            "Network action(s): " + networkActions.stream().map(NetworkAction::getName).collect(Collectors.joining(", "));
        info += String.format(", Cost: %.2f", getBestCost());
        info += ", Status: " + status.getMessage();
        return info;
    }
}
