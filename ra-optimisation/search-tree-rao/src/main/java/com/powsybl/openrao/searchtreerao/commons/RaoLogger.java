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
import com.powsybl.openrao.data.cracapi.cnec.Side;
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
import com.powsybl.openrao.searchtreerao.result.impl.MultiStateRemedialActionResultImpl;
import com.powsybl.openrao.searchtreerao.result.impl.PerimeterResultWithCnecs;
import com.powsybl.openrao.searchtreerao.result.impl.SearchTreeResult;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                                                     PerimeterResultWithCnecs sensitivityAnalysisResult,
                                                     RaoParameters raoParameters,
                                                     int numberOfLoggedLimitingElements) {

        if (!BUSINESS_LOGS.isInfoEnabled()) {
            return;
        }

        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(sensitivityAnalysisResult,
            sensitivityAnalysisResult);

        BUSINESS_LOGS.info(prefix + "cost = {} (functional: {}, virtual: {})",
            formatDouble(prePerimeterObjectiveFunctionResult.getCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getFunctionalCost()),
            formatDouble(prePerimeterObjectiveFunctionResult.getVirtualCost()));

        RaoLogger.logMostLimitingElementsResults(BUSINESS_LOGS,
            sensitivityAnalysisResult,
            raoParameters.getObjectiveFunctionParameters().getType(),
            numberOfLoggedLimitingElements);
    }

    public static void logRangeActions(OpenRaoLogger logger,
                                       SearchTreeResult searchTreeResult,
                                       OptimizationPerimeter optimizationContext,
                                       String prefix) {

        boolean globalPstOptimization = optimizationContext instanceof GlobalOptimizationPerimeter;

        MultiStateRemedialActionResultImpl allStatesRangeActionResult = searchTreeResult.getAllStatesRemedialActionResult();

        List<String> rangeActionSetpoints = optimizationContext.getRangeActionOptimizationStates().stream().flatMap(state -> {
            RangeActionResult stateRangeActionResult = allStatesRangeActionResult.getRangeActionResultOnState(state);
            return stateRangeActionResult.getActivatedRangeActions().stream().map(rangeAction -> {
                double rangeActionValue = rangeAction instanceof PstRangeAction pstRangeAction ?
                    stateRangeActionResult.getOptimizedTap(pstRangeAction) :
                    stateRangeActionResult.getOptimizedSetpoint(rangeAction);
                return globalPstOptimization ? format("%s@%s: %.0f", rangeAction.getName(), state.getId(), rangeActionValue) :
                    format("%s: %.0f", rangeAction.getName(), rangeActionValue);
            });
        }).toList();

        boolean isRangeActionSetPointEmpty = rangeActionSetpoints.isEmpty();
        if (isRangeActionSetPointEmpty) {
            logger.info("{}No range actions activated", prefix == null ? "" : prefix);
        } else {
            logger.info("{}range action(s): {}", prefix == null ? "" : prefix, String.join(", ", rangeActionSetpoints));
        }
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, OptimizationResult optimizationResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, optimizationResult, optimizationResult, null, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, PerimeterResultWithCnecs prePerimeterResult, Set<State> states, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, states, objectiveFunction, numberOfLoggedElements);
    }

    public static void logMostLimitingElementsResults(OpenRaoLogger logger, PerimeterResultWithCnecs prePerimeterResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements) {
        logMostLimitingElementsResults(logger, prePerimeterResult, prePerimeterResult, null, objectiveFunction, numberOfLoggedElements);
    }

    private static void logMostLimitingElementsResults(OpenRaoLogger logger,
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
            summary.add(format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s%s, element %s at state %s, CNEC ID = \"%s\"",
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

    public static void logMostLimitingElementsResults(OpenRaoLogger logger,
                                                      Perimeter preventivePerimeter,
                                                      OptimizationResult basecaseOptimResult,
                                                      Set<ContingencyScenario> contingencyScenarios,
                                                      Map<State, PerimeterResultWithCnecs> contingencyOptimizationResults,
                                                      ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                      int numberOfLoggedElements) {
        getMostLimitingElementsResults(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunction, numberOfLoggedElements)
            .forEach(logger::info);
    }

    public static List<String> getMostLimitingElementsResults(Perimeter preventivePerimeter,
                                                              OptimizationResult basecaseOptimResult,
                                                              Set<ContingencyScenario> contingencyScenarios,
                                                              Map<State, PerimeterResultWithCnecs> contingencyOptimizationResults,
                                                              ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                              int numberOfLoggedElements) {
        List<String> summary = new ArrayList<>();
        Unit unit = objectiveFunction.getUnit();
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
            summary.add(format(Locale.ENGLISH, "Limiting element #%02d:%s margin = %.2f %s, element %s at state %s, CNEC ID = \"%s\"",
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

    public static void logFailedOptimizationSummary(OpenRaoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, Double> rangeActions) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        logger.info("Scenario \"{}\": {}", scenarioName, raResult);
    }

    public static void logOptimizationSummary(OpenRaoLogger logger, State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, Double> rangeActions, ObjectiveFunctionResult preOptimObjectiveFunctionResult, ObjectiveFunctionResult finalObjective) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        Map<String, Double> finalVirtualCostDetailed = getVirtualCostDetailed(finalObjective);
        String initialCostString;
        if (preOptimObjectiveFunctionResult == null) {
            initialCostString = "";
        } else {
            Map<String, Double> initialVirtualCostDetailed = getVirtualCostDetailed(preOptimObjectiveFunctionResult);
            if (initialVirtualCostDetailed.isEmpty()) {
                initialCostString = format("initial cost = %s (functional: %s, virtual: %s), ", formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost()), formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost()), formatDouble(preOptimObjectiveFunctionResult.getVirtualCost()));
            } else {
                initialCostString = format("initial cost = %s (functional: %s, virtual: %s %s), ", formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost()), formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost()), formatDouble(preOptimObjectiveFunctionResult.getVirtualCost()), initialVirtualCostDetailed);
            }
        }
        logger.info("Scenario \"{}\": {}{}, cost after {} optimization = {} (functional: {}, virtual: {}{})", scenarioName, initialCostString, raResult, optimizedState.getInstant(),
            formatDouble(finalObjective.getCost()), formatDouble(finalObjective.getFunctionalCost()), formatDouble(finalObjective.getVirtualCost()), finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed);
    }

    public static String getRaResult(Set<NetworkAction> networkActions, Map<RangeAction<?>, Double> rangeActions) {
        long activatedNetworkActions = networkActions.size();
        long activatedRangeActions = rangeActions.size();
        String networkActionsNames = StringUtils.join(networkActions.stream().map(Identifiable::getName).collect(Collectors.toSet()), ", ");

        Set<String> rangeActionsSet = new HashSet<>();
        rangeActions.forEach((key, value) -> rangeActionsSet.add(format("%s: %.0f", key.getName(), value)));
        String rangeActionsNames = StringUtils.join(rangeActionsSet, ", ");

        if (activatedNetworkActions + activatedRangeActions == 0) {
            return "no remedial actions activated";
        } else if (activatedNetworkActions > 0 && activatedRangeActions == 0) {
            return format("%s network action(s) activated : %s", activatedNetworkActions, networkActionsNames);
        } else if (activatedRangeActions > 0 && activatedNetworkActions == 0) {
            return format("%s range action(s) activated : %s", activatedRangeActions, rangeActionsNames);
        } else {
            return format("%s network action(s) and %s range action(s) activated : %s and %s",
                activatedNetworkActions, activatedRangeActions, networkActionsNames, rangeActionsNames);
        }
    }

    public static String getScenarioName(State state) {
        return state.getContingency()
            .map(contingency -> contingency.getName().orElse(contingency.getId()))
            .orElse("preventive");
    }

    public static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return format(Locale.ENGLISH, "%.2f", value);
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
