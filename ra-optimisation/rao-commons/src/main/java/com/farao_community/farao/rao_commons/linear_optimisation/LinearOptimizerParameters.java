package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.Country;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LinearOptimizerParameters {
    RaoParameters.ObjectiveFunction objectiveFunction;
    Map<String, Integer> maxPstPerTso;
    double pstSensitivityThreshold;
    Set<String> operatorsNotToOptimize;
    private double mnecAcceptableMarginDiminution;
    private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowViolationCost;
    private Set<Country> loopflowCountries; // to replace preprocessing in raodata
    private double loopFlowAcceptableAugmentation;
    private double mnecViolationCost;
    private double mnecConstraintAdjustmentCoefficient;
    private double negativeMarginObjectiveCoefficient;
    private double pstPenaltyCost;
    private double ptdfSumLowerBound;
    private boolean raoWithLoopFlowLimitation;

    public static LinearOptimizerParameters createFromRaoParameters(RaoParameters raoParameters) {

    }

    public boolean isRaoWithLoopFlowLimitation() {
        return raoWithLoopFlowLimitation;
    }

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public Map<String, Integer> getMaxPstPerTso() {
        return maxPstPerTso;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorsNotToOptimize;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public RaoParameters.LoopFlowApproximationLevel getLoopFlowApproximationLevel() {
        return loopFlowApproximationLevel;
    }

    public double getLoopFlowConstraintAdjustmentCoefficient() {
        return loopFlowConstraintAdjustmentCoefficient;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public Set<Country> getLoopflowCountries() {
        return loopflowCountries;
    }

    public double getLoopFlowAcceptableAugmentation() {
        return loopFlowAcceptableAugmentation;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }

    public double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }
}
