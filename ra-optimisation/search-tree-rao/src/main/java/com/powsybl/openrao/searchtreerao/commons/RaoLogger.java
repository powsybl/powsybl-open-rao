/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.openrao.commons.Unit;
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
                                                     RangeActionActivationResult rangeActionActivationResult,
                                                     PrePerimeterResult sensitivityAnalysisResult,
                                                     RaoParameters raoParameters,
                                                     int numberOfLoggedLimitingElements,
                                                     ReportNode reportNode) {

        if (!BUSINESS_LOGS.isInfoEnabled()) { // TODO: should we remove this ?
            return;
        }

        ObjectiveFunctionResult prePerimeterObjectiveFunctionResult = objectiveFunction.evaluate(sensitivityAnalysisResult, rangeActionActivationResult,
            sensitivityAnalysisResult, sensitivityAnalysisResult.getSensitivityStatus(), reportNode);
        ReportNode reportSensitivityAnalysisResults = RaoCommonsReports.reportSensitivityAnalysisResults(reportNode,
            prefix,
            prePerimeterObjectiveFunctionResult.getCost(),
            prePerimeterObjectiveFunctionResult.getFunctionalCost(),
            prePerimeterObjectiveFunctionResult.getVirtualCost());

        RaoLogger.logMostLimitingElementsResults(
                sensitivityAnalysisResult,
            raoParameters.getObjectiveFunctionParameters().getType(),
            numberOfLoggedLimitingElements,
            reportSensitivityAnalysisResults, TypedValue.INFO_SEVERITY);
    }

    public static ReportNode logRangeActions(Leaf leaf,
                                             OptimizationPerimeter optimizationContext,
                                             String prefix,
                                             ReportNode reportNode) {

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
            return RaoCommonsReports.reportSearchTreeOneLeafNoRangeActionActivated(reportNode, prefix);
        } else {
            return RaoCommonsReports.reportSearchTreeOneLeafRangeActionActivated(reportNode, prefix, rangeActionSetpoints.stream().sorted().collect(Collectors.joining(", ")));
        }
    }

    public static void logMostLimitingElementsResults(OptimizationResult optimizationResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements, ReportNode reportNode, TypedValue severity) {
        logMostLimitingElementsResults(optimizationResult, optimizationResult, null, objectiveFunction, numberOfLoggedElements, reportNode, severity);
    }

    public static void logMostLimitingElementsResults(PrePerimeterResult prePerimeterResult, Set<State> states, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements, ReportNode reportNode, TypedValue severity) {
        logMostLimitingElementsResults(prePerimeterResult, prePerimeterResult, states, objectiveFunction, numberOfLoggedElements, reportNode, severity);
    }

    public static void logMostLimitingElementsResults(PrePerimeterResult prePerimeterResult, ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction, int numberOfLoggedElements, ReportNode reportNode, TypedValue severity) {
        logMostLimitingElementsResults(prePerimeterResult, prePerimeterResult, null, objectiveFunction, numberOfLoggedElements, reportNode, severity);
    }

    static void logMostLimitingElementsResults(ObjectiveFunctionResult objectiveFunctionResult,
                                               FlowResult flowResult,
                                               Set<State> states,
                                               ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                               int numberOfLoggedElements,
                                               ReportNode reportNode,
                                               TypedValue reportSeverity) {
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        List<FlowCnec> sortedCnecs = getMostLimitingElements(objectiveFunctionResult, states, numberOfLoggedElements).stream()
            .sorted((cnec1, cnec2) -> compareTwoFlowCnecs(flowResult, cnec1, cnec2, relativePositiveMargins, unit))
            .toList();

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = getCnecMargin(flowResult, cnec, relativePositiveMargins, unit);

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            TwoSides mostConstrainedSide = getMostConstrainedSide(cnec, flowResult, objectiveFunction);
            String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format(" (PTDF %f)", flowResult.getPtdfZonalSum(cnec, mostConstrainedSide)) : "";
            RaoCommonsReports.reportMostLimitingElement(reportNode, reportSeverity, i + 1, isRelativeMargin, cnecMargin, unit, ptdfIfRelative, cnecNetworkElementName, cnecStateId, cnec.getId());
        }
    }

    private static int compareTwoFlowCnecs(FlowResult flowResult, FlowCnec cnec1, FlowCnec cnec2, boolean relativePositiveMargins, Unit unit) {
        double cnecMargin1 = getCnecMargin(flowResult, cnec1, relativePositiveMargins, unit);
        double cnecMargin2 = getCnecMargin(flowResult, cnec2, relativePositiveMargins, unit);
        return compareCnecMarginsAndIds(cnec1, cnec2, cnecMargin1, cnecMargin2);
    }

    private static double getCnecMargin(FlowResult flowResult, FlowCnec cnec1, boolean relativePositiveMargins, Unit unit) {
        return relativePositiveMargins ? flowResult.getRelativeMargin(cnec1, unit) : flowResult.getMargin(cnec1, unit);
    }

    private static int compareCnecMarginsAndIds(FlowCnec cnec1, FlowCnec cnec2, Double cnecMargin1, Double cnecMargin2) {
        if (Objects.equals(cnecMargin1, cnecMargin2)) {
            return cnec1.getId().compareTo(cnec2.getId());
        }
        return Double.compare(cnecMargin1, cnecMargin2);
    }

    private static TwoSides getMostConstrainedSide(FlowCnec cnec,
                                                   FlowResult flowResult,
                                                   ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
        if (cnec.getMonitoredSides().size() == 1) {
            return cnec.getMonitoredSides().iterator().next();
        }
        Unit unit = objectiveFunction.getUnit();
        boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();
        double marginLeft = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.ONE, unit) : flowResult.getMargin(cnec, TwoSides.ONE, unit);
        double marginRight = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.TWO, unit) : flowResult.getMargin(cnec, TwoSides.TWO, unit);
        return marginRight < marginLeft ? TwoSides.TWO : TwoSides.ONE;
    }

    public static void logMostLimitingElementsResults(Perimeter preventivePerimeter,
                                                              OptimizationResult basecaseOptimResult,
                                                              Set<ContingencyScenario> contingencyScenarios,
                                                              Map<State, OptimizationResult> contingencyOptimizationResults,
                                                              ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                              int numberOfLoggedElements,
                                                              ReportNode reportNode) {
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
            .sorted((cnec1, cnec2) -> compareCnecMarginsAndIds(cnec1, cnec2, mostLimitingElementsAndMargins.get(cnec1), mostLimitingElementsAndMargins.get(cnec2)))
            .toList();
        sortedCnecs = sortedCnecs.subList(0, Math.min(sortedCnecs.size(), numberOfLoggedElements));

        for (int i = 0; i < sortedCnecs.size(); i++) {
            FlowCnec cnec = sortedCnecs.get(i);
            String cnecNetworkElementName = cnec.getNetworkElement().getName();
            String cnecStateId = cnec.getState().getId();
            double cnecMargin = mostLimitingElementsAndMargins.get(cnec);

            String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            RaoCommonsReports.reportMostLimitingElement(reportNode, TypedValue.INFO_SEVERITY, i + 1, isRelativeMargin, cnecMargin, unit, "", cnecNetworkElementName, cnecStateId, cnec.getId());
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
            double cnecMargin = getCnecMargin(optimizationResult, cnec, relativePositiveMargins, unit);
            mostLimitingElementsAndMargins.put(cnec, cnecMargin);
        });
        return mostLimitingElementsAndMargins;
    }

    public static void logFailedOptimizationSummary(State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, java.lang.Double> rangeActions, ReportNode reportNode) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        RaoCommonsReports.reportFailedOptimizationSummary(reportNode, scenarioName, raResult);
    }

    public static void logOptimizationSummary(State optimizedState, Set<NetworkAction> networkActions, Map<RangeAction<?>, Double> rangeActions, ObjectiveFunctionResult preOptimObjectiveFunctionResult, ObjectiveFunctionResult finalObjective, ReportNode reportNode) {
        String scenarioName = getScenarioName(optimizedState);
        String raResult = getRaResult(networkActions, rangeActions);
        Map<String, Double> finalVirtualCostDetailed = getVirtualCostDetailed(finalObjective);
        String finalVirtualCostDetailedString = finalVirtualCostDetailed.isEmpty() ? "null" : finalVirtualCostDetailed.toString();

        Map<String, Double> initialVirtualCostDetailed;
        String preOptimObjectiveFunctionResultCost;
        String preOptimObjectiveFunctionResultFunctionalCost;
        String preOptimObjectiveFunctionResultVirtualCost;
        if (Objects.isNull(preOptimObjectiveFunctionResult)) {
            initialVirtualCostDetailed = Collections.emptyMap();
            preOptimObjectiveFunctionResultCost = "null";
            preOptimObjectiveFunctionResultFunctionalCost = "null";
            preOptimObjectiveFunctionResultVirtualCost = "null";
        } else {
            initialVirtualCostDetailed = getVirtualCostDetailed(preOptimObjectiveFunctionResult);
            preOptimObjectiveFunctionResultCost = formatDouble(preOptimObjectiveFunctionResult.getCost());
            preOptimObjectiveFunctionResultFunctionalCost = formatDouble(preOptimObjectiveFunctionResult.getFunctionalCost());
            preOptimObjectiveFunctionResultVirtualCost = formatDouble(preOptimObjectiveFunctionResult.getVirtualCost());
        }
        String initialVirtualCostDetailedString = initialVirtualCostDetailed.isEmpty() ? "null" : initialVirtualCostDetailed.toString();
        RaoCommonsReports.reportOptimizationSummaryOnScenario(reportNode, scenarioName, preOptimObjectiveFunctionResultCost, preOptimObjectiveFunctionResultFunctionalCost, preOptimObjectiveFunctionResultVirtualCost, initialVirtualCostDetailedString, raResult, optimizedState.getInstant().toString(), finalObjective.getCost(), finalObjective.getFunctionalCost(), finalObjective.getVirtualCost(), finalVirtualCostDetailedString);
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
