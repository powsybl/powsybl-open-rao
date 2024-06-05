package com.powsybl.openrao.searchtreerao.castor.algorithm;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.time.Instant;
import java.util.Locale;

import static com.powsybl.commons.report.TypedValue.*;
import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.*;

public final class CastorAlgorithmReports {
    private CastorAlgorithmReports() {
        // Utility class
    }

    private static String formatDouble(double value) {
        if (value >= Double.MAX_VALUE) {
            return "+infinity";
        } else if (value <= -Double.MAX_VALUE) {
            return "-infinity";
        } else {
            return String.format(Locale.ENGLISH, "%.2f", value);
        }
    }

    public static ReportNode reportRao(String networkId, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("rao", "Running RAO on network '${networkId}'")
            .withUntypedValue("networkId", networkId)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Running RAO on network '{}'", networkId);
        return addedNode;
    }

    public static ReportNode reportRaoFailure(String state, Exception exception, ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raoFailure", "Optimizing state '${state}' failed with message: ${message}")
            .withUntypedValue("state", state)
            .withUntypedValue("message", exception.getMessage())
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Optimizing state \"{}\" failed: ", state, exception);
        return addedNode;
    }

    public static ReportNode reportInitialSensitivity(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("initialSensitivity", "Initial sensitivity analysis")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Initial sensitivity analysis");
        return addedNode;
    }

    public static ReportNode reportPreventivePerimeter(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("preventivePerimeter", "Preventive perimeter optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreventivePerimeterEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("preventivePerimeterEnd", "End of preventive perimeter optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportPreCurativeSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("preCurativeSensitivityFailure", "Systematic sensitivity analysis after preventive remedial actions failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions failed");
        return addedNode;
    }

    public static ReportNode reportDoNotOptimizeCurativeBecausePreventiveUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("doNotOptimizeCurativeBecausePreventiveUnsecure", "Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Preventive perimeter could not be secured; there is no point in optimizing post-contingency perimeters. The RAO will be interrupted here.");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimeters(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("postContingencyPerimeters", "Post contingency perimeters optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPostContingencyPerimetersEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("postContingencyPerimetersEnd", "End of post contingency perimeters optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Post-contingency perimeters optimization [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("resultsMergingPreventiveAndPostContingency", "Merging preventive and post-contingency RAO results")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Merging preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveFailed", "Second preventive failed. Falling back to previous solution")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Second preventive failed. Falling back to previous solution:");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveFixedSituation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveFixedSituation", "RAO has succeeded thanks to second preventive step when first preventive step had failed")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("RAO has succeeded thanks to second preventive step when first preventive step had failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveIncreasedOverallCost(ReportNode reportNode, double firstPreventiveCost, double firstPreventiveFunctionalCost, double firstPreventiveVirtualCost, double secondPreventiveCost, double secondPreventiveFunctionalCost, double secondPreventiveVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveIncreasedOverallCost", "Second preventive step has increased the overall cost from ${firstPreventiveCost} (functional: ${firstPreventiveFunctionalCost}, virtual: ${firstPreventiveVirtualCost}) to ${secondPreventiveCost} (functional: ${secondPreventiveFunctionalCost}, virtual: ${secondPreventiveVirtualCost}). Falling back to previous solution:")
            .withUntypedValue("firstPreventiveCost", firstPreventiveCost)
            .withUntypedValue("firstPreventiveFunctionalCost", firstPreventiveFunctionalCost)
            .withUntypedValue("firstPreventiveVirtualCost", firstPreventiveVirtualCost)
            .withUntypedValue("secondPreventiveCost", secondPreventiveCost)
            .withUntypedValue("secondPreventiveFunctionalCost", secondPreventiveFunctionalCost)
            .withUntypedValue("secondPreventiveVirtualCost", secondPreventiveVirtualCost)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Second preventive step has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to previous solution:",
            formatDouble(firstPreventiveCost), formatDouble(firstPreventiveFunctionalCost), formatDouble(firstPreventiveVirtualCost),
            formatDouble(secondPreventiveCost), formatDouble(secondPreventiveFunctionalCost), formatDouble(secondPreventiveVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoIncreasedOverallCost(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveIncreasedOverallCost", "RAO has increased the overall cost from ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}) to ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost}). Falling back to initial solution:")
            .withUntypedValue("initialCost", initialCost)
            .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
            .withUntypedValue("initialVirtualCost", initialVirtualCost)
            .withUntypedValue("finalCost", finalCost)
            .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
            .withUntypedValue("finalVirtualCost", finalVirtualCost)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("RAO has increased the overall cost from {} (functional: {}, virtual: {}) to {} (functional: {}, virtual: {}). Falling back to initial solution:",
            formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
            formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportRaoCostEvolution(ReportNode reportNode, double initialCost, double initialFunctionalCost, double initialVirtualCost, double finalCost, double finalFunctionalCost, double finalVirtualCost) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("raoCostEvolution", "Cost before RAO = ${initialCost} (functional: ${initialFunctionalCost}, virtual: ${initialVirtualCost}), cost after RAO = ${finalCost} (functional: ${finalFunctionalCost}, virtual: ${finalVirtualCost})")
            .withUntypedValue("initialCost", initialCost)
            .withUntypedValue("initialFunctionalCost", initialFunctionalCost)
            .withUntypedValue("initialVirtualCost", initialVirtualCost)
            .withUntypedValue("finalCost", finalCost)
            .withUntypedValue("finalFunctionalCost", finalFunctionalCost)
            .withUntypedValue("finalVirtualCost", finalVirtualCost)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Cost before RAO = {} (functional: {}, virtual: {}), cost after RAO = {} (functional: {}, virtual: {})",
            formatDouble(initialCost), formatDouble(initialFunctionalCost), formatDouble(initialVirtualCost),
            formatDouble(finalCost), formatDouble(finalFunctionalCost), formatDouble(finalVirtualCost));
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondAutomatonSimulation", "Second automaton simulation")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [start]");
        return addedNode;
    }

    public static ReportNode reportSecondAutomatonSimulationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondAutomatonSimulationEnd", "End of second automaton simulation")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Second automaton simulation [end]");
        return addedNode;
    }

    public static ReportNode reportResultsMergingPreventiveFirstAndSecondAndPostContingency(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("resultsMergingPreventiveFirstAndSecondAndPostContingency", "Merging first, second preventive and post-contingency RAO results")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Merging first, second preventive and post-contingency RAO results:");
        return addedNode;
    }

    public static ReportNode reportPostRaoSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("postRaoSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Systematic sensitivity analysis after curative remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimization(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveOptimization", "Second preventive perimeter optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [start]");
        return addedNode;
    }

    public static ReportNode reportPreSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) { // TODO not used !
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("preSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after curative remedial actions before second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportPostSecondPreventiveSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("postSecondPreventiveSensitivityAnalysisFailed", "Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Systematic sensitivity analysis after preventive remedial actions after second preventive optimization failed");
        return addedNode;
    }

    public static ReportNode reportSecondPreventiveOptimizationEnd(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("secondPreventiveOptimizationEnd", "End of second preventive perimeter optimization")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("----- Second preventive perimeter optimization [end]");
        return addedNode;
    }

    public static ReportNode reportRangeActionRemovedFromSecondCurative(ReportNode reportNode, String rangeActionId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("rangeActionRemovedFromSecondCurative", "Range action ${rangeActionId} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)")
            .withUntypedValue("rangeActionId", rangeActionId)
            .withSeverity(TypedValue.WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Range action {} will not be considered in 2nd preventive RAO as it is also curative (or its network element has an associated CRA)", rangeActionId);
        return addedNode;
    }

    public static ReportNode reportNotEnoughTimeForSecondPreventive(ReportNode reportNode, Instant targetEndInstant, long estimatedPreventiveRaoTimeInSeconds) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("notEnoughTimeForSecondPreventive", "There is not enough time to run a 2nd preventive RAO (target end time: ${targetEndInstant}, estimated time needed based on first preventive RAO: ${estimatedPreventiveRaoTimeInSeconds} seconds)")
            .withUntypedValue("targetEndInstant", targetEndInstant.toString())
            .withUntypedValue("estimatedPreventiveRaoTimeInSeconds", estimatedPreventiveRaoTimeInSeconds)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("There is not enough time to run a 2nd preventive RAO (target end time: {}, estimated time needed based on first preventive RAO: {} seconds)", targetEndInstant, estimatedPreventiveRaoTimeInSeconds);
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("noNeedSecondPreventiveBecauseCostHasNotIncreasedDuringRao", "Cost has not increased during RAO, there is no need to run a 2nd preventive RAO")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("Cost has not increased during RAO, there is no need to run a 2nd preventive RAO.");
        return addedNode;
    }

    public static ReportNode reportNoNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("noNeedSecondPreventiveBecauseFirstPreventiveRaoUnsecure", "First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time")
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info("First preventive RAO was not able to fix all preventive constraints, second preventive RAO cancelled to save computation time.");
        return addedNode;
    }

    public static ReportNode reportRunCastorFullOptimization(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRunCastorFullOptimization", "Run castor full optimization")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportRunCastorOneStateOnly(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportRunCastorOneStateOnly", "Run castor one state only")
            .withSeverity(TypedValue.INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportInitialSensitivityFailure(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("initialSensitivityFailure", "Initial sensitivity analysis failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
        return addedNode;
    }

    public static ReportNode reportNewAutomatonSimulator(ReportNode reportNode) {
        return reportNode.newReportNode()
            .withMessageTemplate("reportNewAutomatonSimulator", "New  automaton simulator")
            .withSeverity(INFO_SEVERITY)
            .add();
    }

    public static ReportNode reportOptimizingAutomatingState(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportOptimizingAutomatingState", "Optimizing automaton state ${automatonStateId}.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Optimizing automaton state {}.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonSimulatorInitialSituation(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSimulatorInitialSituation", "Initial situation:")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Initial situation:");
        return addedNode;
    }

    public static ReportNode reportAutomatonStateOptimized(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonStateOptimized", "Automaton state ${automatonStateId} has been optimized.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton state {} has been optimized.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonSimulationFailedRangeActionSensitivityComputation(ReportNode reportNode, String stateId, String instantQualifier) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSimulationFailedRangeActionSensitivityComputation", "Automaton state ${stateId} has failed during sensitivity computation ${instantQualifier} automaton simulation.")
            .withUntypedValue("stateId", stateId)
            .withUntypedValue("instantQualifier", instantQualifier)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton state {} has failed during sensitivity computation {} automaton simulation.", stateId, instantQualifier);
        return addedNode;
    }

    public static ReportNode reportHvdcRangeActionNotActivatedOutsideRange(ReportNode reportNode, String hvdcRaId, double activePowerSetpoint, double minAdmissibleSetpoint, double maxAdmissibleSetpoint) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportHvdcRangeActionNotActivatedOutsideRange", "HVDC range action ${hvdcRaId} could not be activated because its initial set-point (${activePowerSetpoint}) does not fall within its allowed range (${minAdmissibleSetpoint} - ${maxAdmissibleSetpoint})")
            .withUntypedValue("hvdcRaId", hvdcRaId)
            .withUntypedValue("activePowerSetpoint", activePowerSetpoint)
            .withUntypedValue("minAdmissibleSetpoint", minAdmissibleSetpoint)
            .withUntypedValue("maxAdmissibleSetpoint", maxAdmissibleSetpoint)
            .withSeverity(INFO_SEVERITY)
            .add();
        BUSINESS_LOGS.info(String.format("HVDC range action %s could not be activated because its initial set-point (%.1f) does not fall within its allowed range (%.1f - %.1f)",
            hvdcRaId, activePowerSetpoint, minAdmissibleSetpoint, maxAdmissibleSetpoint));
        return addedNode;
    }

    public static ReportNode reportSensitivityAnalysisFailed(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportSensitivityAnalysisFailed", "Initial sensitivity analysis failed")
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error("Initial sensitivity analysis failed");
        return addedNode;
    }

    public static ReportNode reportCastorError(ReportNode reportNode, String message) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportCastorError", "${message}")
            .withUntypedValue("message", message)
            .withSeverity(TypedValue.ERROR_SEVERITY)
            .add();
        BUSINESS_LOGS.error(message);
        return addedNode;
    }

    public static ReportNode reportAutomatonSkipped(ReportNode reportNode, String networkActionId, String networkActionName) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSkipped", "Automaton ${networkActionId} - ${networkActionName} has been skipped as it has no impact on network.")
            .withUntypedValue("networkActionId", networkActionId)
            .withUntypedValue("networkActionName", networkActionName)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton {} - {} has been skipped as it has no impact on network.", networkActionId, networkActionName);
        return addedNode;
    }

    public static ReportNode reportTopologicalAutomatonSkipped(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportTopologicalAutomatonSkipped", "Topological automaton state ${automatonStateId} has been skipped as no topological automatons were activated.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Topological automaton state {} has been skipped as no topological automatons were activated.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonActivated(ReportNode reportNode, String id, String name) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonActivated", "Activating automaton ${id} - ${name}.")
            .withUntypedValue("id", id)
            .withUntypedValue("name", name)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Activating automaton {} - {}.", id, name);
        return addedNode;
    }

    public static ReportNode reportRunSensitivityAnalysisPostApplicationForState(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRunSensitivityAnalysisPostApplicationForState", "Running sensitivity analysis post application of auto network actions for automaton state ${automatonStateId}.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Running sensitivity analysis post application of auto network actions for automaton state {}.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonStateOptimizedNoAutomatonRangeActionAvailable(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonStateOptimizedNoAutomatonRangeActionAvailable", "Automaton state ${automatonStateId} has been optimized (no automaton range actions available).")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Automaton state {} has been optimized (no automaton range actions available).", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportAutomatonSimulatorRangeActionIgnoredNoSpeed(ReportNode reportNode, String rangeActionId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportAutomatonSimulatorRangeActionIgnoredNoSpeed", "Range action ${rangeActionId} will not be considered in RAO as no speed is defined.")
            .withUntypedValue("rangeActionId", rangeActionId)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Range action {} will not be considered in RAO as no speed is defined", rangeActionId);
        return addedNode;
    }

    public static ReportNode reportHeterogenousRangeActionGroupTypes(ReportNode reportNode, String rangeActionGroupId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportHeterogenousRangeActionGroupTypes", "Range action group ${rangeActionGroupId} contains range actions of different types; they are not simulated")
            .withUntypedValue("rangeActionGroupId", rangeActionGroupId)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Range action group {} contains range actions of different types; they are not simulated", rangeActionGroupId);
        return addedNode;
    }

    public static ReportNode reportRangeActionGroupNotAllAvailableAtAutoInstant(ReportNode reportNode, String rangeActionGroupId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRangeActionGroupNotAllAvailableAtAutoInstant", "Range action group ${rangeActionGroupId} contains range actions not all available at AUTO instant; they are not simulated")
            .withUntypedValue("rangeActionGroupId", rangeActionGroupId)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Range action group {} contains range actions not all available at AUTO instant; they are not simulated", rangeActionGroupId);
        return addedNode;
    }

    public static ReportNode reportRunPreCurativeSensitivityAnalysis(ReportNode reportNode, String automatonStateId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRunPreCurativeSensitivityAnalysis", "Running pre curative sensitivity analysis after auto state ${automatonStateId}.")
            .withUntypedValue("automatonStateId", automatonStateId)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Running pre curative sensitivity analysis after auto state {}.", automatonStateId);
        return addedNode;
    }

    public static ReportNode reportRunLoadFlowForHvdcAngleDroopActivePowerControlSetPoint(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRunLoadFlowForHvdcAngleDroopActivePowerControlSetPoint", "Running load-flow computation to access HvdcAngleDroopActivePowerControl set-point values.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Running load-flow computation to access HvdcAngleDroopActivePowerControl set-point values.");
        return addedNode;
    }

    public static ReportNode reportRunSensitivityAnalysisAfterDisablingAngleDroopActivePowerControlOnHvdcRa(ReportNode reportNode) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportRunSensitivityAnalysisAfterDisablingAngleDroopActivePowerControlOnHvdcRa", "Running sensitivity analysis after disabling AngleDroopActivePowerControl on HVDC RAs.")
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.info("Running sensitivity analysis after disabling AngleDroopActivePowerControl on HVDC RAs.");
        return addedNode;
    }

    public static ReportNode reportDisablingAngleDroopActivePowerControl(ReportNode reportNode, String hvdcId, double activePowerSetpoint) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportDisablingAngleDroopActivePowerControl", "Disabling HvdcAngleDroopActivePowerControl on HVDC line ${hvdcId} and setting its set-point to ${activePowerSetpoint}.}")
            .withUntypedValue("hvdcId", hvdcId)
            .withUntypedValue("activePowerSetpoint", activePowerSetpoint)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Disabling HvdcAngleDroopActivePowerControl on HVDC line {} and setting its set-point to {}", hvdcId, activePowerSetpoint);
        return addedNode;
    }

    public static ReportNode reportShiftSetPointOfRangeActionToSecureCnecOnSide(ReportNode reportNode, String currentSetPoint, String optimalSetpoint, String rangeActionIds, String cnecId, String side, String cnecMargin) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportShiftSetPointOfRangeActionToSecureCnecOnSide", "Shifting set-point from ${currentSetPoint} to ${optimalSetpoint} on range action(s) \"${rangeActionIds}\" to secure CNEC ${rangeActionIds} on side ${side} (current margin: ${cnecMargin} MW).")
            .withUntypedValue("currentSetPoint", currentSetPoint)
            .withUntypedValue("optimalSetpoint", optimalSetpoint)
            .withUntypedValue("rangeActionIds", rangeActionIds)
            .withUntypedValue("cnecId", cnecId)
            .withUntypedValue("side", side)
            .withUntypedValue("cnecMargin", cnecMargin)
            .withSeverity(TRACE_SEVERITY)
            .add();
        TECHNICAL_LOGS.debug("Shifting set-point from {} to {} on range action(s) {} to secure CNEC {} on side {} (current margin: {} MW).",
            currentSetPoint,
            optimalSetpoint,
            rangeActionIds,
            cnecId,
            side,
            cnecMargin);
        return addedNode;
    }

    public static ReportNode reportNoCnecAfterContingencyWithAutomatonOrCurativeRemedialAction(ReportNode reportNode, String contingencyId) {
        ReportNode addedNode = reportNode.newReportNode()
            .withMessageTemplate("reportNoCnecAfterContingencyWithAutomatonOrCurativeRemedialAction", "Contingency ${contingencyId} has an automaton or a curative remedial action but no CNECs associated.")
            .withUntypedValue("contingencyId", contingencyId)
            .withSeverity(WARN_SEVERITY)
            .add();
        BUSINESS_WARNS.warn("Contingency {} has an automaton or a curative remedial action but no CNECs associated.", contingencyId);
        return addedNode;
    }
}
