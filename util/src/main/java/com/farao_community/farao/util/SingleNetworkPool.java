/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A {@code AbstractNetworkPool} implementation that is used when parallelism = 1
 * Instead of creating a (useless) copy of the network object, it uses the network object itself
 * while correctly handling setup and cleanup of variants
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class SingleNetworkPool extends AbstractNetworkPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleNetworkPool.class);
    private String networkInitialVariantId;
    private Network network;
    private Collection<String> baseNetworkVariantIds;

    SingleNetworkPool(Network network, String targetVariant) {
        super(network, targetVariant, 1);
    }

    @Override
    protected void initAvailableNetworks(Network network) {
        this.networkInitialVariantId = network.getVariantManager().getWorkingVariantId();
        this.network = network;
        this.baseNetworkVariantIds = new HashSet<>(network.getVariantManager().getVariantIds());
        LOGGER.info("Using base network '{}' on variant '{}'", network.getId(), targetVariant);
        network.getVariantManager().setWorkingVariant(targetVariant);
        network.getVariantManager().cloneVariant(networkInitialVariantId, Arrays.asList(stateSaveVariant, workingVariant), true);
        boolean isSuccess = networksQueue.offer(network);
        if (!isSuccess) {
            throw new AssertionError("Cannot offer base network in pool. Should not happen");
        }
    }

    @Override
    protected void cleanVariants(Network networkClone) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
            .filter(variantId -> !baseNetworkVariantIds.contains(variantId))
            .filter(variantId -> !variantId.equals(stateSaveVariant))
            .collect(Collectors.toList());
        variantsToBeRemoved.forEach(variantId -> networkClone.getVariantManager().removeVariant(variantId));
    }

    @Override
    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.shutdown();
        super.awaitTermination(timeout, unit);
        cleanBaseNetwork();
    }

    private void cleanBaseNetwork() {
        cleanVariants(network);
        network.getVariantManager().removeVariant(stateSaveVariant);
        network.getVariantManager().setWorkingVariant(networkInitialVariantId);
    }
}
