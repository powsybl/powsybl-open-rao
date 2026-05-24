/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions.AppliedRemedialActionsPerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages virtual network variants for the search-tree RAO without physically cloning the network.
 *
 * <p>Each node (leaf) in the search tree applies a set of remedial actions on top of its parent's
 * state. Instead of creating a real {@link com.powsybl.iidm.network.Network} variant per leaf —
 * which is expensive in memory and CPU — this manager tracks the incremental
 * {@link AppliedRemedialActionsPerState} as a linked chain of lightweight {@link VirtualVariant}
 * records. When sensitivity analysis is needed, the full set of applied actions is accumulated by
 * walking the parent chain and passed to the {@link SensitivityComputer}.
 *
 * <p>Thread safety: variant creation is atomic via {@link java.util.concurrent.ConcurrentHashMap}
 * and the working variant is {@link ThreadLocal}, so concurrent search-tree leaves running on
 * different threads do not interfere with each other.
 *
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class VirtualVariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualVariantManager.class);

    /**
     * A node in the virtual variant tree.
     *
     * <p>Holds only the remedial actions applied at this specific node. The full picture for
     * sensitivity analysis is obtained by {@link #getFullAppliedRemedialActions()}, which walks
     * the {@code parent} chain and merges all incremental actions bottom-up.
     */
    protected record VirtualVariant(String variantId, AppliedRemedialActionsPerState appliedRemedialActions, VirtualVariant parent) {

        private void add(AppliedRemedialActionsPerState fullAppliedRemedialActions) {
            fullAppliedRemedialActions.getRangeActions().putAll(appliedRemedialActions.getRangeActions());
            fullAppliedRemedialActions.getNetworkActions().addAll(appliedRemedialActions.getNetworkActions());
            if (parent != null) {
                parent.add(fullAppliedRemedialActions);
            }
        }

        public AppliedRemedialActionsPerState getFullAppliedRemedialActions() {
            AppliedRemedialActionsPerState fullAppliedRemedialActions = new AppliedRemedialActionsPerState();
            add(fullAppliedRemedialActions);
            return fullAppliedRemedialActions;
        }
    }

    private final Map<String, VirtualVariant> variantsById = new ConcurrentHashMap<>();
    // each thread has its own working variant so concurrent leaves don't interfere
    private final ThreadLocal<VirtualVariant> workingVariant = new ThreadLocal<>();

    /**
     * Sets the working variant for the calling thread.
     *
     * <p>If {@code newVariantId} already exists in the registry it is reused as-is. Otherwise a
     * new {@link VirtualVariant} is created: if {@code fromVariantId} is a real network variant
     * the new virtual variant has no parent; if it is itself a virtual variant the new variant
     * inherits from it, extending the parent chain.
     *
     * @param network       the network, used to distinguish real variants from virtual ones
     * @param fromVariantId id of the real or virtual variant to branch from
     * @param newVariantId  id of the virtual variant to create or reuse
     * @throws OpenRaoException if {@code fromVariantId} is neither a real network variant nor a
     *                          known virtual variant
     */
    public void setWorkingVariant(Network network, String fromVariantId, String newVariantId) {
        VirtualVariant variant = variantsById.computeIfAbsent(newVariantId, id -> {
            VirtualVariant fromVariant = null;
            if (network.getVariantManager().getVariantIds().contains(fromVariantId)) {
                LOGGER.info("Create virtual variant '{}' from variant '{}'", newVariantId, fromVariantId);
            } else {
                fromVariant = variantsById.get(fromVariantId);
                if (fromVariant == null) {
                    throw new OpenRaoException("From variant '" + fromVariantId + "' not found");
                }
                LOGGER.info("Create virtual variant '{}' from virtual variant '{}'", newVariantId, fromVariantId);
            }
            return new VirtualVariant(newVariantId, new AppliedRemedialActionsPerState(), fromVariant);
        });
        workingVariant.set(variant);
    }

    /**
     * Removes all virtual variants and clears the calling thread's working variant.
     * Should be called once all leaves have been evaluated to release memory.
     */
    public void removeWorkingVariants() {
        LOGGER.info("Remove all virtual variants");
        variantsById.clear();
        workingVariant.remove();
    }

    protected void checkWorkingVariantIsSet() {
        if (workingVariant.get() == null) {
            throw new OpenRaoException("Working variant not set");
        }
    }

    /**
     * Records a range action setpoint in the calling thread's working variant.
     *
     * @throws OpenRaoException  if no working variant has been set for the calling thread
     * @throws NullPointerException if {@code rangeAction} is null
     */
    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        VirtualVariant variant = workingVariant.get();
        LOGGER.info("Add range action '{}' to virtual variant '{}'", rangeAction.getId(), variant.variantId());
        variant.appliedRemedialActions().addAppliedRangeAction(rangeAction, setpoint);
    }

    /**
     * Records a network action in the calling thread's working variant.
     *
     * @throws OpenRaoException  if no working variant has been set for the calling thread
     * @throws NullPointerException if {@code networkAction} is null
     */
    public void applyNetworkAction(NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        VirtualVariant variant = workingVariant.get();
        LOGGER.info("Add network action '{}' to virtual variant '{}'", networkAction.getId(), variant.variantId());
        variant.appliedRemedialActions().addAppliedNetworkAction(networkAction);
    }

    /**
     * Runs sensitivity analysis for the calling thread's working variant.
     *
     * <p>The full set of applied remedial actions — accumulated from the root of the virtual
     * variant tree down to the current working variant — is passed to the
     * {@link SensitivityComputer}. The network is not modified.
     */
    public void compute(SensitivityComputer sensitivityComputer, Network network) {
        sensitivityComputer.compute(network, workingVariant.get().getFullAppliedRemedialActions());
    }
}
