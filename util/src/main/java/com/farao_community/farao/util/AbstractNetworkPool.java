/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.RandomizedString;
import com.powsybl.iidm.network.Network;

import java.util.Objects;
import java.util.concurrent.*;

import static com.farao_community.farao.util.MCDContextWrapper.wrapWithMdcContext;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractNetworkPool extends ForkJoinPool implements AutoCloseable {
    protected final BlockingQueue<Network> networksQueue;
    protected final String targetVariant;
    protected final String workingVariant;
    // State used to save initial content of target variant.
    // Useful when targetVariant equals VariantManagerConstants.INITIAL_VARIANT_ID
    protected final String stateSaveVariant;

    public static AbstractNetworkPool create(Network network, String targetVariant, int parallelism) {
        if (parallelism == 1) {
            return new SingleNetworkPool(network, targetVariant);
        } else {
            return new MultipleNetworkPool(network, targetVariant, parallelism);
        }
    }

    protected AbstractNetworkPool(Network network, String targetVariant, int parallelism) {
        super(parallelism);
        Objects.requireNonNull(network);
        this.targetVariant = Objects.requireNonNull(targetVariant);
        this.stateSaveVariant = RandomizedString.getRandomizedString("FaraoNetworkPool state save ", network.getVariantManager().getVariantIds(), 5);
        this.workingVariant = RandomizedString.getRandomizedString("FaraoNetworkPool working variant ", network.getVariantManager().getVariantIds(), 5);
        this.networksQueue = new ArrayBlockingQueue<>(getParallelism());
        initAvailableNetworks(network);
    }

    public Network getAvailableNetwork() throws InterruptedException {
        Network networkClone = networksQueue.take();
        networkClone.getVariantManager().cloneVariant(stateSaveVariant, workingVariant, true);
        networkClone.getVariantManager().setWorkingVariant(workingVariant);
        return networkClone;
    }

    protected abstract void initAvailableNetworks(Network network);

    public abstract void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    public void releaseUsedNetwork(Network networkToRelease) throws InterruptedException {
        cleanVariants(networkToRelease);
        networksQueue.put(networkToRelease);
    }

    protected abstract void cleanVariants(Network networkToRelease);

    @Override
    public void close() {
        shutdownNow();
    }

    // This will transfer the previous MDC context to the new thread from its pool making it MDC aware, so extra-fields created by application are propagated correctly
    // Must configure the same way different methods from ForkJoinPool when needed
    @Override
    public ForkJoinTask<?> submit(Runnable task) {
        return super.submit(wrapWithMdcContext(task));
    }

}
