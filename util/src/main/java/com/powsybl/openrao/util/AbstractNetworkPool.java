/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.RandomizedString;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.concurrent.*;

import static com.powsybl.openrao.util.MCDContextWrapper.wrapWithMdcContext;

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
    protected Network network;
    protected String networkInitialVariantId;
    protected Set<String> baseNetworkVariantIds;

    public static AbstractNetworkPool create(Network network, String targetVariant, int parallelism, boolean initClones) {
        if (parallelism == 1) {
            return new SingleNetworkPool(network, targetVariant);
        } else {
            return new MultipleNetworkPool(network, targetVariant, parallelism, initClones);
        }
    }

    protected AbstractNetworkPool(Network network, String targetVariant, int parallelism) {
        super(parallelism);
        Objects.requireNonNull(network);
        this.targetVariant = Objects.requireNonNull(targetVariant);
        this.stateSaveVariant = RandomizedString.getRandomizedString("OpenRaoNetworkPool state save ", network.getVariantManager().getVariantIds(), 5);
        this.workingVariant = RandomizedString.getRandomizedString("OpenRaoNetworkPool working variant ", network.getVariantManager().getVariantIds(), 5);
        this.networksQueue = new ArrayBlockingQueue<>(getParallelism());
        this.networkInitialVariantId = network.getVariantManager().getWorkingVariantId();
        this.network = network;
        this.baseNetworkVariantIds = new HashSet<>(network.getVariantManager().getVariantIds());
    }

    public Network getAvailableNetwork() throws InterruptedException {
        Network networkClone = networksQueue.take();
        if (!networkClone.getVariantManager().getVariantIds().contains(workingVariant)) {
            networkClone.getVariantManager().cloneVariant(stateSaveVariant, workingVariant, true);
        }
        networkClone.getVariantManager().setWorkingVariant(workingVariant);
        return networkClone;
    }

    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.shutdown();
        super.awaitTermination(timeout, unit);
    }

    protected void cleanBaseNetwork() {
        cleanVariants(network);
        network.getVariantManager().removeVariant(stateSaveVariant);
        network.getVariantManager().setWorkingVariant(networkInitialVariantId);
    }

    public void releaseUsedNetwork(Network networkToRelease) throws InterruptedException {
        releaseUsedNetwork(networkToRelease, true);
    }

    public void releaseUsedNetwork(Network networkToRelease, boolean deleteWorkingVariant) throws InterruptedException {
        cleanVariants(networkToRelease, deleteWorkingVariant);
        networksQueue.put(networkToRelease);
    }

    protected void cleanVariants(Network networkClone) {
        cleanVariants(networkClone, true);
    }

    protected void cleanVariants(Network networkClone, boolean deleteWorkingVariant) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
                .filter(variantId -> !baseNetworkVariantIds.contains(variantId))
                .filter(variantId -> !variantId.equals(stateSaveVariant))
                .filter(variantId -> deleteWorkingVariant || !variantId.equals(workingVariant))
                .toList();
        variantsToBeRemoved.forEach(variantId -> networkClone.getVariantManager().removeVariant(variantId));
    }

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

    @Override
    public <T> ForkJoinTask<T> submit(Callable<T> task) {
        return super.submit(wrapWithMdcContext(task));
    }

    public int getNetworkNumberOfClones() {
        return 1;
    }

    public abstract void initClones(int desiredNumberOfClones);

}
