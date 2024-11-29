/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.GlobalOptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.result.api.*;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class RaoLogger {
    private RaoLogger() {
    }

    public static void logSensitivityAnalysisResults(String prefix,
                                                     ObjectiveFunction objectiveFunction,
                                                     PrePerimeterResult sensitivityAnalysisResult,
                                                     RaoParameters raoParameters,
                                                     int numberOfLoggedLimitingElements) {

        if (!BUSINESS_LOGS.isInfoEnabled()) {
            return;
        }

        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(sensitivityAnalysisResult);

        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {})",
            formatDoubleBasedOnMargin(prePerimeterObjectiveFunctionResult.getCost(), -prePerimeterObjectiveFunctionResult.getCost()),
            formatDoubleBasedOnMargin(prePerimeterObjectiveFunctionResult.getFunctionalCost(), -prePerimeterObjectiveFunctionResult.getCost()),
            formatDoubleBasedOnMargin(prePerimeterObjectiveFunctionResult.getVirtualCost(), -prePerimeterObjectiveFunctionResult.getCost()));

        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS,
            sensitivityAnalysisResult,
            raoParameters.getObjectiveFunctionParameters().getType(),
            raoParameters.getObjectiveFunctionParameters().getUnit(),
            numberOfLoggedLimitingElements);
    }

    public static void logRangeActions(OpenRaoLogger logger,
                                       Leaf leaf,
                                       OptimizationPerimeter
                                           optimizationContext, String prefix) {

        boolean globalPstOptimization = optimizationContext instanceof GlobalOptimizationPerimeter;

        List<String> rangeActionSetpoints = optimizationContext.getRangeActionOptimizationStates().stream().flatMap(state ->
            leaf.getActivatedRangeActions(state).stream().map(rangeAction -> {
                double rangeActionValue = rangeAction instanceof PstRangeAction pstRangeAction ? leaf.getOptimizedTap(pstRangeAction, state) :
                    leaf.getOptimizedSetpoint(rangeAction, state);
                return globalPstOptimization ? format("%s@%s: %.0f", rangeAction.getName(), state.getId(), rangeActionValue) :
                    format("%s: %.0f", rangeAction.getName(), rangeActionValue);
            })).toList();

        boolean isRangeActionSetPointEmpty = rangeActionSetpoints.isEmpty();
        if (isRangeActionSetPointEmpty) {
            logger.info("{}No range actions activated", prefix == null ? "" : prefix);
        } else {
            logger.info("{}range action(s): {}", prefix == null ? "" : prefix, String.join(", ", rangeActionSetpoints));
        }
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, OptimizationResult optimizationResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, Unit unit, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, optimizationResult, optimizationResult, null, objectiveFunction, unit, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, PrePerimeterResult prePerimeterResult, Set<State> states, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, Unit unit, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, states, objectiveFunction, unit, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, PrePerimeterResult prePerimeterResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, Unit unit, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, null, objectiveFunction, unit, numberOfLoggedElements);
    }

    private static void logMostLimitingElementsResults(OpenRaoLogger logger,
                                                       ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                       Unit unit,
                                                       int numberOfLoggedElements) {
        getMostLimitingElementsResults(objectiveFunctionResult, flowResult, states, objectiveFunction, unit, numberOfLoggedElements)
            .forEach(logger::info);
    }

    static List<String> getMostLimitingElementsResults(ObjectiveFunctionResult objectiveFunctionResult,
                                                       FlowResult flowResult,
                                                       Set<State> states,
                                                       ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                       Unit unit,
                                                       int numberOfLoggedElements) {
        List<String> summary = new ArrayList<>();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        List<FlowCnec> sortedCnecs = getMostLimitingElements(objectiveFunctionResult, states, numberOfLoggedElements);

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, unit) : flowResult.getMargin(cnec, unit);

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            TwoSides mostConstrainedSide = getMostConstrainedSide(cnec, flowResult, objectiveFunction, unit);
            String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format(" (PTDF %f)", flowResult.getPtdfZonalSum(cnec, mostConstrainedSide)) : "";
            summary.add(String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %s %s%s, element %s at state %s, CNEC ID = \"%s\"",
                i + 1,
                isRelativeMargin,
                roundValueBasedOnMargin(cnecMargin, cnecMargin, 2).doubleValue(),
                unit,
                ptdfIfRelative,
                cnecNetworkElementName,
                cnecStateId,
                cnec.getId()));
        }
        return summary;
    }

    private static TwoSides getMostConstrainedSide(FlowCnec cnec,
                                                   FlowResult flowResult,
                                                   ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                   Unit unit) {
        if (cnec.getMonitoredSides().size() == 1) {
            return cnec.getMonitoredSides().iterator().next();
        }
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();
        double marginLeft = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.ONE, unit) : flowResult.getMargin(cnec, TwoSides.ONE, unit);
        double marginRight = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.TWO, unit) : flowResult.getMargin(cnec, TwoSides.TWO, unit);
        return marginRight < marginLeft ? TwoSides.TWO : TwoSides.ONE;
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger,
                                                      Perimeter preventivePerimeter,
                                                      OptimizationResult basecaseOptimResult,
                                                      Set<ContingencyScenario> contingencyScenarios,
                                                      Map<State, OptimizationResult> contingencyOptimizationResults,
                                                      ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                      Unit unit,
                                                      int numberOfLoggedElements) {
        getMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunction, unit, numberOfLoggedElements)
            .forEach(logger::info);
    }

    public static List<String> getMostLimitingElementsResults(Perimeter preventivePerimeter,
                                                              OptimizationResult basecaseOptimResult,
                                                              Set<ContingencyScenario> contingencyScenarios,
                                                              Map<State, OptimizationResult> contingencyOptimizationResults,
                                                              ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                              Unit unit,
                                                              int numberOfLoggedElements) {
        List<String> summary = new ArrayList<>();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        Map<FlowCnec, Double> mostLimitingElementsAndMargins =
            getMostLimitingElementsAndMargins(basecaseOptimResult, preventivePerimeter.getAllStates(), unit, relativePositiveMargins, numberOfLoggedElements);

        contingencyScenarios.forEach(contingencyScenario -> {
            Optional<State> automatonState = contingencyScenario.getAutomatonState();
            automatonState.ifPresent(state -> mostLimitingElementsAndMargins.putAll(
                getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(state), Set.of(state), unit, relativePositiveMargins, numberOfLoggedElements)
            ));
            contingencyScenario.getCurativePerimeters()
                .forEach(
                    curativePerimeter -> mostLimitingElementsAndMargins.putAll(
                        getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(curativePerimeter.getRaOptimisationState()), Set.of(curativePerimeter.getRaOptimisationState()), unit, relativePositiveMargins, numberOfLoggedElements)
                    )
                );
        });

        List<FlowCnec> sortedCnecs = mostLimitingElementsAndMargins.keySet().stream()
            .sorted(Comparator.comparing(mostLimitingElementsAndMargins::get))
            .toList();
        sortedCnecs = sortedCnecs.subList(0, Math.min(sortedCnecs.size(), numberOfLoggedElements));

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = mostLimitingElementsAndMargins.get(cnec);

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            summary.add(String.format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %s %s, element %s at state %s, CNEC ID = \"%s\"",
                i + 1,
                isRelativeMargin,
                roundValueBasedOnMargin(cnecMargin, cnecMargin, 2).doubleValue(),
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
                .toList();
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

    public static void logFailedOptimizationSummary(OpenRaoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        logger.info("Scenario \"{}\": {}", scenarioName, raResult);
    }

    public static void logOptimizationSummary(OpenRaoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions, ObjectiveFunctionResult preOptimObjectiveFunctionResult, ObjectiveFunctionResult finalObjective) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        Map<String, Double> finalVirtualCostDetailed = getVirtualCostDetailed(finalObjective);
        String initialCostString;
        if (preOptimObjectiveFunctionResult == null) {
            initialCostString = "";
        } else {
            Map<String, Double> initialVirtualCostDetailed = getVirtualCostDetailed(preOptimObjectiveFunctionResult);
            double margin = -(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost());
            if (initialVirtualCostDetailed.isEmpty()) {
                initialCostString = String.format("initial cost = %s (functional: %s, virtual: %s), ", formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost(), margin), formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost(), margin), formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getVirtualCost(), margin));
            } else {
                initialCostString = String.format("initial cost = %s (functional: %s, virtual: %s %s), ", formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost(), margin), formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost(), margin), formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getVirtualCost(), margin), initialVirtualCostDetailed);
            }
        }
        logger.info("Scenario \"{}\": {}{}, cost after {} optimization = {} (functional: {}, virtual: {}{})", scenarioName, initialCostString, raResult, optimizedState.getInstant(),
            formatDoubleBasedOnMargin(finalObjective.getCost(), -finalObjective.getCost()), formatDoubleBasedOnMargin(finalObjective.getFunctionalCost(), -finalObjective.getCost()), formatDoubleBasedOnMargin(finalObjective.getVirtualCost(), -finalObjective.getCost()), finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed);
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
        return state.getContingency()
            .map(contingency -> contingency.getName().orElse(contingency.getId()))
            .orElse("preventive");
    }

    public static String formatDoubleBasedOnMargin(double value, double margin) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            // Double.toString, similarly to String formatting with Locale.English ensures doubles are written with "." rather than ","
            return Double.toString(roundValueBasedOnMargin(value, margin, 2).doubleValue());
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
