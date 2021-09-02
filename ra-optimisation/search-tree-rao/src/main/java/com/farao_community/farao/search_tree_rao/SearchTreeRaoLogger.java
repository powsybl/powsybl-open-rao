/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_commons.result_api.FlowResult;
import com.farao_community.farao.rao_commons.result_api.ObjectiveFunctionResult;
import com.farao_community.farao.rao_commons.result_api.OptimizationResult;
import com.farao_community.farao.rao_commons.result_api.PrePerimeterResult;
import com.farao_community.farao.search_tree_rao.state_tree.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.state_tree.ContingencyScenario;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
final class SearchTreeRaoLogger {

    private SearchTreeRaoLogger() {
    }

    static void logRangeActions(Leaf leaf, Set<RangeAction> rangeActions) {
        logRangeActions(leaf, rangeActions, null);
    }

    static void logRangeActions(Leaf leaf, Set<RangeAction> rangeActions, String prefix) {
        StringBuilder rangeActionMsg = new StringBuilder();
        if (prefix != null) {
            rangeActionMsg.append(prefix).append(" - ");
        }
        rangeActionMsg.append("Range action(s): ");
        rangeActions.forEach(rangeAction -> {
            String rangeActionName = rangeAction.getName();
            int rangeActionTap = leaf.getOptimizedTap((PstRangeAction) rangeAction);
            rangeActionMsg
                .append(format("%s: %d", rangeActionName, rangeActionTap))
                .append(" , ");
        });
        String rangeActionsLog = rangeActionMsg.toString();
        SearchTree.LOGGER.info(rangeActionsLog);
    }

    static void logMostLimitingElementsResults(OptimizationResult optimizationResult, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(optimizationResult, optimizationResult, null, objectiveFunction, numberOfLoggedElements);
    }

    static void logMostLimitingElementsResults(PrePerimeterResult prePerimeterResult, Set<State> states, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(prePerimeterResult, prePerimeterResult, states, objectiveFunction, numberOfLoggedElements);
    }

    static void logMostLimitingElementsResults(PrePerimeterResult prePerimeterResult, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(prePerimeterResult, prePerimeterResult, null, objectiveFunction, numberOfLoggedElements);
    }

    private static void logMostLimitingElementsResults(ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       RaoParameters.ObjectiveFunction objectiveFunction,
                                                       int numberOfLoggedElements) {
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        List<FlowCnec> sortedCnecs = getMostLimitingElements(objectiveFunctionResult, states, numberOfLoggedElements);

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, unit) : flowResult.getMargin(cnec, unit);

            String margin = new DecimalFormat("#0.00").format(cnecMargin);
            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? "relative " : "";
            String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format("(PTDF %f)", flowResult.getPtdfZonalSum(cnec)) : "";
            SearchTree.LOGGER.info("Limiting element #{}: element {} at state {} with a {}margin of {} {} {}",
                i + 1,
                cnecNetworkElementName,
                cnecStateId,
                isRelativeMargin,
                margin,
                unit,
                ptdfIfRelative);
        }
    }

    public static void logMostLimitingElementsResults(BasecaseScenario basecaseScenario,
                                                      OptimizationResult basecaseOptimResult,
                                                      Set<ContingencyScenario> contingencyScenarios,
                                                      Map<State, OptimizationResult> contingencyOptimizationResults,
                                                      RaoParameters.ObjectiveFunction objectiveFunction,
                                                      int numberOfLoggedElements) {
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        Map<FlowCnec, Double> mostLimitingElementsAndMargins =
            getMostLimitingElementsAndMargins(basecaseOptimResult, basecaseScenario.getAllStates(), unit, relativePositiveMargins, numberOfLoggedElements);

        contingencyScenarios.forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            automatonState.ifPresent(state -> mostLimitingElementsAndMargins.putAll(
                getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(state), Set.of(state), unit, relativePositiveMargins, numberOfLoggedElements)
            ));
            mostLimitingElementsAndMargins.putAll(
                getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(contingencyScenario.getCurativeState()), Set.of(contingencyScenario.getCurativeState()), unit, relativePositiveMargins, numberOfLoggedElements)
            );
        });

        List<FlowCnec> sortedCnecs = mostLimitingElementsAndMargins.keySet().stream()
            .sorted(Comparator.comparing(mostLimitingElementsAndMargins::get))
            .collect(Collectors.toList());
        sortedCnecs = sortedCnecs.subList(0, Math.min(sortedCnecs.size(), numberOfLoggedElements));

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = mostLimitingElementsAndMargins.get(cnec);

            String margin = new DecimalFormat("#0.00").format(cnecMargin);
            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? "relative " : "";
            SearchTree.LOGGER.info("Limiting element #{}: element {} at state {} with a {}margin of {} {}",
                i + 1,
                cnecNetworkElementName,
                cnecStateId,
                isRelativeMargin,
                margin,
                unit);
        }
    }

    private static List<FlowCnec> getMostLimitingElements(ObjectiveFunctionResult objectiveFunctionResult,
                                                          Set<State> states,
                                                          int maxNumberOfElements) {
        if (states == null) {
            return objectiveFunctionResult.getMostLimitingElements(maxNumberOfElements);
        } else {
            List<FlowCnec> cnecs = objectiveFunctionResult.getMostLimitingElements(Integer.MAX_VALUE)
                .stream().filter(cnec -> states.contains(cnec.getState()))
                .collect(Collectors.toList());
            cnecs = cnecs.subList(0, Math.min(cnecs.size(), maxNumberOfElements));
            return cnecs;
        }
    }

    private static Map<FlowCnec, Double> getMostLimitingElementsAndMargins(OptimizationResult optimizationResult,
                                                                           Set<State> states,
                                                                           Unit unit,
                                                                           boolean relativePositiveMargins,
                                                                           int maxNumberOfElements) {
        Map<FlowCnec, Double> mostLimitingElementsAndMargins = new HashMap<>();
        List<FlowCnec> cnecs = getMostLimitingElements(optimizationResult, states, maxNumberOfElements);
        cnecs.forEach(cnec -> {
            double cnecMargin = relativePositiveMargins ? optimizationResult.getRelativeMargin(cnec, unit) : optimizationResult.getMargin(cnec, unit);
            mostLimitingElementsAndMargins.put(cnec, cnecMargin);
        });
        return mostLimitingElementsAndMargins;
    }
}
