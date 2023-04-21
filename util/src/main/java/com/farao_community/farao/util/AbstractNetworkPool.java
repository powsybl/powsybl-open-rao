/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.RandomizedString;
import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;
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
    protected int networkNumberOfClones;
    protected Network network;
    protected String networkInitialVariantId;
    protected Set<String> baseNetworkVariantIds;

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
        this.networkNumberOfClones = 1;
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

    protected void initAvailableNetworks(Network network) {
        this.networkInitialVariantId = network.getVariantManager().getWorkingVariantId();
        this.network = network;
        this.baseNetworkVariantIds = new HashSet<>(network.getVariantManager().getVariantIds());
        FaraoLoggerProvider.TECHNICAL_LOGS.info("Using base network '{}' on variant '{}'", network.getId(), targetVariant);
        network.getVariantManager().setWorkingVariant(targetVariant);
        network.getVariantManager().cloneVariant(networkInitialVariantId, Arrays.asList(stateSaveVariant, workingVariant), true);
        boolean isSuccess = networksQueue.offer(network);
        if (!isSuccess) {
            throw new AssertionError("Cannot offer base network in pool. Should not happen");
        }
    }

    public abstract void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    public void releaseUsedNetwork(Network networkToRelease) throws InterruptedException {
        cleanVariants(networkToRelease);
        networksQueue.put(networkToRelease);
    }

    protected void cleanVariants(Network networkClone) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
                .filter(variantId -> !baseNetworkVariantIds.contains(variantId))
                .filter(variantId -> !variantId.equals(stateSaveVariant))
                .collect(Collectors.toList());
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

    public int getNetworkNumberOfClones() {
        // The number of clones includes the original network itself
        return networkNumberOfClones;
    }

    public void addNetworkClones(int numberOfClones) {

        int addedClones;
        int previousClones = this.networkNumberOfClones;

        if (this.networkNumberOfClones + numberOfClones > getParallelism()) {
            addedClones = getParallelism() - this.networkNumberOfClones;
            this.networkNumberOfClones = getParallelism();
        } else {
            addedClones = numberOfClones;
            this.networkNumberOfClones += numberOfClones;
        }

        TECHNICAL_LOGS.debug("Filling network pool with '{}' cop'{}' of network '{}' on variant '{}'", addedClones, addedClones == 1 ? "y" : "ies", network.getId(), targetVariant);

        network.getVariantManager().setWorkingVariant(targetVariant);

        for (int i = previousClones; i < previousClones + addedClones; i++) {
            TECHNICAL_LOGS.debug("Copy n°{}", i + 1);
            Network copy = NetworkXml.copy(network);
            // The initial network working variant is VariantManagerConstants.INITIAL_VARIANT_ID
            // in cloned network, so we need to copy it again.
            copy.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, Arrays.asList(stateSaveVariant, workingVariant), true);
            boolean isSuccess = networksQueue.offer(copy);
            if (!isSuccess) {
                throw new AssertionError(String.format("Cannot offer copy n°'%d' in pool. Should not happen", i + 1));
            }
        }
        network.getVariantManager().setWorkingVariant(networkInitialVariantId);

    }

}
