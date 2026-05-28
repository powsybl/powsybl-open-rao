/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeAdder;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.commons.RaoLogger;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.searchtree.algorithms.Leaf;
import com.powsybl.openrao.searchtreerao.searchtree.inputs.SearchTreeInput;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.searchtreerao.castor.algorithm.AutomatonSimulator.getRangeActionsAndTheirTapsAppliedOnState;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class OptimizationSummaryReports {
    private OptimizationSummaryReports() {
        // Utility class should not be instantiated
    }

    public static void reportOptimizationSummary(final ReportNode parentNode,
                                                 final State optimizedState,
                                                 final Set<NetworkAction> networkActions,
                                                 final Map<RangeAction<?>, Double> rangeActions,
                                                 final ObjectiveFunctionResult preOptimObjectiveFunctionResult,
                                                 final ObjectiveFunctionResult finalObjective) {
        final String scenarioName = ReportUtils.getScenarioName(optimizedState);
        final String raResult = ReportUtils.getRaResult(networkActions, rangeActions);
        final Map<String, Double> finalVirtualCostDetailed = RaoLogger.getVirtualCostDetailed(finalObjective);
        ReportNodeAdder reportNodeAdder = parentNode.newReportNode();

        if (preOptimObjectiveFunctionResult == null) {
            reportNodeAdder = reportNodeAdder.withMessageTemplate("openrao.searchtreerao.reportOptimizationSummaryWithoutInitialCost");
        } else {
            final Map<String, Double> initialVirtualCostDetailed = RaoLogger.getVirtualCostDetailed(preOptimObjectiveFunctionResult);
            final double margin = -(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost());
            reportNodeAdder = reportNodeAdder.withMessageTemplate("openrao.searchtreerao.reportOptimizationSummaryWithInitialCost")
                .withUntypedValue("initialCost", ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost(), margin))
                .withUntypedValue("initialFunctionalCost", ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost(), margin))
                .withUntypedValue("initialVirtualCost", ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getVirtualCost(), margin))
                .withUntypedValue("initialVirtualCostDetail", initialVirtualCostDetailed.isEmpty() ? "" : " " + initialVirtualCostDetailed);
        }

        reportNodeAdder
            .withUntypedValue("scenarioName", scenarioName)
            .withUntypedValue("raResult", raResult)
            .withUntypedValue("optimizedStateInstant", Objects.toString(optimizedState.getInstant()))
            .withUntypedValue("finalCost", ReportUtils.formatDoubleBasedOnMargin(finalObjective.getCost(), -finalObjective.getCost()))
            .withUntypedValue("finalFunctionalCost", ReportUtils.formatDoubleBasedOnMargin(finalObjective.getFunctionalCost(), -finalObjective.getCost()))
            .withUntypedValue("finalVirtualCost", ReportUtils.formatDoubleBasedOnMargin(finalObjective.getVirtualCost(), -finalObjective.getCost()))
            .withUntypedValue("finalVirtualCostDetail", finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed)
            .withSeverity(INFO_SEVERITY)
            .add();

        logOptimizationSummary(BUSINESS_LOGS, optimizedState, preOptimObjectiveFunctionResult, finalObjective, scenarioName, raResult);
    }

    public static void reportOptimizationSummary(final ReportNode parentNode,
                                                 final Leaf optimalLeaf,
                                                 final SearchTreeInput input,
                                                 final ObjectiveFunctionResult preOptimObjectiveFunctionResult) {
        final State state = input.getOptimizationPerimeter().getMainOptimizationState();
        final Map<RangeAction<?>, Double> rangeActionsAndTheirTapsAppliedOnState = getRangeActionsAndTheirTapsAppliedOnState(optimalLeaf, state);
        final Set<NetworkAction> networkActions = optimalLeaf.getActivatedNetworkActions();
        reportOptimizationSummary(parentNode,
            state,
            networkActions,
            rangeActionsAndTheirTapsAppliedOnState,
            preOptimObjectiveFunctionResult,
            optimalLeaf);
    }

    public static void logOptimizationSummary(final OpenRaoLogger logger,
                                              final State optimizedState,
                                              final ObjectiveFunctionResult preOptimObjectiveFunctionResult,
                                              final ObjectiveFunctionResult finalObjective,
                                              final String scenarioName,
                                              final String raResult) {
        final Map<String, Double> finalVirtualCostDetailed = RaoLogger.getVirtualCostDetailed(finalObjective);
        final String initialCostString;
        if (preOptimObjectiveFunctionResult == null) {
            initialCostString = "";
        } else {
            final Map<String, Double> initialVirtualCostDetailed = RaoLogger.getVirtualCostDetailed(preOptimObjectiveFunctionResult);
            final double margin = -(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost());
            if (initialVirtualCostDetailed.isEmpty()) {
                initialCostString = String.format("initial cost = %s (functional: %s, virtual: %s), ", ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost(), margin), ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost(), margin), ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getVirtualCost(), margin));
            } else {
                initialCostString = String.format("initial cost = %s (functional: %s, virtual: %s %s), ", ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost() + preOptimObjectiveFunctionResult.getVirtualCost(), margin), ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getFunctionalCost(), margin), ReportUtils.formatDoubleBasedOnMargin(preOptimObjectiveFunctionResult.getVirtualCost(), margin), initialVirtualCostDetailed);
            }
        }
        logger.info("Scenario \"{}\": {}{}, cost after {} optimization = {} (functional: {}, virtual: {}{})", scenarioName, initialCostString, raResult, optimizedState.getInstant(),
            ReportUtils.formatDoubleBasedOnMargin(finalObjective.getCost(), -finalObjective.getCost()), ReportUtils.formatDoubleBasedOnMargin(finalObjective.getFunctionalCost(), -finalObjective.getCost()), ReportUtils.formatDoubleBasedOnMargin(finalObjective.getVirtualCost(), -finalObjective.getCost()), finalVirtualCostDetailed.isEmpty() ? "" : " " + finalVirtualCostDetailed);
    }
}
