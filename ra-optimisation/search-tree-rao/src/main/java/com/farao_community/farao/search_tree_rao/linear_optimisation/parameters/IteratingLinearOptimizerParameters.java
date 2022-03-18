/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.linear_optimisation.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.commons.parameters.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizerParameters {

    private final RaoParameters.ObjectiveFunction objectiveFunction;

    private final RangeActionParameters rangeActionParameters;
    private final MnecParameters mnecParameters;
    private final MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
    private final LoopFlowParameters loopFlowParameters;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final RangeActionLimitationParameters raLimitationParameters;
    private final SolverParameters solverParameters;

    private final int maxNumberOfIterations;

    private IteratingLinearOptimizerParameters(RaoParameters.ObjectiveFunction objectiveFunction,
                                               RangeActionParameters rangeActionParameters,
                                               MnecParameters mnecParameters,
                                               MaxMinRelativeMarginParameters maxMinRelativeMarginParameters,
                                               LoopFlowParameters loopFlowParameters,
                                               UnoptimizedCnecParameters unoptimizedCnecParameters,
                                               RangeActionLimitationParameters raLimitationParameters,
                                               SolverParameters solverParameters,
                                               int maxNumberOfIterations) {
        this.objectiveFunction = objectiveFunction;
        this.rangeActionParameters = rangeActionParameters;
        this.mnecParameters = mnecParameters;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
        this.unoptimizedCnecParameters = unoptimizedCnecParameters;
        this.raLimitationParameters = raLimitationParameters;
        this.solverParameters = solverParameters;
        this.maxNumberOfIterations = maxNumberOfIterations;
    }

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public Unit getObjectiveFunctionUnit() {
        return getObjectiveFunction().getUnit();
    }

    public boolean hasRelativeMargins() {
        return getObjectiveFunction().relativePositiveMargins();
    }

    public boolean hasOperatorsNotToOptimize() {
        return unoptimizedCnecParameters != null
            && !unoptimizedCnecParameters.getOperatorsNotToOptimize().isEmpty();
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return loopFlowParameters != null;
    }

    public boolean isRaoWithMnecLimitation() {
        return mnecParameters != null;
    }

    public RangeActionParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public MaxMinRelativeMarginParameters getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public RangeActionLimitationParameters getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public SolverParameters getSolver() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
    }

    public static class LinearOptimizerParametersBuilder {

        private RaoParameters.ObjectiveFunction objectiveFunction;
        private RangeActionParameters rangeActionParameters;
        private MnecParameters mnecParameters;
        private MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
        private LoopFlowParameters loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RangeActionLimitationParameters raLimitationParameters;
        private SolverParameters solverParameters;
        private int maxNumberOfIterations;

        public LinearOptimizerParametersBuilder withObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public LinearOptimizerParametersBuilder withRangeActionParameters(RangeActionParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withRaLimitationParameters(RangeActionLimitationParameters raLimitationParameters) {
            this.raLimitationParameters = raLimitationParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withSolverParameters(SolverParameters solverParameters) {
            this.solverParameters = solverParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxNumberOfIterations(int maxNumberOfIterations) {
            this.maxNumberOfIterations = maxNumberOfIterations;
            return this;
        }

        public IteratingLinearOptimizerParameters build() {
            if (objectiveFunction.relativePositiveMargins() && maxMinRelativeMarginParameters == null) {
                throw new FaraoException("An objective function with relative margins requires parameters on relative margins.");
            }

            return new IteratingLinearOptimizerParameters(
                objectiveFunction,
                rangeActionParameters,
                mnecParameters,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                unoptimizedCnecParameters,
                raLimitationParameters,
                solverParameters,
                maxNumberOfIterations);
        }
    }
}
