/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.perimeter_rao;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.UsageMethod;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.farao_community.farao.rao_api.*;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.google.auto.service.AutoService;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(RaoProvider.class)
public class PerimeterRaoProvider implements RaoProvider {
    private static final String SEARCH_TREE_RAO = "SearchTreeRao";
    private static final String PREVENTIVE_VARIANT = "preventive-variant";
    private static final String CURATIVE_VARIANT = "curative-variant";

    @Override
    public String getName() {
        return "PerimeterRao";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CompletableFuture<RaoResult> run(RaoInput raoInput, RaoParameters parameters) {
        Network network = raoInput.getNetwork();
        network.getVariantManager().cloneVariant(network.getVariantManager().getWorkingVariantId(), PREVENTIVE_VARIANT);

        List<List<State>> perimeters = createPerimeters(raoInput.getCrac(), network, raoInput.getCrac().getPreventiveState());
        List<State> preventivePerimeter = perimeters.remove(0);

        RaoInput.RaoInputBuilder preventiveRaoInputBuilder = RaoInput.builder()
            .withNetwork(raoInput.getNetwork())
            .withCrac(raoInput.getCrac())
            .withVariantId(PREVENTIVE_VARIANT)
            .withOptimizedState(preventivePerimeter.get(0))
            .withPerimeter(new HashSet<>(preventivePerimeter));
        raoInput.getGlskProvider().ifPresent(preventiveRaoInputBuilder::withGlskProvider);
        raoInput.getReferenceProgram().ifPresent(preventiveRaoInputBuilder::withRefProg);
        RaoInput preventiveRaoInput = preventiveRaoInputBuilder.build();
        RaoResult preventiveRaoResult = Rao.find(SEARCH_TREE_RAO).run(preventiveRaoInput, parameters);

        List<RaoResult> curativeResults = new ArrayList<>();
        network.getVariantManager().setWorkingVariant(PREVENTIVE_VARIANT);
        try (FaraoNetworkPool networkPool = new FaraoNetworkPool(network, CURATIVE_VARIANT, 5)) {
            perimeters.forEach(perimeter ->
                networkPool.submit(() -> {
                    try {
                        Network networkClone = networkPool.getAvailableNetwork();
                        RaoInput.RaoInputBuilder curativeRaoInputBuilder = RaoInput.builder()
                            .withNetwork(networkClone)
                            .withCrac(raoInput.getCrac())
                            .withVariantId(CURATIVE_VARIANT)
                            .withOptimizedState(perimeter.get(0))
                            .withPerimeter(new HashSet<>(perimeter));
                        raoInput.getGlskProvider().ifPresent(curativeRaoInputBuilder::withGlskProvider);
                        raoInput.getReferenceProgram().ifPresent(curativeRaoInputBuilder::withRefProg);
                        RaoInput curativeRaoInput = curativeRaoInputBuilder.build();

                        curativeResults.add(Rao.find(SEARCH_TREE_RAO).run(curativeRaoInput, parameters));
                        networkPool.releaseUsedNetwork(networkClone);
                    } catch (InterruptedException | NotImplementedException e) {
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

    public static List<List<State>> createPerimeters(Crac crac, Network network, State startingState) {
        List<List<State>> perimeters = new ArrayList<>();
        State preventiveState = crac.getPreventiveState();

        if (startingState.equals(preventiveState)) {
            List<State> preventivePerimeter = new ArrayList<>();
            preventivePerimeter.add(preventiveState);
            perimeters.add(preventivePerimeter);

            List<State> currentPerimeter;
            for (Contingency contingency : crac.getContingencies()) {
                currentPerimeter = preventivePerimeter;
                for (State state : crac.getStates(contingency)) {
                    if (anyAvailableRemedialAction(crac, network, state)) {
                        currentPerimeter = new ArrayList<>();
                        perimeters.add(currentPerimeter);
                    }
                    currentPerimeter.add(state);
                }
            }
        } else {
            throw new NotImplementedException("Cannot create perimeters if starting state is different from preventive state");
        }

        return perimeters;
    }

    private static boolean anyAvailableRemedialAction(Crac crac, Network network, State state) {
        return !crac.getNetworkActions(network, state, UsageMethod.AVAILABLE).isEmpty() ||
            !crac.getRangeActions(network, state, UsageMethod.AVAILABLE).isEmpty();
    }

    private RaoResult mergeRaoResults(RaoResult preventiveRaoResult, List<RaoResult> curativeRaoResults) {
        return preventiveRaoResult;
    }
}
