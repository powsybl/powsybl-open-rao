/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.openrao.data.cracapi.RaUsageLimits;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;
import com.powsybl.openrao.searchtreerao.searchtree.parameters.SearchTreeParameters;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class SearchTreeBloomer {
    private final List<NetworkActionCombination> preDefinedNaCombinations;
    private final List<NetworkActionCombinationFilter> networkActionCombinationFilters;
    private SearchTreeInput input;
    private final SearchTreeParameters parameters;

    public SearchTreeBloomer(SearchTreeInput input, SearchTreeParameters parameters) {
        RaUsageLimits raUsageLimits = parameters.getRaLimitationParameters().getOrDefault(input.getOptimizationPerimeter().getMainOptimizationState().getInstant(), new RaUsageLimits());
        this.preDefinedNaCombinations = parameters.getNetworkActionParameters().getNetworkActionCombinations();
        this.networkActionCombinationFilters = List.of(
            new AlreadyAppliedNetworkActionsFilter(),
            new AlreadyTestedCombinationsFilter(preDefinedNaCombinations),
            new MaximumNumberOfRemedialActionsFilter(raUsageLimits.getMaxRa(), input.getOptimizationPerimeter().getMainOptimizationState()),
            new MaximumNumberOfRemedialActionPerTsoFilter(raUsageLimits.getMaxTopoPerTso(), raUsageLimits.getMaxRaPerTso(), input.getOptimizationPerimeter().getMainOptimizationState()),
            new MaximumNumberOfTsosFilter(raUsageLimits.getMaxTso(), input.getOptimizationPerimeter().getMainOptimizationState()),
            new FarFromMostLimitingElementFilter(input.getNetwork(), parameters.getNetworkActionParameters().skipNetworkActionFarFromMostLimitingElements(), parameters.getNetworkActionParameters().getMaxNumberOfBoundariesForSkippingNetworkActions())
        );
        this.input = input;
        this.parameters = parameters;
    }

    /**
     * This method generates a map of NetworkActionCombinations and associated boolean.
     * The networkActionCombinations generated would be available after this leaf inside the tree.
     * They are either individual NetworkAction as defined in the Crac, or predefined
     * combinations of NetworkActions, defined in the SearchTreeRaoParameters and considered as being efficient when
     * activated together.
     * If the associated boolean is false, the combination can be applied while keeping parentLeafRangeActions.
     * If it is true, parentLeafRangeActions must be removed before applying the combination.
     * <p>
     * Moreover, the bloom method ensure that the returned NetworkActionCombinations respect the following rules:
     * <ul>
     * <li>they do not exceed the maximum number of usable remedial actions</li>
     * <li>they do not exceed the maximum number of usable remedial actions (PST & topo) per operator</li>
     * <li>they do not exceed the maximum number of operators</li>
     * <li>they are not too far away from the most limiting CNEC</li>
     * </ul>
     */
    Set<NetworkActionCombination> bloom(Leaf fromLeaf, Set<NetworkAction> networkActions) {

        // preDefined combinations
        Map<NetworkActionCombination, Boolean> networkActionCombinations = preDefinedNaCombinations.stream()
            .distinct()
            .filter(naCombination -> networkActions.containsAll(naCombination.getNetworkActionSet()))
            .collect(Collectors.toMap(naCombination -> naCombination, naCombination -> false));

        // + individual available Network Actions
        final List<NetworkActionCombination> finalNetworkActionCombinations = new ArrayList<>(networkActionCombinations.keySet());
        Map<NetworkActionCombination, Boolean> effectivelyFinalNACombinations = networkActionCombinations;
        networkActions.stream()
            .filter(na ->
                finalNetworkActionCombinations.stream().noneMatch(naCombi -> naCombi.getNetworkActionSet().size() == 1 && naCombi.getNetworkActionSet().contains(na))
            )
            .forEach(ra -> effectivelyFinalNACombinations.put(new NetworkActionCombination(Set.of(ra)), false));
        networkActionCombinations.putAll(effectivelyFinalNACombinations);

        // filters
        for (NetworkActionCombinationFilter networkActionCombinationFilter : networkActionCombinationFilters) {
            networkActionCombinations = networkActionCombinationFilter.filter(networkActionCombinations, fromLeaf);
        }

        return networkActionCombinations.keySet();
    }
    boolean shouldRangeActionsBeRemovedToApplyNa(NetworkActionCombination naCombination, OptimizationResult optimizationResult) {
        Set<String> operators = naCombination.getOperators();
        boolean removeRangeActions = false;
        for (String tso : operators) {
            int naCombinationSize = (int) naCombination.getNetworkActionSet().stream().filter(networkAction -> tso.equals(networkAction.getOperator())).count();
            int numberOfAlreadyActivatedRangeActionsForTso = (int) optimizationResult.getActivatedRangeActions(input.getOptimizationPerimeter().getMainOptimizationState()).stream().filter(ra -> tso.equals(ra.getOperator())).count();
            int numberOfAlreadyAppliedNetworkActionsForTso = (int) optimizationResult.getActivatedNetworkActions().stream().filter(na -> tso.equals(na.getOperator())).count();
            if (numberOfAlreadyAppliedNetworkActionsForTso + numberOfAlreadyActivatedRangeActionsForTso + naCombinationSize > parameters.getRaLimitationParameters().get(input.getOptimizationPerimeter().getMainOptimizationState().getInstant()).getMaxRaPerTso().getOrDefault(tso, Integer.MAX_VALUE)) {
                removeRangeActions = true;
            }
        }

        int naCombinationSize = naCombination.getNetworkActionSet().size();
        int alreadyActivatedNetworkActionsSize = optimizationResult.getActivatedNetworkActions().size();
        removeRangeActions = removeRangeActions || alreadyActivatedNetworkActionsSize + optimizationResult.getActivatedRangeActions(input.getOptimizationPerimeter().getMainOptimizationState()).size() + naCombinationSize > parameters.getRaLimitationParameters().get(input.getOptimizationPerimeter().getMainOptimizationState().getInstant()).getMaxRa();

        Set<String> alreadyActivatedTsos = optimizationResult.getActivatedNetworkActions().stream().map(RemedialAction::getOperator).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> involvedTsos = naCombination.getOperators();
        involvedTsos.addAll(alreadyActivatedTsos);

        removeRangeActions = removeRangeActions || involvedTsos.size() > parameters.getRaLimitationParameters().get(input.getOptimizationPerimeter().getMainOptimizationState().getInstant()).getMaxTso();

        return removeRangeActions;
    }

    boolean hasPreDefinedNetworkActionCombination(NetworkActionCombination naCombination) {
        return this.preDefinedNaCombinations.contains(naCombination);
    }
}
