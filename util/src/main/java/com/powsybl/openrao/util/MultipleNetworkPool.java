/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.serde.NetworkSerDe;
import com.powsybl.openrao.commons.OpenRaoException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MultipleNetworkPool extends AbstractNetworkPool {

    private int networkNumberOfClones = 0;

    protected MultipleNetworkPool(Network network, String targetVariant, int parallelism, boolean initClones) {
        super(network, targetVariant, parallelism);
        if (initClones) {
            initClones(parallelism);
        }
    }

    @Override
    protected void cleanVariants(Network networkClone) {
        List<String> variantsToBeRemoved = networkClone.getVariantManager().getVariantIds().stream()
            .filter(variantId -> !variantId.equals(VariantManagerConstants.INITIAL_VARIANT_ID))
            .filter(variantId -> !variantId.equals(stateSaveVariant))
            .toList();
        variantsToBeRemoved.forEach(variantId -> networkClone.getVariantManager().removeVariant(variantId));
    }

    @Override
    public int getNetworkNumberOfClones() {
        // The number of clones includes the original network itself
        return networkNumberOfClones;
    }

    @Override
    public void initClones(int desiredNumberOfClones) {
        int requiredClones = Math.min(getParallelism(), desiredNumberOfClones);
        int clonesToAdd = requiredClones - networkNumberOfClones;

        if (clonesToAdd == 0) {
            return;
        }

        TECHNICAL_LOGS.debug("Filling network pool with {} new cop{} of network {} on variant {}", clonesToAdd, clonesToAdd == 1 ? "y" : "ies", network.getId(), targetVariant);

        String initialVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().setWorkingVariant(targetVariant);

        AtomicInteger remainingClones = new AtomicInteger(requiredClones);
        List<ForkJoinTask<Network>> tasks = new ArrayList<>();
        try {
            for (int i = networkNumberOfClones; i < requiredClones; i++) {
                int finalI = i;
                tasks.add(this.submit(() -> createNetworkCopy(finalI, remainingClones)));
            }
            for (ForkJoinTask<Network> task : tasks) {
                try {
                    boolean isSuccess = networksQueue.offer(task.get());
                    if (!isSuccess) {
                        throw new OpenRaoException(String.format("Cannot offer copy n°'%d' in pool. Should not happen", networkNumberOfClones + 1));
                    } else {
                        networkNumberOfClones++;
                    }
                } catch (ExecutionException e) {
                    throw new OpenRaoException(e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        network.getVariantManager().setWorkingVariant(initialVariant);
    }

    private Network createNetworkCopy(int finalI, AtomicInteger remainingClones) {
        TECHNICAL_LOGS.debug("Copy n°{}", finalI + 1);
        Network copy = NetworkSerDe.copy(network);
        // The initial network working variant is VariantManagerConstants.INITIAL_VARIANT_ID
        // in cloned network, so we need to copy it again.
        copy.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, Arrays.asList(stateSaveVariant, workingVariant), true);
        remainingClones.decrementAndGet();
        return copy;
    }
}
