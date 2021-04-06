package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LinearOptimizerParameters {
    private RaoParameters.ObjectiveFunction objectiveFunction;
    private Map<String, Integer> maxPstPerTso;
    private double pstSensitivityThreshold;
    private Set<String> operatorsNotToOptimize;
    private double mnecAcceptableMarginDiminution;
    private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;
    private double loopFlowConstraintAdjustmentCoefficient;
    private double loopFlowViolationCost;
    private double loopFlowAcceptableAugmentation;
    private double mnecViolationCost;
    private double mnecConstraintAdjustmentCoefficient;
    private double negativeMarginObjectiveCoefficient;
    private double pstPenaltyCost;
    private double ptdfSumLowerBound;
    private boolean raoWithLoopFlowLimitation;

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
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

    public static final class LinearOptimizerParametersBuilder {
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private Map<String, Integer> maxPstPerTso;
        private double pstSensitivityThreshold;
        private Set<String> operatorsNotToOptimize;
        private double mnecAcceptableMarginDiminution;
        private RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel;
        private double loopFlowConstraintAdjustmentCoefficient;
        private double loopFlowViolationCost;
        private double loopFlowAcceptableAugmentation;
        private double mnecViolationCost;
        private double mnecConstraintAdjustmentCoefficient;
        private double negativeMarginObjectiveCoefficient;
        private double pstPenaltyCost;
        private double ptdfSumLowerBound;
        private boolean raoWithLoopFlowLimitation;

        public LinearOptimizerParametersBuilder withObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
            this.maxPstPerTso = maxPstPerTso;
            return this;
        }

        public LinearOptimizerParametersBuilder withPstSensitivityThreshold(double pstSensitivityThreshold) {
            this.pstSensitivityThreshold = pstSensitivityThreshold;
            return this;
        }

        public LinearOptimizerParametersBuilder withOperatorsNotToOptimize(Set<String> operatorsNotToOptimize) {
            this.operatorsNotToOptimize = operatorsNotToOptimize;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecAcceptableMarginDiminution(double mnecAcceptableMarginDiminution) {
            this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel) {
            this.loopFlowApproximationLevel = loopFlowApproximationLevel;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowConstraintAdjustmentCoefficient(double loopFlowConstraintAdjustmentCoefficient) {
            this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowViolationCost(double loopFlowViolationCost) {
            this.loopFlowViolationCost = loopFlowViolationCost;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowAcceptableAugmentation(double loopFlowAcceptableAugmentation) {
            this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecViolationCost(double mnecViolationCost) {
            this.mnecViolationCost = mnecViolationCost;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecConstraintAdjustmentCoefficient(double mnecConstraintAdjustmentCoefficient) {
            this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
            return this;
        }

        public LinearOptimizerParametersBuilder withNegativeMarginObjectiveCoefficient(double negativeMarginObjectiveCoefficient) {
            this.negativeMarginObjectiveCoefficient = negativeMarginObjectiveCoefficient;
            return this;
        }

        public LinearOptimizerParametersBuilder withPstPenaltyCost(double pstPenaltyCost) {
            this.pstPenaltyCost = pstPenaltyCost;
            return this;
        }

        public LinearOptimizerParametersBuilder withPtdfSumLowerBound(double ptdfSumLowerBound) {
            this.ptdfSumLowerBound = ptdfSumLowerBound;
            return this;
        }

        public LinearOptimizerParametersBuilder withRaoWithLoopFlowLimitation(boolean raoWithLoopFlowLimitation) {
            this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
            return this;
        }

        public LinearOptimizerParameters build() {
            LinearOptimizerParameters linearOptimizerParameters = new LinearOptimizerParameters();
            linearOptimizerParameters.objectiveFunction = this.objectiveFunction;
            linearOptimizerParameters.maxPstPerTso = this.maxPstPerTso;
            linearOptimizerParameters.pstSensitivityThreshold = this.pstSensitivityThreshold;
            linearOptimizerParameters.operatorsNotToOptimize = this.operatorsNotToOptimize;
            linearOptimizerParameters.mnecAcceptableMarginDiminution = this.mnecAcceptableMarginDiminution;
            linearOptimizerParameters.loopFlowApproximationLevel = this.loopFlowApproximationLevel;
            linearOptimizerParameters.loopFlowConstraintAdjustmentCoefficient = this.loopFlowConstraintAdjustmentCoefficient;
            linearOptimizerParameters.loopFlowViolationCost = this.loopFlowViolationCost;
            linearOptimizerParameters.loopFlowAcceptableAugmentation = this.loopFlowAcceptableAugmentation;
            linearOptimizerParameters.mnecViolationCost = this.mnecViolationCost;
            linearOptimizerParameters.mnecConstraintAdjustmentCoefficient = this.mnecConstraintAdjustmentCoefficient;
            linearOptimizerParameters.negativeMarginObjectiveCoefficient = this.negativeMarginObjectiveCoefficient;
            linearOptimizerParameters.pstPenaltyCost = this.pstPenaltyCost;
            linearOptimizerParameters.ptdfSumLowerBound = this.ptdfSumLowerBound;
            linearOptimizerParameters.raoWithLoopFlowLimitation = this.raoWithLoopFlowLimitation;
            return linearOptimizerParameters;
        }
    }
}
