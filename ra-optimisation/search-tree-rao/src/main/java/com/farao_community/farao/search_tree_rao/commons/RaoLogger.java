/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.logs.FaraoLogger;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_api.parameters.ObjectiveFunctionParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.castor.algorithm.ContingencyScenario;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.Leaf;

import java.util.*;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_LOGS;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoLogger {
    private RaoLogger() {
    }

    public static void logSensitivityAnalysisResults(String prefix,
                                                     ObjectiveFunction objectiveFunction,
                                                     RangeActionActivationResult rangeActionActivationResult,
                                                     PrePerimeterResult sensitivityAnalysisResult,
                                                     RaoParameters raoParameters,
                                                     int numberOfLoggedLimitingElements) {

        if (!BUSINESS_LOGS.isInfoEnabled()) {
            return;
        }

        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(sensitivityAnalysisResult, rangeActionActivationResult,
                sensitivityAnalysisResult, sensitivityAnalysisResult.getSensitivityStatus());

        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {})",
                formatDouble(prePerimeterObjectiveFunctionResult.getCost()),
                formatDouble(prePerimeterObjectiveFunctionResult.getFunctionalCost()),
                formatDouble(prePerimeterObjectiveFunctionResult.getVirtualCost()));

        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS,
                sensitivityAnalysisResult,
                raoParameters.getObjectiveFunctionType(),
                numberOfLoggedLimitingElements);
    }

    public static void logRangeActions(FaraoLogger logger,
                                       Leaf leaf,
                                       OptimizationPerimeter optimizationContext) {
        logRangeActions(logger, leaf, optimizationContext, null);
    }

    public static void logRangeActions(FaraoLogger logger,
                                       Leaf leaf,
                                       OptimizationPerimeter
                                               optimizationContext, String prefix) {

        boolean globalPstOptimization = optimizationContext instanceof GlobalOptimizationPerimeter;

        String rangeActionSetpoints = optimizationContext.getRangeActionsPerState().entrySet().stream()
                .flatMap(eState -> eState.getValue().stream().map(rangeAction -> {
                    double rangeActionValue;
                    if (rangeAction instanceof PstRangeAction) {
                        rangeActionValue = leaf.getOptimizedTap((PstRangeAction) rangeAction, eState.getKey());
                    } else {
                        rangeActionValue = leaf.getOptimizedSetpoint(rangeAction, eState.getKey());
                    }
                    if (globalPstOptimization) {
                        return format("%s@%s: %.0f", rangeAction.getName(), eState.getKey().getId(), rangeActionValue);
                    } else {
                        return format("%s: %.0f", rangeAction.getName(), rangeActionValue);
                    }
                }))
                .collect(Collectors.joining(", "));

        logger.info("{}range action(s): {}", prefix == null ? "" : prefix, rangeActionSetpoints);
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, OptimizationResult optimizationResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, optimizationResult, optimizationResult, null, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, PrePerimeterResult prePerimeterResult, Set<State> states, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, states, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, PrePerimeterResult prePerimeterResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, null, objectiveFunction, numberOfLoggedElements);
    }

    private static void logMostLimitingElementsResults(FaraoLogger logger,
                                                       ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                       int numberOfLoggedElements) {
        getMostLimitingElementsResults(objectiveFunctionResult, flowResult, states, objectiveFunction, numberOfLoggedElements)
                .forEach(logger::info);
    }

    static List<String> getMostLimitingElementsResults(ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                       int numberOfLoggedElements) {
        List<String> summary = new ArrayList<>();
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        List<FlowCnec> sortedCnecs = getMostLimitingElements(objectiveFunctionResult, states, numberOfLoggedElements);

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, unit) : flowResult.getMargin(cnec, unit);

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            Side mostConstrainedSide = getMostConstrainedSide(cnec, flowResult, objectiveFunction);
            String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format(" (PTDF %f)", flowResult.getPtdfZonalSum(cnec, mostConstrainedSide)) : "";
            summary.add(String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s%s, element %s at state %s, CNEC ID = \"%s\"",
                    i + 1,
                    isRelativeMargin,
                    cnecMargin,
                    unit,
                    ptdfIfRelative,
                    cnecNetworkElementName,
                    cnecStateId,
                    cnec.getId()));
        }
        return summary;
    }

    private static Side getMostConstrainedSide(FlowCnec cnec,
                                               FlowResult flowResult,
                                               ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
        if (cnec.getMonitoredSides().size() == 1) {
            return cnec.getMonitoredSides().iterator().next();
        }
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();
        double marginLeft = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, Side.LEFT, unit) : flowResult.getMargin(cnec, Side.LEFT, unit);
        double marginRight = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, Side.RIGHT, unit) : flowResult.getMargin(cnec, Side.RIGHT, unit);
        return marginRight < marginLeft ? Side.RIGHT : Side.LEFT;
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger,
                                                      BasecaseScenario basecaseScenario,
                                                      OptimizationResult basecaseOptimResult,
                                                      Set<ContingencyScenario> contingencyScenarios,
                                                      Map<State, OptimizationResult> contingencyOptimizationResults,
                                                      ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                      int numberOfLoggedElements) {
        getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunction, numberOfLoggedElements)
                .forEach(logger::info);
    }

    public static List<String> getMostLimitingElementsResults(BasecaseScenario basecaseScenario,
                                                              OptimizationResult basecaseOptimResult,
                                                              Set<ContingencyScenario> contingencyScenarios,
                                                              Map<State, OptimizationResult> contingencyOptimizationResults,
                                                              ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                              int numberOfLoggedElements) {
        List<String> summary = new ArrayList<>();
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

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            summary.add(String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s, element %s at state %s, CNEC ID = \"%s\"",
                    i + 1,
                    isRelativeMargin,
                    cnecMargin,
                    unit,
                    cnecNetworkElementName,
                    cnecStateId,
                    cnec.getId()));
        }
        return summary;
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

    public static void logOptimizationSummary(FaraoLogger logger, State optimizedState, long activatedNetworkActions, long activatedRangeActions, Double initialFunctionalCost, Double initialVirtualCost, ObjectiveFunctionResult finalObjective) {
        Optional<Contingency> optionalContingency = optimizedState.getContingency();
        String scenarioName = optionalContingency.isEmpty() ? "preventive" : optionalContingency.get().getName();
        String raResult = "";
        if (activatedNetworkActions + activatedRangeActions == 0) {
            raResult = "no remedial actions activated";
        } else if (activatedNetworkActions > 0 && activatedRangeActions == 0) {
            raResult = String.format("%s network action(s) activated", activatedNetworkActions);
        } else if (activatedRangeActions > 0 && activatedNetworkActions == 0) {
            raResult = String.format("%s range action(s) activated", activatedRangeActions);
        } else {
            raResult = String.format("%s network action(s) and %s range action(s) activated", activatedNetworkActions, activatedRangeActions);
        }
        String initialCostString = initialFunctionalCost == null || initialVirtualCost == null ? "" :
                String.format("initial cost = %s (functional: %s, virtual: %s), ", formatDouble(initialFunctionalCost + initialVirtualCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost));

        logger.info("Scenario \"{}\": {}{}, cost {} = {} (functional: {}, virtual: {})", scenarioName, initialCostString, raResult, OptimizationState.afterOptimizing(optimizedState),
                formatDouble(finalObjective.getCost()), formatDouble(finalObjective.getFunctionalCost()), formatDouble(finalObjective.getVirtualCost()));
    }

    public static void logFailedOptimizationSummary(FaraoLogger logger, State optimizedState, long activatedNetworkActions, long activatedRangeActions) {
        Optional<Contingency> optionalContingency = optimizedState.getContingency();
        String scenarioName = optionalContingency.isEmpty() ? "preventive" : optionalContingency.get().getName();
        String raResult = "";
        if (activatedNetworkActions + activatedRangeActions == 0) {
            raResult = "no remedial actions activated";
        } else if (activatedNetworkActions > 0 && activatedRangeActions == 0) {
            raResult = String.format("%s network action(s) activated", activatedNetworkActions);
        } else if (activatedRangeActions > 0 && activatedNetworkActions == 0) {
            raResult = String.format("%s range action(s) activated", activatedRangeActions);
        } else {
            raResult = String.format("%s network action(s) and %s range action(s) activated", activatedNetworkActions, activatedRangeActions);
        }
        logger.info("Scenario \"{}\": {}", scenarioName, raResult);
    }

    public static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }
}
