/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.iidm.network.Network;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public abstract class AbstractNetworkPool extends ForkJoinPool implements AutoCloseable {
    public static AbstractNetworkPool create(Network network, String targetVariant, int parallelism) {
        if (parallelism == 1) {
            return new SingleNetworkPool(network, targetVariant);
        } else {
            return new MultipleNetworkPool(network, targetVariant, parallelism);
        }
    }

    protected AbstractNetworkPool(int parallelism) {
        super(parallelism);
    }

    abstract public Network getAvailableNetwork() throws InterruptedException;

    abstract public void releaseUsedNetwork(Network networkToRelease) throws InterruptedException;

    abstract public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    public void close() {
        shutdownNow();
    }
}
