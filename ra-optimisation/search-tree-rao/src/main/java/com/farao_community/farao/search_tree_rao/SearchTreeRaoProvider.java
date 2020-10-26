/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResult;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResult;
import com.farao_community.farao.data.crac_result_extensions.RangeActionResultExtension;
import com.farao_community.farao.rao_api.*;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class SearchTreeRaoProvider implements RaoProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoProvider.class);
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_VARIANT = "preventive-variant";
    private static final String CURATIVE_VARIANT = "curative-variant";

    @Override
    public String getName() {
        return SEARCH_TREE_RAO;
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        RaoUtil.initData(raoInput, parameters);

        if (raoInput.getOptimizedState() != null) {
            return CompletableFuture.completedFuture(new Tree().run(RaoData.create(raoInput), parameters).join());
        }

        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_VARIANT);

        List<List<State>> perimeters = RaoUtil.createPerimeters(raoInput.getCrac(), network, raoInput.getCrac().getPreventiveState());
        List<State> preventivePerimeter = perimeters.remove(0);

        RaoInput preventiveRaoInput = RaoInput.createOnState(raoInput.getNetwork(), raoInput.getCrac(), preventivePerimeter.get(0))
            .withNetworkVariantId(PREVENTIVE_VARIANT)
            .withPerimeter(new HashSet<>(preventivePerimeter))
            .withGlskProvider(raoInput.getGlskProvider())
            .withRefProg(raoInput.getReferenceProgram())
            .build();
        RaoResult preventiveRaoResult = new Tree().run(RaoData.create(preventiveRaoInput), parameters).join();

        applyPreventiveRemedialActions(raoInput.getNetwork(), raoInput.getCrac(),
            preventiveRaoResult.getPostOptimVariantIdPerStateId().get(raoInput.getCrac().getPreventiveState().getId()));

        List<RaoResult> curativeResults = new ArrayList<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_VARIANT);
        network.getVariantManager().cloneVariant(PREVENTIVE_VARIANT, CURATIVE_VARIANT);
        network.getVariantManager().setWorkingVariant(CURATIVE_VARIANT);
        // For now only one curative computation at a time
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_VARIANT, 1)) {
            perimeters.forEach(perimeter ->
                networkPool.submit(() -> {
                    try {
                        Network networkClone = networkPool.getAvailableNetwork();
                        RaoInput curativeRaoInput = RaoInput.createOnState(networkClone, raoInput.getCrac(), perimeter.get(0))
                            .withBaseCracVariantId(preventiveRaoResult.getPostOptimVariantIdPerStateId().get(raoInput.getCrac().getPreventiveState().getId()))
                            .withNetworkVariantId(CURATIVE_VARIANT)
                            .withPerimeter(new HashSet<>(perimeter))
                            .withGlskProvider(raoInput.getGlskProvider())
                            .withRefProg(raoInput.getReferenceProgram())
                            .build();

                        curativeResults.add(new Tree().run(RaoData.create(curativeRaoInput), parameters).join());
                        networkPool.releaseUsedNetwork(networkClone);
                    } catch (InterruptedException | NotImplementedException | FaraoException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            networkPool.shutdown();
            networkPool.awaitTermination(24, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return CompletableFuture.completedFuture(mergeRaoResults(preventiveRaoResult, curativeResults));
    }

    private static void applyPreventiveRemedialActions(Network network, Crac crac, String cracVariantId) {
        String preventiveStateId = crac.getPreventiveState().getId();
        crac.getNetworkActions().forEach(na -> applyNetworkAction(na, network, cracVariantId, preventiveStateId));
        crac.getRangeActions().forEach(ra -> applyRangeAction(ra, network, cracVariantId, preventiveStateId));
    }

    private static void applyNetworkAction(NetworkAction networkAction, Network network, String cracVariantId, String preventiveStateId) {
        NetworkActionResultExtension resultExtension = networkAction.getExtension(NetworkActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error(String.format("Could not find results on network action %s", networkAction.getId()));
        } else {
            NetworkActionResult networkActionResult = resultExtension.getVariant(cracVariantId);
            if (networkActionResult != null) {
                if (networkActionResult.isActivated(preventiveStateId)) {
                    networkAction.apply(network);
                }
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on network action %s", cracVariantId, networkAction.getId()));
            }
        }
    }

    private static void applyRangeAction(RangeAction rangeAction, Network network, String cracVariantId, String preventiveStateId) {
        RangeActionResultExtension resultExtension = rangeAction.getExtension(RangeActionResultExtension.class);
        if (resultExtension == null) {
            LOGGER.error(String.format("Could not find results on range action %s", rangeAction.getId()));
        } else {
            RangeActionResult rangeActionResult = resultExtension.getVariant(cracVariantId);
            if (rangeActionResult != null) {
                rangeAction.apply(network, rangeActionResult.getSetPoint(preventiveStateId));
            } else {
                LOGGER.error(String.format("Could not find results for variant %s on range action %s", cracVariantId, rangeAction.getId()));
            }
        }
    }

    private RaoResult mergeRaoResults(RaoResult preventiveRaoResult, List<RaoResult> curativeRaoResults) {
        curativeRaoResults.forEach(curativeRaoResult -> {
            if (curativeRaoResult.getStatus().equals(RaoResult.Status.FAILURE)) {
                preventiveRaoResult.setStatus(RaoResult.Status.FAILURE);
            }
            curativeRaoResult.getPostOptimVariantIdPerStateId().forEach((k, v) -> preventiveRaoResult.getPostOptimVariantIdPerStateId().put(k, v));
        });
        return preventiveRaoResult;
    }
}
