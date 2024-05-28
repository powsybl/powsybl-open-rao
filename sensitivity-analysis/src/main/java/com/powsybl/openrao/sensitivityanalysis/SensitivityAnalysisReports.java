package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

public final class SensitivityAnalysisReports {
    private SensitivityAnalysisReports() {
        // Utility class
    }

    public static ReportNode reportNewSystematicSensitivityInterface(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewSystematicSensitivityInterface", "New systematic sensitivity interface")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityAnalysisFailed", "Sensitivity analysis failed.")
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Sensitivity analysis failed.");
        return addedNode;

    }

    public static ReportNode reportNewSensitivityComputer(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewSensitivityComputer", "New sensitivity computer")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportSensitivityAnalysisFailedNoOutputDataAvailable(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityAnalysisFailedNoOutputDataAvailable", "Sensitivity analysis failed: no output data available.")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.error("Sensitivity analysis failed: no output data available.");
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisWithAppliedRAStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisWithAppliedRAStart", "Systematic sensitivity analysis with applied RA [start]")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [start]");
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisForStatesWithoutRA(ReportNode reportNode, int sizeWithRa, int sizeWithoutRa) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisForStatesWithoutRA", "... (1/${sizeWithRa}) ${sizeWithoutRa} state(s) without RA")
            .withUntypedValue("sizeWithRa", sizeWithRa)
            .withUntypedValue("sizeWithoutRa", sizeWithoutRa)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("... (1/{}) {} state(s) without RA", sizeWithRa, sizeWithoutRa);
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisForStatesWithRA(ReportNode reportNode, int counterForLogs, int sizeWithRa, String stateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisForStatesWithRA", "... (${counterForLogs}/${sizeWithRa}) state with RA ${stateId}")
            .withUntypedValue("counterForLogs", counterForLogs)
            .withUntypedValue("sizeWithRa", sizeWithRa)
            .withUntypedValue("stateId", stateId)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("... ({}/{}) state with RA {}", counterForLogs, sizeWithRa, stateId);
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisWithAppliedRAEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisWithAppliedRAEnd", "Systematic sensitivity analysis with applied RA [end]]")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis with applied RA [end]");
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisStart(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisStart", "Systematic sensitivity analysis [start]")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [start]");
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisFailed(ReportNode reportNode, String message) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisFailed", "Systematic sensitivity analysis failed: ${message}")
            .withUntypedValue("message", message)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.error("Systematic sensitivity analysis failed: {}", message);
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityAnalysisEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityAnalysisEnd", "Systematic sensitivity analysis [end]")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Systematic sensitivity analysis [end]");
        return addedNode;
    }

    public static ReportNode reportSensitivityProviderUnhandledUnit(ReportNode reportNode, String unit) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityProviderUnhandledUnit", "Unit ${unit} cannot be handled by the sensitivity provider as it is not a flow unit")
            .withUntypedValue("unit", unit)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("Unit {} cannot be handled by the sensitivity provider as it is not a flow unit", unit);
        return addedNode;

    }

    public static ReportNode reportNewSensitivityProvider(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewSensitivityProvider", "New sensitivity provider")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportUnableComputeSensitivityForCounterTradeRangeAction(ReportNode reportNode, String counterTradeRangeActionId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportUnableComputeSensitivityForCounterTradeRangeAction", "Unable to compute sensitivity for CounterTradeRangeAction. (${counterTradeRangeActionId})")
            .withUntypedValue("counterTradeRangeActionId", counterTradeRangeActionId)
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("Unable to compute sensitivity for CounterTradeRangeAction. ({})", counterTradeRangeActionId);
        return addedNode;
    }

    public static ReportNode reportSensitivityOnlyHandleMegawattUnit(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityOnlyHandleMegawattUnit", "PtdfSensitivity provider currently only handle Megawatt unit")
            .withSeverity(TypedValue.TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.warn("PtdfSensitivity provider currently only handle Megawatt unit");
        return addedNode;
    }

    public static ReportNode reportSystematicSensitivityLoadFlowProvider(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportSystematicSensitivityLoadFlowProvider", "New systematic sensitivity load flow provider")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportPtdfSensitivityProvider(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportPtdfSensitivityProvider", "New PTDF sensitivity provider")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportRangeActionSensitivityProvider(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRangeActionSensitivityProvider", "New range action sensitivity provider")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }
}
