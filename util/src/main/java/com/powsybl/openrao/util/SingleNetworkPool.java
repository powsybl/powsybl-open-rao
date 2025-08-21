/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.iidm.network.Network;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * A {@code AbstractNetworkPool} implementation that is used when parallelism = 1
 * Instead of creating a (useless) copy of the network object, it uses the network object itself
 * while correctly handling setup and cleanup of variants
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SingleNetworkPool extends AbstractNetworkPool {

    boolean cloneInitialised = false;

    SingleNetworkPool(Network network, String targetVariant) {
        super(network, targetVariant, 1);
        initClones(1);
    }

    @Override
    public void initClones(int desiredNumberOfClones) {
        if (cloneInitialised) {
            return;
        }
        OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Using base network '{}' on variant '{}'", network.getId(), targetVariant);
        network.getVariantManager().setWorkingVariant(targetVariant);
        network.getVariantManager().cloneVariant(networkInitialVariantId, Arrays.asList(stateSaveVariant, workingVariant), true);
        boolean isSuccess = networksQueue.offer(network);
        if (!isSuccess) {
            throw new AssertionError("Cannot offer base network in pool. Should not happen");
        }
        cloneInitialised = true;
    }

    @Override
    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        super.shutdown();
        super.awaitTermination(timeout, unit);
        cleanBaseNetwork();
    }
}
