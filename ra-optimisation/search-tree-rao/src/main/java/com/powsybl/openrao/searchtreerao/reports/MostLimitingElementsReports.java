/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.logs.OpenRaoLogger;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.searchtreerao.castor.algorithm.ContingencyScenario;
import com.powsybl.openrao.searchtreerao.castor.algorithm.Perimeter;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.ObjectiveFunctionResult;
import com.powsybl.openrao.searchtreerao.result.api.OptimizationResult;
import com.powsybl.openrao.searchtreerao.result.impl.PostPerimeterResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.TRACE_SEVERITY;
import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;
import static java.lang.String.format;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class MostLimitingElementsReports {
    private MostLimitingElementsReports() {
        // Utility class should not be instantiated
    }

    public static void reportTechnicalMostLimitingElements(final ReportNode parentNode,
                                                           final ObjectiveFunctionResult objectiveFunctionResult,
                                                           final FlowResult flowResult,
                                                           final Set<State> automatonStates,
                                                           final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                           final Unit objectiveFunctionUnit,
                                                           final int numberLoggedElementsDuringRao) {
        reportMostLimitingElementsResults(parentNode, TRACE_SEVERITY, TECHNICAL_LOGS, objectiveFunctionResult, flowResult, automatonStates, objectiveFunctionType, objectiveFunctionUnit, numberLoggedElementsDuringRao);
    }

    public static void reportTechnicalMostLimitingElements(final ReportNode parentNode,
                                                           final ObjectiveFunctionResult objectiveFunctionResult,
                                                           final FlowResult flowResult,
                                                           final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                           final Unit objectiveFunctionUnit,
                                                           final int numberLoggedElementsDuringRao) {
        reportMostLimitingElementsResults(parentNode, TRACE_SEVERITY, TECHNICAL_LOGS, objectiveFunctionResult, flowResult, null, objectiveFunctionType, objectiveFunctionUnit, numberLoggedElementsDuringRao);
    }

    public static void reportBusinessMostLimitingElements(final ReportNode parentNode,
                                                          final ObjectiveFunctionResult objectiveFunctionResult,
                                                          final FlowResult flowResult,
                                                          final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                          final Unit objectiveFunctionUnit,
                                                          final int numberLoggedElementsDuringRao) {
        reportMostLimitingElementsResults(parentNode, INFO_SEVERITY, BUSINESS_LOGS, objectiveFunctionResult, flowResult, null, objectiveFunctionType, objectiveFunctionUnit, numberLoggedElementsDuringRao);
    }

    public static void reportBusinessMostLimitingElements(final ReportNode parentNode,
                                                          final Perimeter preventivePerimeter,
                                                          final OptimizationResult basecaseOptimResult,
                                                          final Set<ContingencyScenario> contingencyScenarios,
                                                          final Map<State, PostPerimeterResult> contingencyOptimizationResults,
                                                          final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                          final Unit unit,
                                                          final int numberOfLoggedElements) {
        reportMostLimitingElementsResults(parentNode, INFO_SEVERITY, BUSINESS_LOGS, preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunctionType, unit, numberOfLoggedElements);
    }

    private static void reportMostLimitingElementsResults(final ReportNode parentNode,
                                                          final TypedValue reportSeverity,
                                                          final OpenRaoLogger logger,
                                                          final ObjectiveFunctionResult objectiveFunctionResult,
                                                          final FlowResult flowResult,
                                                          final Set<State> automatonStates,
                                                          final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                          final Unit objectiveFunctionUnit,
                                                          final int numberLoggedElementsDuringRao) {
        final List<MostLimitingElementRecord> mostLimitingElementRecords = getMostLimitingElementRecords(objectiveFunctionResult, flowResult, automatonStates, objectiveFunctionType, objectiveFunctionUnit, numberLoggedElementsDuringRao);
        if (!mostLimitingElementRecords.isEmpty()) {
            final ReportNode addedNode = parentNode.newReportNode()
                .withMessageTemplate("openrao.searchtreerao.reportMostLimitingElements")
                .withSeverity(reportSeverity)
                .add();
            logger.info("Most limiting elements:");

            mostLimitingElementRecords
                .forEach(element -> reportMostLimitingElementResult(addedNode, reportSeverity, logger, element));
        }
    }

    private static void reportMostLimitingElementsResults(final ReportNode parentNode,
                                                          final TypedValue reportSeverity,
                                                          final OpenRaoLogger logger,
                                                          final Perimeter preventivePerimeter,
                                                          final OptimizationResult basecaseOptimResult,
                                                          final Set<ContingencyScenario> contingencyScenarios,
                                                          final Map<State, PostPerimeterResult> contingencyOptimizationResults,
                                                          final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunctionType,
                                                          final Unit unit,
                                                          final int numberOfLoggedElements) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMostLimitingElements")
            .withSeverity(reportSeverity)
            .add();
        logger.info("Most limiting elements:");

        getMostLimitingElementRecords(preventivePerimeter, basecaseOptimResult, contingencyScenarios, contingencyOptimizationResults, objectiveFunctionType, unit, numberOfLoggedElements)
            .forEach(element -> reportMostLimitingElementResult(addedNode, reportSeverity, logger, element));
    }

    private static void reportMostLimitingElementResult(final ReportNode parentNode,
                                                        final TypedValue reportSeverity,
                                                        final OpenRaoLogger logger,
                                                        final MostLimitingElementRecord element) {
        final String limitingElementIndex = String.format(Locale.ENGLISH, "%02d", element.limitingElementIndex());
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportInitialSituationMostLimitingElements")
            .withUntypedValue("limitingElementIndex", limitingElementIndex)
            .withUntypedValue("isRelativeMargin", element.isRelativeMargin())
            .withUntypedValue("roundedCnecMargin", element.roundedCnecMargin())
            .withUntypedValue("unit", Objects.toString(element.unit()))
            .withUntypedValue("ptdfIfRelative", element.ptdfIfRelative())
            .withUntypedValue("cnecNetworkElementName", element.cnecNetworkElementName())
            .withUntypedValue("cnecStateId", element.cnecStateId())
            .withUntypedValue("cnecId", element.cnecId())
            .withSeverity(reportSeverity)
            .add();

        logger.info("Limiting element #{}:{} margin = {} {}{}, element {} at state {}, CNEC ID = \"{}\"",
            limitingElementIndex,
            element.isRelativeMargin(),
            element.roundedCnecMargin(),
            element.unit(),
            element.ptdfIfRelative(),
            element.cnecNetworkElementName(),
            element.cnecStateId(),
            element.cnecId());
    }

    static List<MostLimitingElementRecord> getMostLimitingElementRecords(final ObjectiveFunctionResult objectiveFunctionResult,
                                                                         final FlowResult flowResult,
                                                                         final Set<State> states,
                                                                         final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                                         final Unit unit,
                                                                         final int numberOfLoggedElements) {
        final List<MostLimitingElementRecord> mostLimitingElements = new ArrayList<>();
        final boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        final List<FlowCnec> sortedCnecs = ReportUtils.getMostLimitingElements(objectiveFunctionResult, states, numberOfLoggedElements);

        for (int i = 0; i < sortedCnecs.size(); i++) {
            final FlowCnec cnec = sortedCnecs.get(i);
            final double cnecMargin = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, unit) : flowResult.getMargin(cnec, unit);

            final int limitingElementIndex = i + 1;
            final String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            final double roundedCnecMargin = roundValueBasedOnMargin(cnecMargin, cnecMargin, 2).doubleValue();
            final TwoSides mostConstrainedSide = getMostConstrainedSide(cnec, flowResult, objectiveFunction, unit);
            final String ptdfIfRelative = (relativePositiveMargins && cnecMargin > 0) ? format(" (PTDF %f)", flowResult.getPtdfZonalSum(cnec, mostConstrainedSide)) : "";
            final String cnecNetworkElementName = cnec.getNetworkElement().getName();
            final String cnecStateId = cnec.getState().getId();
            final String cnecId = cnec.getId();

            mostLimitingElements.add(new MostLimitingElementRecord(
                limitingElementIndex,
                isRelativeMargin,
                roundedCnecMargin,
                unit,
                ptdfIfRelative,
                cnecNetworkElementName,
                cnecStateId,
                cnecId));
        }
        return mostLimitingElements;
    }

    public static List<MostLimitingElementRecord> getMostLimitingElementRecords(final Perimeter preventivePerimeter,
                                                                                final OptimizationResult basecaseOptimResult,
                                                                                final Set<ContingencyScenario> contingencyScenarios,
                                                                                final Map<State, PostPerimeterResult> contingencyOptimizationResults,
                                                                                final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                                                final Unit unit,
                                                                                final int numberOfLoggedElements) {
        final List<MostLimitingElementRecord> mostLimitingElements = new ArrayList<>();
        final boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();

        final Map<FlowCnec, Double> mostLimitingElementsAndMargins =
            ReportUtils.getMostLimitingElementsAndMargins(basecaseOptimResult, preventivePerimeter.getAllStates(), unit, relativePositiveMargins, numberOfLoggedElements);

        contingencyScenarios.forEach(contingencyScenario -> {
            final Optional<State> automatonState = contingencyScenario.getAutomatonState();
            automatonState.ifPresent(state -> mostLimitingElementsAndMargins.putAll(
                ReportUtils.getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(state).optimizationResult(), Set.of(state), unit, relativePositiveMargins, numberOfLoggedElements)
            ));
            contingencyScenario.getCurativePerimeters()
                .forEach(
                    curativePerimeter -> mostLimitingElementsAndMargins.putAll(
                        ReportUtils.getMostLimitingElementsAndMargins(contingencyOptimizationResults.get(curativePerimeter.getRaOptimisationState()).optimizationResult(), Set.of(curativePerimeter.getRaOptimisationState()), unit, relativePositiveMargins, numberOfLoggedElements)
                    )
                );
        });

        List<FlowCnec> sortedCnecs = mostLimitingElementsAndMargins.keySet().stream()
            .sorted(Comparator.comparing(mostLimitingElementsAndMargins::get))
            .toList();
        sortedCnecs = sortedCnecs.subList(0, Math.min(sortedCnecs.size(), numberOfLoggedElements));

        for (int i = 0; i < sortedCnecs.size(); i++) {
            final FlowCnec cnec = sortedCnecs.get(i);
            final double cnecMargin = mostLimitingElementsAndMargins.get(cnec);

            final int limitingElementIndex = i + 1;
            final String isRelativeMargin = (relativePositiveMargins && cnecMargin > 0) ? " relative" : "";
            final double roundedCnecMargin = roundValueBasedOnMargin(cnecMargin, cnecMargin, 2).doubleValue();
            final String cnecNetworkElementName = cnec.getNetworkElement().getName();
            final String cnecStateId = cnec.getState().getId();
            final String cnecId = cnec.getId();

            mostLimitingElements.add(new MostLimitingElementRecord(limitingElementIndex, isRelativeMargin, roundedCnecMargin, unit, "", cnecNetworkElementName, cnecStateId, cnecId));
        }
        return mostLimitingElements;
    }

    private static TwoSides getMostConstrainedSide(final FlowCnec cnec,
                                                   final FlowResult flowResult,
                                                   final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                                   final Unit unit) {
        if (cnec.getMonitoredSides().size() == 1) {
            return cnec.getMonitoredSides().iterator().next();
        }
        final boolean relativePositiveMargins = objectiveFunction.relativePositiveMargins();
        final double marginLeft = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.ONE, unit) : flowResult.getMargin(cnec, TwoSides.ONE, unit);
        final double marginRight = relativePositiveMargins ? flowResult.getRelativeMargin(cnec, TwoSides.TWO, unit) : flowResult.getMargin(cnec, TwoSides.TWO, unit);
        return marginRight < marginLeft ? TwoSides.TWO : TwoSides.ONE;
    }

    public record MostLimitingElementRecord(int limitingElementIndex,
                                            String isRelativeMargin,
                                            double roundedCnecMargin,
                                            Unit unit,
                                            String ptdfIfRelative,
                                            String cnecNetworkElementName,
                                            String cnecStateId,
                                            String cnecId) {
    }
}
