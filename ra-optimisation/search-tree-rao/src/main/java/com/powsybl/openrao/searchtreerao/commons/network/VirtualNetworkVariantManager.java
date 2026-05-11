/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.SensitivityComputer;
import com.powsybl.openrao.sensitivityanalysis.AppliedRemedialActions.AppliedRemedialActionsPerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class VirtualNetworkVariantManager implements NetworkVariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualNetworkVariantManager.class);

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

    private final Network network;
    private final Map<String, VirtualVariant> variantsById = new HashMap<>();
    private VirtualVariant workingVariant;

    public VirtualNetworkVariantManager(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public void setWorkingVariant(String fromVariantId, String newVariantId) {
        VirtualVariant variant = variantsById.get(newVariantId);
        if (variant != null) {
            workingVariant = variant;
        } else {
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
            workingVariant = new VirtualVariant(newVariantId, new AppliedRemedialActionsPerState(), fromVariant);
            variantsById.put(newVariantId, workingVariant);
        }
    }

    @Override
    public void removeWorkingVariants() {
        LOGGER.info("Remove all virtual variants");
        variantsById.clear();
        workingVariant = null;
    }

    protected void checkWorkingVariantIsSet() {
        if (workingVariant == null) {
            throw new OpenRaoException("Working variant not set");
        }
    }

    @Override
    public void applyRangeAction(RangeAction<?> rangeAction, double setpoint) {
        Objects.requireNonNull(rangeAction);
        checkWorkingVariantIsSet();
        LOGGER.info("Add range action '{}' to virtual variant '{}'", rangeAction.getId(), workingVariant.variantId);
        workingVariant.appliedRemedialActions.addAppliedRangeAction(rangeAction, setpoint);
    }

    @Override
    public void applyNetworkAction(NetworkAction networkAction) {
        Objects.requireNonNull(networkAction);
        checkWorkingVariantIsSet();
        LOGGER.info("Add network action '{}' to virtual variant '{}'", networkAction.getId(), workingVariant.variantId);
        workingVariant.appliedRemedialActions.addAppliedNetworkAction(networkAction);
    }

    @Override
    public void compute(SensitivityComputer sensitivityComputer) {
        sensitivityComputer.compute(network, workingVariant.getFullAppliedRemedialActions());
    }
}
