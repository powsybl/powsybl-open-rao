/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.closed_optimisation_rao.pre_processors;

import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoVariantsPool extends ForkJoinPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(FaraoVariantsPool.class);
    private final BlockingQueue<String> variantsQueue;
    private final Network network;
    private final String initialVariant;

    public FaraoVariantsPool(Network network, String initialVariant, int numberOfAvailableVariants) {
        super(numberOfAvailableVariants);
        this.network = network;
        this.initialVariant = initialVariant;
        this.variantsQueue = new ArrayBlockingQueue<>(numberOfAvailableVariants);
        initAvailableVariants(numberOfAvailableVariants);
    }

    public FaraoVariantsPool(Network network, String initialVariant) {
        this(network, initialVariant, Runtime.getRuntime().availableProcessors());
    }

    private void initAvailableVariants(int numberOfAvailableVariants) {
        for (int i = 0; i < numberOfAvailableVariants; i++) {
            String variantId = initialVariant + " variant modified " + i;
            LOGGER.info("Initializing variants pool with variant '{}'", variantId);
            network.getVariantManager().cloneVariant(initialVariant, variantId, true);
            boolean isSuccess = variantsQueue.offer(variantId);
            if (!isSuccess) {
                LOGGER.error("Cannot offer variant '{}' in pool. Should not happen", variantId);
                throw new AssertionError(String.format("Cannot offer variant '%s' in pool. Should not happen", variantId));
            }
        }
        network.getVariantManager().allowVariantMultiThreadAccess(true);
    }

    public String getAvailableVariant() throws InterruptedException {
        LOGGER.info("Searching for available variant for computation");
        String polledVariant = variantsQueue.take();
        network.getVariantManager().cloneVariant(initialVariant, polledVariant, true);
        LOGGER.info("Return variant '{}'", polledVariant);
        return polledVariant;
    }

    public void releaseUsedVariant(String variantToRelease) throws InterruptedException {
        LOGGER.info("Releasing variant '{}'", variantToRelease);
        variantsQueue.put(variantToRelease);
    }
}
