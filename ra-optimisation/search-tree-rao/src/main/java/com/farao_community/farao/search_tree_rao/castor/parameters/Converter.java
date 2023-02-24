package com.farao_community.farao.search_tree_rao.castor.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.rao_api.parameters.*;
import com.farao_community.farao.rao_api.parameters.extensions.LoopFlowParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.MnecParametersExtension;
import com.farao_community.farao.rao_api.parameters.extensions.RelativeMarginsParametersExtension;
import old.OldRaoParameters;

public final class Converter {
    private Converter() {
    }

    public static RaoParameters buildWithOldRaoParameters(OldRaoParameters oldRaoParameters) {
        OldSearchTreeRaoParameters searchTreeRaoParameters = oldRaoParameters.getExtension(OldSearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            searchTreeRaoParameters = new OldSearchTreeRaoParameters();
        }
        RaoParameters newRaoParameters = new RaoParameters();
        // Obj function parameters
        newRaoParameters.getObjectiveFunctionParameters().setObjectiveFunctionType(convertObjectiveFunctionType(oldRaoParameters.getObjectiveFunction()));
        newRaoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(oldRaoParameters.getForbidCostIncrease());
        newRaoParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(searchTreeRaoParameters.getCurativeRaoMinObjImprovement());
        newRaoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(convertPreventiveStopCriterion(searchTreeRaoParameters.getPreventiveRaoStopCriterion()));
        newRaoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(convertCurativeStopCriterion(searchTreeRaoParameters.getCurativeRaoStopCriterion()));
        // Range actions optimization parameters
        newRaoParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(oldRaoParameters.getMaxIterations());
        newRaoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(oldRaoParameters.getPstPenaltyCost());
        newRaoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(oldRaoParameters.getPstSensitivityThreshold());
        newRaoParameters.getRangeActionsOptimizationParameters().setPstModel(convertPstModel(oldRaoParameters.getPstOptimizationApproximation()));
        newRaoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(oldRaoParameters.getHvdcPenaltyCost());
        newRaoParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(oldRaoParameters.getHvdcSensitivityThreshold());
        newRaoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(oldRaoParameters.getInjectionRaPenaltyCost());
        newRaoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(oldRaoParameters.getInjectionRaSensitivityThreshold());
        newRaoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolver(convertSolver(oldRaoParameters.getSolver()));
        newRaoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setRelativeMipGap(oldRaoParameters.getRelativeMipGap());
        newRaoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().setSolverSpecificParameters(oldRaoParameters.getSolverSpecificParameters());
        // Topo Opt Param
        newRaoParameters.getTopoOptimizationParameters().setMaxSearchTreeDepth(searchTreeRaoParameters.getMaximumSearchDepth());
        newRaoParameters.getTopoOptimizationParameters().setPredefinedCombinations(searchTreeRaoParameters.getNetworkActionIdCombinations());
        newRaoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(searchTreeRaoParameters.getRelativeNetworkActionMinimumImpactThreshold());
        newRaoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(searchTreeRaoParameters.getAbsoluteNetworkActionMinimumImpactThreshold());
        newRaoParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(searchTreeRaoParameters.getSkipNetworkActionsFarFromMostLimitingElement());
        newRaoParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(searchTreeRaoParameters.getMaxNumberOfBoundariesForSkippingNetworkActions());
        // Multi threading params
        newRaoParameters.getMultithreadingParameters().setContingencyScenariosInParallel(oldRaoParameters.getPerimetersInParallel());
        newRaoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(searchTreeRaoParameters.getPreventiveLeavesInParallel());
        newRaoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(searchTreeRaoParameters.getCurativeLeavesInParallel());
        // Second Prev param
        newRaoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(convertExecutionCondition(searchTreeRaoParameters.getSecondPreventiveOptimizationCondition()));
        newRaoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(searchTreeRaoParameters.isGlobalOptimizationInSecondPreventive());
        newRaoParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(searchTreeRaoParameters.isSecondPreventiveHintFromFirstPreventive());
        // Ra Usage Limits per contingency params
        newRaoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRa(searchTreeRaoParameters.getMaxCurativeRa());
        newRaoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeTso(searchTreeRaoParameters.getMaxCurativeTso());
        newRaoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeTopoPerTso(searchTreeRaoParameters.getMaxCurativeTopoPerTso());
        newRaoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativePstPerTso(searchTreeRaoParameters.getMaxCurativePstPerTso());
        newRaoParameters.getRaUsageLimitsPerContingencyParameters().setMaxCurativeRaPerTso(searchTreeRaoParameters.getMaxCurativeRaPerTso());
        // Not Optimized Cnecs Params
        // FOLLOWING PARAM IS OPPOSITE !!
        newRaoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras());
        newRaoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCnecsSecuredByTheirPst(searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds());
        // LoadFlow and Sensi params
        newRaoParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(oldRaoParameters.getLoadFlowProvider());
        newRaoParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(oldRaoParameters.getSensitivityProvider());
        newRaoParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(10000);
        newRaoParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(oldRaoParameters.getDefaultSensitivityAnalysisParameters());
        // EXTENSIONS
        // -- LoopFlow
        if (oldRaoParameters.isRaoWithLoopFlowLimitation()) {
            newRaoParameters.addExtension(LoopFlowParametersExtension.class, LoopFlowParametersExtension.loadDefault());
            LoopFlowParametersExtension loopFlowParametersExtension = newRaoParameters.getExtension(LoopFlowParametersExtension.class);
            loopFlowParametersExtension.setAcceptableIncrease(oldRaoParameters.getLoopFlowAcceptableAugmentation());
            loopFlowParametersExtension.setApproximation(convertLFApproximation(oldRaoParameters.getLoopFlowApproximationLevel()));
            loopFlowParametersExtension.setConstraintAdjustmentCoefficient(oldRaoParameters.getLoopFlowConstraintAdjustmentCoefficient());
            loopFlowParametersExtension.setViolationCost(oldRaoParameters.getLoopFlowViolationCost());
            loopFlowParametersExtension.setCountries(oldRaoParameters.getLoopflowCountries());
        }
        // -- Mnec
        if (oldRaoParameters.isRaoWithMnecLimitation()) {
            newRaoParameters.addExtension(MnecParametersExtension.class, MnecParametersExtension.loadDefault());
            MnecParametersExtension mnecParametersExtension = newRaoParameters.getExtension(MnecParametersExtension.class);
            mnecParametersExtension.setAcceptableMarginDecrease(oldRaoParameters.getMnecAcceptableMarginDiminution());
            mnecParametersExtension.setViolationCost(oldRaoParameters.getMnecViolationCost());
            mnecParametersExtension.setConstraintAdjustmentCoefficient(oldRaoParameters.getMnecConstraintAdjustmentCoefficient());
        }
        // -- Relative Margins
        if (oldRaoParameters.getObjectiveFunction().doesRequirePtdf()) {
            newRaoParameters.addExtension(RelativeMarginsParametersExtension.class, RelativeMarginsParametersExtension.loadDefault());
            RelativeMarginsParametersExtension relativeMarginParametersExtension = newRaoParameters.getExtension(RelativeMarginsParametersExtension.class);
            relativeMarginParametersExtension.setPtdfBoundariesFromString(oldRaoParameters.getRelativeMarginPtdfBoundariesAsString());
            relativeMarginParametersExtension.setPtdfSumLowerBound(oldRaoParameters.getPtdfSumLowerBound());
        }
        return newRaoParameters;
    }

    public static ObjectiveFunctionParameters.ObjectiveFunctionType convertObjectiveFunctionType(OldRaoParameters.ObjectiveFunction oldObjFunction) {
        if (oldObjFunction.equals(OldRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT;
        } else if (oldObjFunction.equals(OldRaoParameters.ObjectiveFunction.MAX_MIN_MARGIN_IN_AMPERE)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_MARGIN_IN_AMPERE;
        } else if (oldObjFunction.equals(OldRaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT;
        } else if (oldObjFunction.equals(OldRaoParameters.ObjectiveFunction.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE)) {
            return ObjectiveFunctionParameters.ObjectiveFunctionType.MAX_MIN_RELATIVE_MARGIN_IN_AMPERE;
        } else {
            throw new FaraoException("Wrong objective function type !!");
        }
    }

    public static ObjectiveFunctionParameters.PreventiveStopCriterion convertPreventiveStopCriterion(OldSearchTreeRaoParameters.PreventiveRaoStopCriterion oldPrevRaoStopCriterion) {
        if (oldPrevRaoStopCriterion.equals(OldSearchTreeRaoParameters.PreventiveRaoStopCriterion.MIN_OBJECTIVE)) {
            return ObjectiveFunctionParameters.PreventiveStopCriterion.MIN_OBJECTIVE;
        } else if (oldPrevRaoStopCriterion.equals(OldSearchTreeRaoParameters.PreventiveRaoStopCriterion.SECURE)) {
            return ObjectiveFunctionParameters.PreventiveStopCriterion.SECURE;
        } else {
            throw new FaraoException("Wrong preventive rao stop criterion !!");
        }
    }

    public static ObjectiveFunctionParameters.CurativeStopCriterion convertCurativeStopCriterion(OldSearchTreeRaoParameters.CurativeRaoStopCriterion oldCurRaoStopCriterion) {
        if (oldCurRaoStopCriterion.equals(OldSearchTreeRaoParameters.CurativeRaoStopCriterion.MIN_OBJECTIVE)) {
            return ObjectiveFunctionParameters.CurativeStopCriterion.MIN_OBJECTIVE;
        } else if (oldCurRaoStopCriterion.equals(OldSearchTreeRaoParameters.CurativeRaoStopCriterion.SECURE)) {
            return ObjectiveFunctionParameters.CurativeStopCriterion.SECURE;
        } else if (oldCurRaoStopCriterion.equals(OldSearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE)) {
            return ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE;
        } else if (oldCurRaoStopCriterion.equals(OldSearchTreeRaoParameters.CurativeRaoStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE)) {
            return ObjectiveFunctionParameters.CurativeStopCriterion.PREVENTIVE_OBJECTIVE_AND_SECURE;
        } else {
            throw new FaraoException("Wrong curative rao stop criterion !!");
        }
    }

    public static RangeActionsOptimizationParameters.Solver convertSolver(OldRaoParameters.Solver oldSolver) {
        if (oldSolver.equals(OldRaoParameters.Solver.CBC)) {
            return RangeActionsOptimizationParameters.Solver.CBC;
        } else if (oldSolver.equals(OldRaoParameters.Solver.SCIP)) {
            return RangeActionsOptimizationParameters.Solver.SCIP;
        } else if (oldSolver.equals(OldRaoParameters.Solver.XPRESS)) {
            return RangeActionsOptimizationParameters.Solver.XPRESS;
        } else {
            throw new FaraoException("Wrong Solver model !!");
        }
    }

    public static RangeActionsOptimizationParameters.PstModel convertPstModel(OldRaoParameters.PstOptimizationApproximation oldPstModel) {
        if (oldPstModel.equals(OldRaoParameters.PstOptimizationApproximation.CONTINUOUS)) {
            return RangeActionsOptimizationParameters.PstModel.CONTINUOUS;
        } else if (oldPstModel.equals(OldRaoParameters.PstOptimizationApproximation.APPROXIMATED_INTEGERS)) {
            return RangeActionsOptimizationParameters.PstModel.APPROXIMATED_INTEGERS;
        } else {
            throw new FaraoException("Wrong Pst model !!");
        }
    }

    public static SecondPreventiveRaoParameters.ExecutionCondition convertExecutionCondition(OldSearchTreeRaoParameters.SecondPreventiveRaoCondition oldCondition) {
        if (oldCondition.equals(OldSearchTreeRaoParameters.SecondPreventiveRaoCondition.DISABLED)) {
            return SecondPreventiveRaoParameters.ExecutionCondition.DISABLED;
        } else if (oldCondition.equals(OldSearchTreeRaoParameters.SecondPreventiveRaoCondition.COST_INCREASE)) {
            return SecondPreventiveRaoParameters.ExecutionCondition.COST_INCREASE;
        } else if (oldCondition.equals(OldSearchTreeRaoParameters.SecondPreventiveRaoCondition.POSSIBLE_CURATIVE_IMPROVEMENT)) {
            return SecondPreventiveRaoParameters.ExecutionCondition.POSSIBLE_CURATIVE_IMPROVEMENT;
        } else {
            throw new FaraoException("Wrong Execution condition model !!");
        }
    }

    public static LoopFlowParametersExtension.Approximation convertLFApproximation(OldRaoParameters.LoopFlowApproximationLevel oldApprox) {
        if (oldApprox.equals(OldRaoParameters.LoopFlowApproximationLevel.FIXED_PTDF)) {
            return LoopFlowParametersExtension.Approximation.FIXED_PTDF;
        } else if (oldApprox.equals(OldRaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO)) {
            return LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO;
        } else if (oldApprox.equals(OldRaoParameters.LoopFlowApproximationLevel.UPDATE_PTDF_WITH_TOPO_AND_PST)) {
            return LoopFlowParametersExtension.Approximation.UPDATE_PTDF_WITH_TOPO_AND_PST;
        } else {
            throw new FaraoException("Wrong Approximation model !!");
        }
    }
}

