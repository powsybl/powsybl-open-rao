/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.logs.FaraoLoggerProvider;
import com.powsybl.iidm.network.Network;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoVariantsPool extends ForkJoinPool implements AutoCloseable {
    private static final Lock LOCK = new ReentrantLock();
    private final BlockingQueue<String> variantsQueue;
    private final Network network;
    private final String initialVariant;

    public FaraoVariantsPool(Network network, String initialVariant, int parallelism) {
        super(parallelism);
        this.network = network;
        this.initialVariant = initialVariant;
        this.variantsQueue = new ArrayBlockingQueue<>(getParallelism());
        initAvailableVariants();
    }

    public FaraoVariantsPool(Network network, String initialVariant) {
        this(network, initialVariant, Runtime.getRuntime().availableProcessors());
    }

    private void initAvailableVariants() {
        LOCK.lock();
        try {
            for (int i = 0; i < getParallelism(); i++) {
                String variantId = getVariantIdByIndex(i);
                FaraoLoggerProvider.TECHNICAL_LOGS.info("Filling variants pool with variant '{}'", variantId);
                network.getVariantManager().cloneVariant(initialVariant, variantId);
                boolean isSuccess = variantsQueue.offer(variantId);
                if (!isSuccess) {
                    throw new AssertionError(String.format("Cannot offer variant '%s' in pool. Should not happen", variantId));
                }
            }
            network.getVariantManager().allowVariantMultiThreadAccess(true);
        } finally {
            LOCK.unlock();
        }
    }

    public String getAvailableVariant() throws InterruptedException {
        String polledVariant = variantsQueue.take();
        network.getVariantManager().cloneVariant(initialVariant, polledVariant, true);
        return polledVariant;
    }

    public void releaseUsedVariant(String variantToRelease) throws InterruptedException {
        variantsQueue.put(variantToRelease);
    }

    @Override
    public void close() {
        LOCK.lock();
        try {
            shutdownNow();
            for (int i = 0; i < getParallelism(); i++) {
                String variantId = getVariantIdByIndex(i);
                network.getVariantManager().removeVariant(variantId);
            }
        } finally {
            LOCK.unlock();
        }
    }

    private String getVariantIdByIndex(int index) {
        return initialVariant + " variant modified " + index;
    }
}
