/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A {@link AbstractNetworkPool} implementation that allows multithreaded computations on a single network instance
 * through well-handled variants.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MultiThreadNetworkPool extends AbstractNetworkPool {

    private final BlockingQueue<String> availableVariants = new ArrayBlockingQueue<>(getParallelism());
    private final ThreadLocal<String> threadVariant = new ThreadLocal<>();
    private int networkNumberOfVariants = 0;

    public MultiThreadNetworkPool(Network network, String targetVariant, int parallelism, boolean initVariants) {
        super(network, targetVariant, parallelism);
        network.getVariantManager().allowVariantMultiThreadAccess(true);
        if (initVariants) {
            initClones(parallelism);
        }
    }

    @Override
    public int getNetworkNumberOfClones() {
        return networkNumberOfVariants;
    }

    @Override
    public void initClones(int desiredNumberOfVariants) {
        int requiredVariants = Math.min(getParallelism(), desiredNumberOfVariants);
        int variantsToAdd = requiredVariants - networkNumberOfVariants;

        if (variantsToAdd <= 0) {
            return;
        }

        OpenRaoLoggerProvider.TECHNICAL_LOGS.debug("Filling network pool with {} new variant{} of network {} on variant {}", variantsToAdd, variantsToAdd == 1 ? "" : "s", network.getId(), targetVariant);

        for (int i = 0; i < variantsToAdd; i++) {
            String variantName = workingVariant + "_" + (networkNumberOfVariants + i);
            network.getVariantManager().cloneVariant(targetVariant, Collections.singletonList(variantName), true);
            availableVariants.offer(variantName);
            boolean isSuccess = networksQueue.offer(network);
            if (!isSuccess) {
                throw new AssertionError("Cannot offer network in pool. Should not happen");
            }
        }
        networkNumberOfVariants = requiredVariants;
    }

    @Override
    public Network getAvailableNetwork() throws InterruptedException {
        Network availableNetwork = networksQueue.take();
        String variantName = availableVariants.take();
        threadVariant.set(variantName);
        availableNetwork.getVariantManager().setWorkingVariant(variantName);
        return availableNetwork;
    }

    @Override
    public void releaseUsedNetwork(Network networkToRelease, boolean deleteWorkingVariant) throws InterruptedException {
        String variantName = threadVariant.get();
        if (variantName != null) {
            availableVariants.put(variantName);
            threadVariant.remove();
        }
        networksQueue.put(networkToRelease);
    }

    @Override
    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.shutdown();
        super.awaitTermination(timeout, unit);
        cleanBaseNetwork();
        network.getVariantManager().allowVariantMultiThreadAccess(false);
    }

    @Override
    protected void cleanBaseNetwork() {
        network.getVariantManager().setWorkingVariant(networkInitialVariantId);
        cleanVariants(network);
    }
}
