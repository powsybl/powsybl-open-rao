/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.RandomizedString;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoNetworkPool extends ForkJoinPool implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FaraoNetworkPool.class);
    private final BlockingQueue<Network> networksQueue;
    private final String targetVariant;

    // State used to save initial content of target variant.
    // Useful when targetVariant equals VariantManagerConstants.INITIAL_VARIANT_ID
    private final String stateSaveVariant;

    public FaraoNetworkPool(Network network, String targetVariant, int parallelism) {
        super(parallelism);
        Objects.requireNonNull(network);
        this.targetVariant = Objects.requireNonNull(targetVariant);
        this.stateSaveVariant = RandomizedString.getRandomizedString("FaraoNetworkPool state save ", network.getVariantManager().getVariantIds(), 5);
        this.networksQueue = new ArrayBlockingQueue<>(getParallelism());
        initAvailableNetworks(network);
    }

    public FaraoNetworkPool(Network network, String targetVariant) {
        this(network, targetVariant, Runtime.getRuntime().availableProcessors());
    }

    private void initAvailableNetworks(Network network) {
        LOGGER.info("Filling network pool with copies of network '{}' on variant '{}'", network.getId(), targetVariant);
        String initialVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(targetVariant);
        for (int i = 0; i < getParallelism(); i++) {
            LOGGER.info("Copy n°{}", i + 1);
            Network copy = NetworkXml.copy(network);
            // The initial network working variant is VariantManagerConstants.INITIAL_VARIANT_ID
            // in cloned network, so we need to copy it again.
            copy.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, Arrays.asList(targetVariant, stateSaveVariant), true);
            boolean isSuccess = networksQueue.offer(copy);
            if (!isSuccess) {
                throw new AssertionError(String.format("Cannot offer copy n°'%d' in pool. Should not happen", i + 1));
            }
        }
        network.getVariantManager().setWorkingVariant(initialVariant);
    }

    public Network getAvailableNetwork() throws InterruptedException {
        Network networkClone = networksQueue.take();
        networkClone.getVariantManager().cloneVariant(stateSaveVariant, targetVariant, true);
        networkClone.getVariantManager().setWorkingVariant(targetVariant);
        return networkClone;
    }

    private void cleanVariants(Network networkClone) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
                .filter(variantId -> !variantId.equals(VariantManagerConstants.INITIAL_VARIANT_ID))
                .filter(variantId -> !variantId.equals(stateSaveVariant))
                .collect(Collectors.toList());
        variantsToBeRemoved.forEach(variantId -> networkClone.getVariantManager().removeVariant(variantId));
    }

    public void releaseUsedNetwork(Network networkToRelease) throws InterruptedException {
        cleanVariants(networkToRelease);
        networksQueue.put(networkToRelease);
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
