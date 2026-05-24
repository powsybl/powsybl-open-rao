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
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class VirtualVariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualVariantManager.class);

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

    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        VirtualVariant variant = workingVariant.get();
        LOGGER.info("Add range action '{}' to virtual variant '{}'", rangeAction.getId(), variant.variantId());
        variant.appliedRemedialActions().addAppliedRangeAction(rangeAction, setpoint);
    }

    public void applyNetworkAction(NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        VirtualVariant variant = workingVariant.get();
        LOGGER.info("Add network action '{}' to virtual variant '{}'", networkAction.getId(), variant.variantId());
        variant.appliedRemedialActions().addAppliedNetworkAction(networkAction);
    }

    public void compute(SensitivityComputer sensitivityComputer, Network network) {
        sensitivityComputer.compute(network, workingVariant.get().getFullAppliedRemedialActions());
    }
}
