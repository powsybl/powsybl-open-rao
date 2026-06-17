/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.reports;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.searchtreerao.commons.objectivefunction.ObjectiveFunction;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import static com.powsybl.commons.report.TypedValue.ERROR_SEVERITY;
import static com.powsybl.commons.report.TypedValue.INFO_SEVERITY;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_LOGS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class FastRaoReports {
    private FastRaoReports() {
        // Utility class should not be instantiated
    }

    public static void reportFastRaoInitialSensitivityAnalysisResults(final ReportNode parentNode,
                                                                      final ObjectiveFunction objectiveFunction,
                                                                      final RemedialActionActivationResult remedialActionActivationResult,
                                                                      final PrePerimeterResult sensitivityAnalysisResult,
                                                                      final RaoParameters raoParameters,
                                                                      final int numberOfLoggedLimitingElements) {
        final String messageTemplate = "openrao.searchtreerao.reportFastRaoInitialSensitivityAnalysisResults";
        final String prefix = "[FAST RAO] Initial sensitivity analysis: ";
        CommonReports.reportSensitivityAnalysisResults(parentNode, messageTemplate, prefix, objectiveFunction, remedialActionActivationResult, sensitivityAnalysisResult, raoParameters, numberOfLoggedLimitingElements);
    }

    public static void reportFastRaoIterationIntermediateResult(final ReportNode parentNode,
                                                                final Integer iterationCounter,
                                                                final PrePerimeterResult sensitivityAnalysisResult,
                                                                final RaoParameters parameters,
                                                                final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportFastRaoIterationIntermediateResult";
        final String prefix = String.format("[FAST RAO] Iteration %d: sensitivity analysis: ", iterationCounter);
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportFastRaoFinalResult(final ReportNode parentNode,
                                                final PrePerimeterResult sensitivityAnalysisResult,
                                                final RaoParameters parameters,
                                                final int numberLoggedElementsDuringRao) {
        final String messageTemplate = "openrao.searchtreerao.reportFastRaoFinalResult";
        final String prefix = "[FAST RAO] Final Result: ";
        CommonReports.reportObjectiveFunctionResult(parentNode, messageTemplate, prefix, sensitivityAnalysisResult, sensitivityAnalysisResult, sensitivityAnalysisResult, parameters, numberLoggedElementsDuringRao);
    }

    public static void reportMissingFastRaoParametersExtension(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportMissingFastRaoParametersExtension")
            .withSeverity(WARN_SEVERITY)
            .add();

        BUSINESS_WARNS.warn("Parameters are missing FastRaoParameters extension. Default FastRaoParameters will be used");
    }

    public static void reportFastRaoDoesNotSupportOptimizationOnOneGivenStateOnly(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFastRaoDoesNotSupportOptimizationOnOneGivenStateOnly")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Fast Rao does not support optimization on one given state only");
    }

    public static void reportFastRaoDoesNotSupportMultiCurativeOptimization(final ReportNode parentNode) {
        parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFastRaoDoesNotSupportMultiCurativeOptimization")
            .withSeverity(ERROR_SEVERITY)
            .add();

        BUSINESS_LOGS.error("Fast Rao does not support multi-curative optimization");
    }

    public static ReportNode reportFastRaoIteration(final ReportNode parentNode, final int iterationCounter) {
        return parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFastRaoIteration")
            .withUntypedValue("iterationCounter", iterationCounter)
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportFastRaoIterationRunFilteredRao(final ReportNode parentNode,
                                                                  final int iterationCounter,
                                                                  final int nbCnecsToKeep,
                                                                  final int nbCnecsInCrac) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFastRaoIterationRunFilteredRao")
            .withUntypedValue("nbCnecsToKeep", nbCnecsToKeep)
            .withUntypedValue("nbCnecsInCrac", nbCnecsInCrac)
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run filtered RAO with {}/{} cnecs [start]", iterationCounter, nbCnecsToKeep, nbCnecsInCrac);

        return addedNode;
    }

    public static void reportFastRaoIterationRunFilteredRaoEnd(final int iterationCounter) {
        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run filtered RAO [end]", iterationCounter);
    }

    public static ReportNode reportFastRaoIterationRunFullSensitivityAnalysis(final ReportNode parentNode, final int iterationCounter) {
        final ReportNode addedNode = parentNode.newReportNode()
            .withMessageTemplate("openrao.searchtreerao.reportFastRaoIterationRunFullSensitivityAnalysis")
            .withSeverity(INFO_SEVERITY)
            .add();

        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run full sensitivity analysis [start]", iterationCounter);

        return addedNode;
    }

    public static void reportFastRaoIterationRunFullSensitivityAnalysisEnd(final int iterationCounter) {
        BUSINESS_LOGS.info("[FAST RAO] Iteration {}: Run full sensitivity analysis [end]", iterationCounter);
    }
}
