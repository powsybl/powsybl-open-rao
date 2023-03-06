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
import com.farao_community.farao.data.crac_api.Identifiable;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.objective_function_evaluator.ObjectiveFunction;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.GlobalOptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.commons.optimization_perimeters.OptimizationPerimeter;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.castor.algorithm.BasecaseScenario;
import com.farao_community.farao.search_tree_rao.castor.algorithm.ContingencyScenario;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.Leaf;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
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

        Map<String, Double> virtualCosts = getVirtualCostDetailed(prePerimeterObjectiveFunctionResult);

        BUSINESS_LOGS.info(prefix +  "cost = {} (functional: {}, virtual: {}{})",
            formatDouble(prePerimeterObjectiveFunctionResult.getCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getFunctionalCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getVirtualCost()),
            virtualCosts.isEmpty() ? "" : " " + virtualCosts);

        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS,
            sensitivityAnalysisResult,
            raoParameters.getObjectiveFunction(),
            numberOfLoggedLimitingElements);
    }

    public static void logRangeActions(FaraoLogger logger,
                                       Leaf leaf,
                                       OptimizationPerimeter
                                           optimizationContext, String prefix) {

        boolean globalPstOptimization = optimizationContext instanceof GlobalOptimizationPerimeter;

        List<String> rangeActionSetpoints = optimizationContext.getRangeActionOptimizationStates().stream().flatMap(state ->
            leaf.getActivatedRangeActions(state).stream().map(rangeAction -> {
                double rangeActionValue = rangeAction instanceof PstRangeAction ? leaf.getOptimizedTap((PstRangeAction) rangeAction, state) :
                    leaf.getOptimizedSetpoint(rangeAction, state);
                return globalPstOptimization ? format("%s@%s: %.0f", rangeAction.getName(), state.getId(), rangeActionValue) :
                    format("%s: %.0f", rangeAction.getName(), rangeActionValue);
            })).collect(Collectors.toList());

        boolean isRangeActionSetPointEmpty = rangeActionSetpoints.isEmpty();
        if (isRangeActionSetPointEmpty) {
            logger.info("{}No range actions activated", prefix == null ? "" : prefix);
        } else {
            logger.info("{}range action(s): {}", prefix == null ? "" : prefix, String.join(", ", rangeActionSetpoints));
        }
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, OptimizationResult optimizationResult, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, optimizationResult, optimizationResult, null, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, PrePerimeterResult prePerimeterResult, Set<State> states, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, states, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(FaraoLogger logger, PrePerimeterResult prePerimeterResult, RaoParameters.ObjectiveFunction objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, null, objectiveFunction, numberOfLoggedElements);
    }

    private static void logMostLimitingElementsResults(FaraoLogger logger,
                                                       ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       RaoParameters.ObjectiveFunction objectiveFunction,
                                                       int numberOfLoggedElements) {
        getMostLimitingElementsResults(objectiveFunctionResult, flowResult, states, objectiveFunction, numberOfLoggedElements)
            .forEach(logger::info);
    }

    static List<String> getMostLimitingElementsResults(ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       RaoParameters.ObjectiveFunction objectiveFunction,
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
                                               RaoParameters.ObjectiveFunction objectiveFunction) {
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
                                               RaoParameters.ObjectiveFunction objectiveFunction,
                                               int numberOfLoggedElements) {
        getMostLimitingElementsResults(basecaseScenario, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunction, numberOfLoggedElements)
            .forEach(logger::info);
    }

    public static List<String> getMostLimitingElementsResults(BasecaseScenario basecaseScenario,
                                                       OptimizationResult basecaseOptimResult,
                                                       Set<ContingencyScenario> contingencyScenarios,
                                                       Map<State, OptimizationResult> contingencyOptimizationResults,
                                                       RaoParameters.ObjectiveFunction objectiveFunction,
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

    public static void logFailedOptimizationSummary(FaraoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        logger.info("Scenario \"{}\": {}", scenarioName, raResult);
    }

    public static void logOptimizationSummary(FaraoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions, ObjectiveFunctionResult preOptimObjectiveFunctionResult, ObjectiveFunctionResult finalObjective) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        Map<String, Double> finalVirtualCostDetailed = getVirtualCostDetailed(finalObjective);
        boolean isPreOptimNull = preOptimObjectiveFunctionResult == null;
        Map<String, Double> initialVirtualCostDetailed = isPreOptimNull ? Collections.emptyMap() : getVirtualCostDetailed(preOptimObjectiveFunctionResult);
        String initialCostString;
        if (initialVirtualCostDetailed.isEmpty()) {
            initialCostString = isPreOptimNull ? "" :
                String.format("initial cost = %s (functional: %s, virtual: %s), ", formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost()), formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost()), formatDouble(preOptimObjectiveFunctionResult.getVirtualCost()));
        } else {
            initialCostString = String.format("initial cost = %s (functional: %s, virtual: %s %s), ", formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost()), formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost()), formatDouble(preOptimObjectiveFunctionResult.getVirtualCost()), initialVirtualCostDetailed);
        }
        logger.info("Scenario \"{}\": {}{}, cost {} = {} (functional: {}, virtual: {}{})", scenarioName, initialCostString, raResult, OptimizationState.afterOptimizing(optimizedState),
            formatDouble(finalObjective.getCost()), formatDouble(finalObjective.getFunctionalCost()), formatDouble(finalObjective.getVirtualCost()), finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed);
    }

    public static String getRaResult(Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions) {
        long activatedNetworkActions = networkActions.size();
        long activatedRangeActions = rangeActions.size();
        String networkActionsNames = StringUtils.join(networkActions.stream().map(Identifiable::getName).collect(Collectors.toSet()), ", ");

        Set<String> rangeActionsSet = new HashSet<>();
        rangeActions.forEach((key, value) -> rangeActionsSet.add(format("%s: %.0f", key.getName(), value)));
        String rangeActionsNames = StringUtils.join(rangeActionsSet, ", ");

        if (activatedNetworkActions + activatedRangeActions == 0) {
            return "no remedial actions activated";
        } else if (activatedNetworkActions > 0 && activatedRangeActions == 0) {
            return String.format("%s network action(s) activated : %s", activatedNetworkActions, networkActionsNames);
        } else if (activatedRangeActions > 0 && activatedNetworkActions == 0) {
            return String.format("%s range action(s) activated : %s", activatedRangeActions, rangeActionsNames);
        } else {
            return String.format("%s network action(s) and %s range action(s) activated : %s and %s",
                activatedNetworkActions, activatedRangeActions, networkActionsNames, rangeActionsNames);
        }
    }

    public static String getScenarioName(State state) {
        Optional<Contingency> optionalContingency = state.getContingency();
        return optionalContingency.isEmpty() ? "preventive" : optionalContingency.get().getName();
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

    /**
     * For a given virtual-cost-name, if its associated virtual cost is positive, this method will return a map containing
     * these information to be used in the Rao logs
     */
    public static Map<String, Double> getVirtualCostDetailed(ObjectiveFunctionResult objectiveFunctionResult) {
        return objectiveFunctionResult.getVirtualCostNames().stream()
            .filter(virtualCostName -> objectiveFunctionResult.getVirtualCost(virtualCostName) > 1e-6)
            .collect(Collectors.toMap(Function.identity(),
                name -> Math.round(objectiveFunctionResult.getVirtualCost(name) * 100.0) / 100.0));
    }
}
