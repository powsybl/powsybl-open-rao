/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.LoopFlowParameters;
import com.farao_community.farao.rao_commons.linear_optimisation.MnecParameters;

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
    private double negativeMarginObjectiveCoefficient;
    private double pstPenaltyCost;
    private double ptdfSumLowerBound;
    private MnecParameters mnecParameters;
    private LoopFlowParameters loopFlowParameters;

    public static IteratingLinearOptimizerParametersBuilder create() {
        return new IteratingLinearOptimizerParametersBuilder();
    }

    public int getMaxIterations() {
        return maxIterations;
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

    public static final class IteratingLinearOptimizerParametersBuilder {
        private int maxIterations;
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private Map<String, Integer> maxPstPerTso;
        private double pstSensitivityThreshold;
        private Set<String> operatorsNotToOptimize;
        private double negativeMarginObjectiveCoefficient;
        private double pstPenaltyCost;
        private double ptdfSumLowerBound;
        private MnecParameters mnecParameters;
        private LoopFlowParameters loopFlowParameters;

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

        public IteratingLinearOptimizerParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public IteratingLinearOptimizerParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public IteratingLinearOptimizerParameters build() {
            IteratingLinearOptimizerParameters iteratingLinearOptimizerParameters = new IteratingLinearOptimizerParameters();
            iteratingLinearOptimizerParameters.maxIterations = this.maxIterations;
            iteratingLinearOptimizerParameters.objectiveFunction = this.objectiveFunction;
            iteratingLinearOptimizerParameters.maxPstPerTso = this.maxPstPerTso;
            iteratingLinearOptimizerParameters.pstSensitivityThreshold = this.pstSensitivityThreshold;
            iteratingLinearOptimizerParameters.operatorsNotToOptimize = this.operatorsNotToOptimize;
            iteratingLinearOptimizerParameters.negativeMarginObjectiveCoefficient = this.negativeMarginObjectiveCoefficient;
            iteratingLinearOptimizerParameters.pstPenaltyCost = this.pstPenaltyCost;
            iteratingLinearOptimizerParameters.ptdfSumLowerBound = this.ptdfSumLowerBound;
            iteratingLinearOptimizerParameters.mnecParameters = this.mnecParameters;
            iteratingLinearOptimizerParameters.loopFlowParameters = this.loopFlowParameters;
            return iteratingLinearOptimizerParameters;
        }
    }
}
