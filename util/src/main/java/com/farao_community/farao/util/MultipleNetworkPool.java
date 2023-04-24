/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.xml.NetworkXml;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MultipleNetworkPool extends AbstractNetworkPool {

    private int networkNumberOfClones;

    protected MultipleNetworkPool(Network network, String targetVariant, int parallelism) {
        super(network, targetVariant, parallelism);
        this.networkNumberOfClones = 0;
    }

    @Override
    protected void cleanVariants(Network networkClone) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
                .filter(variantId -> !variantId.equals(VariantManagerConstants.INITIAL_VARIANT_ID))
                .filter(variantId -> !variantId.equals(stateSaveVariant))
                .collect(Collectors.toList());
        variantsToBeRemoved.forEach(variantId -> networkClone.getVariantManager().removeVariant(variantId));
    }

//    @Override
//    public void shutdownAndAwaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
//        super.shutdown();
//        super.awaitTermination(timeout, unit);
//    }

    @Override
    public int getNetworkNumberOfClones() {
        // The number of clones includes the original network itself
        return networkNumberOfClones;
    }

    @Override
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

        String initialVariant = network.getVariantManager().getWorkingVariantId();
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
        network.getVariantManager().setWorkingVariant(initialVariant);
    }
}
