package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_api.RaoParameters;

import java.util.Map;
import java.util.Set;

public class LinearOptimizerParameters {
    private RaoParameters.ObjectiveFunction objectiveFunction;
    private Map<String, Integer> maxPstPerTso;
    private double pstSensitivityThreshold;
    private Set<String> operatorsNotToOptimize;
    private double negativeMarginObjectiveCoefficient;
    private double pstPenaltyCost;
    private double ptdfSumLowerBound;
    private MnecParameters mnecParameters;
    private LoopFlowParameters loopFlowParameters;

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
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

    public double getNegativeMarginObjectiveCoefficient() {
        return negativeMarginObjectiveCoefficient;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public double getPtdfSumLowerBound() {
        return ptdfSumLowerBound;
    }

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public static final class LinearOptimizerParametersBuilder {
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private Map<String, Integer> maxPstPerTso;
        private double pstSensitivityThreshold;
        private Set<String> operatorsNotToOptimize;
        private MnecParameters mnecParameters;
        private LoopFlowParameters loopFlowParameters;
        private double negativeMarginObjectiveCoefficient;
        private double pstPenaltyCost;
        private double ptdfSumLowerBound;

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

        public LinearOptimizerParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public LinearOptimizerParameters build() {
            LinearOptimizerParameters linearOptimizerParameters = new LinearOptimizerParameters();
            linearOptimizerParameters.objectiveFunction = this.objectiveFunction;
            linearOptimizerParameters.maxPstPerTso = this.maxPstPerTso;
            linearOptimizerParameters.pstSensitivityThreshold = this.pstSensitivityThreshold;
            linearOptimizerParameters.operatorsNotToOptimize = this.operatorsNotToOptimize;
            linearOptimizerParameters.negativeMarginObjectiveCoefficient = this.negativeMarginObjectiveCoefficient;
            linearOptimizerParameters.pstPenaltyCost = this.pstPenaltyCost;
            linearOptimizerParameters.ptdfSumLowerBound = this.ptdfSumLowerBound;
            linearOptimizerParameters.mnecParameters = this.mnecParameters;
            linearOptimizerParameters.loopFlowParameters = this.loopFlowParameters;
            return linearOptimizerParameters;
        }
    }
}
