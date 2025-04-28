/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.LinearOptimizationSolver;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizerParameters {

    private final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
    private final Unit objectiveFunctionUnit;

    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    private final MnecParameters mnecParameters;
    private final SearchTreeRaoMnecParameters mnecParametersExtension;
    private final SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters;
    private final LoopFlowParameters loopFlowParameters;
    private final SearchTreeRaoLoopFlowParameters loopFlowParametersExtension;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final RangeActionLimitationParameters raLimitationParameters;
    private final LinearOptimizationSolver solverParameters;

    private final int maxNumberOfIterations;
    private final boolean raRangeShrinking;

    private IteratingLinearOptimizerParameters(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                               Unit objectiveFunctionUnit,
                                               RangeActionsOptimizationParameters rangeActionParameters,
                                               SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                               MnecParameters mnecParameters,
                                               SearchTreeRaoMnecParameters mnecParametersExtension,
                                               SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters,
                                               LoopFlowParameters loopFlowParameters,
                                               SearchTreeRaoLoopFlowParameters loopFlowParametersExtension,
                                               UnoptimizedCnecParameters unoptimizedCnecParameters,
                                               RangeActionLimitationParameters raLimitationParameters,
                                               LinearOptimizationSolver solverParameters,
                                               int maxNumberOfIterations,
                                               boolean raRangeShrinking) {
        this.objectiveFunction = objectiveFunction;
        this.objectiveFunctionUnit = objectiveFunctionUnit;
        this.rangeActionParameters = rangeActionParameters;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.mnecParameters = mnecParameters;
        this.mnecParametersExtension = mnecParametersExtension;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
        this.loopFlowParametersExtension = loopFlowParametersExtension;
        this.unoptimizedCnecParameters = unoptimizedCnecParameters;
        this.raLimitationParameters = raLimitationParameters;
        this.solverParameters = solverParameters;
        this.maxNumberOfIterations = maxNumberOfIterations;
        this.raRangeShrinking = raRangeShrinking;
    }

    public ObjectiveFunctionParameters.ObjectiveFunctionType getObjectiveFunction() {
        return objectiveFunction;
    }

    public Unit getObjectiveFunctionUnit() {
        return objectiveFunctionUnit;
    }

    public boolean hasRelativeMargins() {
        return getObjectiveFunction().relativePositiveMargins();
    }

    public boolean hasOperatorsNotToOptimize() {
        return unoptimizedCnecParameters != null
            && !unoptimizedCnecParameters.getOperatorsNotToOptimize().isEmpty();
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return loopFlowParameters != null && loopFlowParametersExtension != null;
    }

    public boolean isRaoWithMnecLimitation() {
        return mnecParameters != null && mnecParametersExtension != null;
    }

    public RangeActionsOptimizationParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public SearchTreeRaoRangeActionsOptimizationParameters getRangeActionParametersExtension() {
        return rangeActionParametersExtension;
    }

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public SearchTreeRaoMnecParameters getMnecParametersExtension() {
        return mnecParametersExtension;
    }

    public SearchTreeRaoRelativeMarginsParameters getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParameters getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public SearchTreeRaoLoopFlowParameters getLoopFlowParametersExtension() {
        return loopFlowParametersExtension;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public RangeActionLimitationParameters getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public LinearOptimizationSolver getSolverParameters() {
        return solverParameters;
    }

    public int getMaxNumberOfIterations() {
        return maxNumberOfIterations;
    }

    public boolean getRaRangeShrinking() {
        return raRangeShrinking;
    }

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
    }

    public static class LinearOptimizerParametersBuilder {

        private ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
        private Unit objectiveFunctionUnit;
        private RangeActionsOptimizationParameters rangeActionParameters;
        private SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;

        private MnecParameters mnecParameters;
        private SearchTreeRaoMnecParameters mnecParametersExtension;
        private SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters;
        private LoopFlowParameters loopFlowParameters;
        private SearchTreeRaoLoopFlowParameters loopFlowParametersExtension;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RangeActionLimitationParameters raLimitationParameters;
        private LinearOptimizationSolver solverParameters;
        private int maxNumberOfIterations;
        private boolean raRangeShrinking;

        public LinearOptimizerParametersBuilder withObjectiveFunction(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public LinearOptimizerParametersBuilder withObjectiveFunctionUnit(Unit objectiveFunctionUnit) {
            this.objectiveFunctionUnit = objectiveFunctionUnit;
            return this;
        }

        public LinearOptimizerParametersBuilder withRangeActionParameters(RangeActionsOptimizationParameters rangeActionParameters) {
            this.rangeActionParameters = rangeActionParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withRangeActionParametersExtension(SearchTreeRaoRangeActionsOptimizationParameters rangeActionParameters) {
            this.rangeActionParametersExtension = rangeActionParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecParameters(MnecParameters mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMnecParametersExtension(SearchTreeRaoMnecParameters mnecParametersExtension) {
            this.mnecParametersExtension = mnecParametersExtension;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxMinRelativeMarginParameters(SearchTreeRaoRelativeMarginsParameters maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowParameters(LoopFlowParameters loopFlowParameters) {
            this.loopFlowParameters = loopFlowParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowParametersExtension(SearchTreeRaoLoopFlowParameters loopFlowParametersExtension) {
            this.loopFlowParametersExtension = loopFlowParametersExtension;
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

        public LinearOptimizerParametersBuilder withSolverParameters(LinearOptimizationSolver solverParameters) {
            this.solverParameters = solverParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxNumberOfIterations(int maxNumberOfIterations) {
            this.maxNumberOfIterations = maxNumberOfIterations;
            return this;
        }

        public LinearOptimizerParametersBuilder withRaRangeShrinking(boolean raRangeShrinking) {
            this.raRangeShrinking = raRangeShrinking;
            return this;
        }

        public IteratingLinearOptimizerParameters build() {
            if (objectiveFunction.relativePositiveMargins() && maxMinRelativeMarginParameters == null) {
                throw new OpenRaoException("An objective function with relative margins requires parameters on relative margins.");
            }

            return new IteratingLinearOptimizerParameters(
                objectiveFunction,
                objectiveFunctionUnit,
                rangeActionParameters,
                rangeActionParametersExtension,
                mnecParameters,
                mnecParametersExtension,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                loopFlowParametersExtension,
                unoptimizedCnecParameters,
                raLimitationParameters,
                solverParameters,
                maxNumberOfIterations,
                raRangeShrinking);
        }
    }
}
