/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;

import java.util.Map;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class IteratingLinearOptimizerParameters {
    private int maxIterations;
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

    public static IteratingLinearOptimizerParametersBuilder create() {
        return new IteratingLinearOptimizerParametersBuilder();
    }

    public int getMaxIterations() {
        return maxIterations;
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

    public static final class IteratingLinearOptimizerParametersBuilder {
        private int maxIterations;
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

        public IteratingLinearOptimizerParametersBuilder withMaxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withMaxPstPerTso(Map<String, Integer> maxPstPerTso) {
            this.maxPstPerTso = maxPstPerTso;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withPstSensitivityThreshold(double pstSensitivityThreshold) {
            this.pstSensitivityThreshold = pstSensitivityThreshold;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withOperatorsNotToOptimize(Set<String> operatorsNotToOptimize) {
            this.operatorsNotToOptimize = operatorsNotToOptimize;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withMnecAcceptableMarginDiminution(double mnecAcceptableMarginDiminution) {
            this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withLoopFlowApproximationLevel(RaoParameters.LoopFlowApproximationLevel loopFlowApproximationLevel) {
            this.loopFlowApproximationLevel = loopFlowApproximationLevel;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withLoopFlowConstraintAdjustmentCoefficient(double loopFlowConstraintAdjustmentCoefficient) {
            this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withLoopFlowViolationCost(double loopFlowViolationCost) {
            this.loopFlowViolationCost = loopFlowViolationCost;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withLoopFlowAcceptableAugmentation(double loopFlowAcceptableAugmentation) {
            this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withMnecViolationCost(double mnecViolationCost) {
            this.mnecViolationCost = mnecViolationCost;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withMnecConstraintAdjustmentCoefficient(double mnecConstraintAdjustmentCoefficient) {
            this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withNegativeMarginObjectiveCoefficient(double negativeMarginObjectiveCoefficient) {
            this.negativeMarginObjectiveCoefficient = negativeMarginObjectiveCoefficient;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withPstPenaltyCost(double pstPenaltyCost) {
            this.pstPenaltyCost = pstPenaltyCost;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withPtdfSumLowerBound(double ptdfSumLowerBound) {
            this.ptdfSumLowerBound = ptdfSumLowerBound;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withRaoWithLoopFlowLimitation(boolean raoWithLoopFlowLimitation) {
            this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
            return this;
        }

        public IteratingLinearOptimizerParameters build() {
            IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters = new IteratingLinearOptimizerParameters();
            iteratingLinearOptimizerParameters.maxIterations = this.maxIterations;
            iteratingLinearOptimizerParameters.objectiveFunction = this.objectiveFunction;
            iteratingLinearOptimizerParameters.maxPstPerTso = this.maxPstPerTso;
            iteratingLinearOptimizerParameters.pstSensitivityThreshold = this.pstSensitivityThreshold;
            iteratingLinearOptimizerParameters.operatorsNotToOptimize = this.operatorsNotToOptimize;
            iteratingLinearOptimizerParameters.mnecAcceptableMarginDiminution = this.mnecAcceptableMarginDiminution;
            iteratingLinearOptimizerParameters.loopFlowApproximationLevel = this.loopFlowApproximationLevel;
            iteratingLinearOptimizerParameters.loopFlowConstraintAdjustmentCoefficient = this.loopFlowConstraintAdjustmentCoefficient;
            iteratingLinearOptimizerParameters.loopFlowViolationCost = this.loopFlowViolationCost;
            iteratingLinearOptimizerParameters.loopFlowAcceptableAugmentation = this.loopFlowAcceptableAugmentation;
            iteratingLinearOptimizerParameters.mnecViolationCost = this.mnecViolationCost;
            iteratingLinearOptimizerParameters.mnecConstraintAdjustmentCoefficient = this.mnecConstraintAdjustmentCoefficient;
            iteratingLinearOptimizerParameters.negativeMarginObjectiveCoefficient = this.negativeMarginObjectiveCoefficient;
            iteratingLinearOptimizerParameters.pstPenaltyCost = this.pstPenaltyCost;
            iteratingLinearOptimizerParameters.ptdfSumLowerBound = this.ptdfSumLowerBound;
            iteratingLinearOptimizerParameters.raoWithLoopFlowLimitation = this.raoWithLoopFlowLimitation;
            return iteratingLinearOptimizerParameters;
        }
    }
}
