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
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.openrao.searchtreerao.commons.parameters.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class IteratingLinearOptimizerParameters {

    private final ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction;
    private final Unit objectiveFunctionUnit;

    private final RangeActionsOptimizationParameters rangeActionParameters;
    private final MnecParametersExtension mnecParameters;
    private final RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
    private final LoopFlowParametersExtension loopFlowParameters;
    private final UnoptimizedCnecParameters unoptimizedCnecParameters;
    private final RangeActionLimitationParameters raLimitationParameters;
    private final RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;

    private final int maxNumberOfIterations;
    private final boolean raRangeShrinking;

    private IteratingLinearOptimizerParameters(ObjectiveFunctionParameters.ObjectiveFunctionType objectiveFunction,
                                               Unit objectiveFunctionUnit,
                                               RangeActionsOptimizationParameters rangeActionParameters,
                                               MnecParametersExtension mnecParameters,
                                               RelativeMarginsParametersExtension maxMinRelativeMarginParameters,
                                               LoopFlowParametersExtension loopFlowParameters,
                                               UnoptimizedCnecParameters unoptimizedCnecParameters,
                                               RangeActionLimitationParameters raLimitationParameters,
                                               RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters,
                                               int maxNumberOfIterations,
                                               boolean raRangeShrinking) {
        this.objectiveFunction = objectiveFunction;
        this.objectiveFunctionUnit = objectiveFunctionUnit;
        this.rangeActionParameters = rangeActionParameters;
        this.mnecParameters = mnecParameters;
        this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
        this.loopFlowParameters = loopFlowParameters;
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
        return loopFlowParameters != null;
    }

    public boolean isRaoWithMnecLimitation() {
        return mnecParameters != null;
    }

    public RangeActionsOptimizationParameters getRangeActionParameters() {
        return rangeActionParameters;
    }

    public MnecParametersExtension getMnecParameters() {
        return mnecParameters;
    }

    public RelativeMarginsParametersExtension getMaxMinRelativeMarginParameters() {
        return maxMinRelativeMarginParameters;
    }

    public LoopFlowParametersExtension getLoopFlowParameters() {
        return loopFlowParameters;
    }

    public UnoptimizedCnecParameters getUnoptimizedCnecParameters() {
        return unoptimizedCnecParameters;
    }

    public RangeActionLimitationParameters getRaLimitationParameters() {
        return raLimitationParameters;
    }

    public RangeActionsOptimizationParameters.LinearOptimizationSolver getSolverParameters() {
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
        private MnecParametersExtension mnecParameters;
        private RelativeMarginsParametersExtension maxMinRelativeMarginParameters;
        private LoopFlowParametersExtension loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RangeActionLimitationParameters raLimitationParameters;
        private RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters;
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

        public LinearOptimizerParametersBuilder withMnecParameters(MnecParametersExtension mnecParameters) {
            this.mnecParameters = mnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxMinRelativeMarginParameters(RelativeMarginsParametersExtension maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withLoopFlowParameters(LoopFlowParametersExtension loopFlowParameters) {
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

        public LinearOptimizerParametersBuilder withSolverParameters(RangeActionsOptimizationParameters.LinearOptimizationSolver solverParameters) {
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
                mnecParameters,
                maxMinRelativeMarginParameters,
                loopFlowParameters,
                unoptimizedCnecParameters,
                raLimitationParameters,
                solverParameters,
                maxNumberOfIterations,
                raRangeShrinking);
        }
    }
}
