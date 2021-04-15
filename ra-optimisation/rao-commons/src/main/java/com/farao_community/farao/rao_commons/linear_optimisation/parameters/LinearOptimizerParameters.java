/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.linear_optimisation.parameters;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.RaoParameters;

import java.util.Set;

public class LinearOptimizerParameters {
    private RaoParameters.ObjectiveFunction objectiveFunction;
    private double pstSensitivityThreshold;
    private Set<String> operatorsNotToOptimize;
    private double negativeMarginObjectiveCoefficient;
    private double pstPenaltyCost;
    private double ptdfSumLowerBound;
    private MnecParameters mnecParameters;
    private LoopFlowParameters loopFlowParameters;
    private boolean raoWithLoopFlowLimitation;

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
    }

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public Unit getUnit() {
        return objectiveFunction.getUnit();
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

    public boolean isRaoWithLoopFlowLimitation() {
        return raoWithLoopFlowLimitation;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public static final class LinearOptimizerParametersBuilder {
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private double pstSensitivityThreshold;
        private Set<String> operatorsNotToOptimize;
        private MnecParameters mnecParameters;
        private LoopFlowParameters loopFlowParameters;
        private double negativeMarginObjectiveCoefficient;
        private double pstPenaltyCost;
        private double ptdfSumLowerBound;
        private boolean raoWithLoopFlowLimitation;

        public LinearOptimizerParametersBuilder withObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
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

        public LinearOptimizerParametersBuilder withRaoLoopFlowLimitation(boolean raoWithLoopFlowLimitation) {
            this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
            return this;
        }

        public LinearOptimizerParameters build() {
            LinearOptimizerParameters linearOptimizerParameters = new LinearOptimizerParameters();
            linearOptimizerParameters.objectiveFunction = this.objectiveFunction;
            linearOptimizerParameters.pstSensitivityThreshold = this.pstSensitivityThreshold;
            linearOptimizerParameters.operatorsNotToOptimize = this.operatorsNotToOptimize;
            linearOptimizerParameters.negativeMarginObjectiveCoefficient = this.negativeMarginObjectiveCoefficient;
            linearOptimizerParameters.pstPenaltyCost = this.pstPenaltyCost;
            linearOptimizerParameters.ptdfSumLowerBound = this.ptdfSumLowerBound;
            linearOptimizerParameters.mnecParameters = this.mnecParameters;
            linearOptimizerParameters.loopFlowParameters = this.loopFlowParameters;
            linearOptimizerParameters.raoWithLoopFlowLimitation = this.raoWithLoopFlowLimitation;
            return linearOptimizerParameters;
        }
    }
}
