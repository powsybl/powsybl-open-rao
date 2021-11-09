/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class LinearOptimizerParameters {
    private RaoParameters.ObjectiveFunction objectiveFunction;
    private double pstSensitivityThreshold;
    private double hvdcSensitivityThreshold;
    private MnecParameters mnecParameters;
    private MaxMinMarginParameters maxMinMarginParameters;
    private MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
    private LoopFlowParameters loopFlowParameters;
    private UnoptimizedCnecParameters unoptimizedCnecParameters;
    private RaoParameters.Solver solver;
    private double relativeMipGap;
    private String solverSpecificParameters;
    private RaoParameters.PstOptimizationApproximation pstOptimizationApproximation;

    private LinearOptimizerParameters() {
        // Can be instantiated only by builder
    }

    public static LinearOptimizerParametersBuilder create() {
        return new LinearOptimizerParametersBuilder();
    }

    public static class LinearOptimizerParametersBuilder {
        private RaoParameters.ObjectiveFunction objectiveFunction;
        private Double pstSensitivityThreshold;
        private Double hvdcSensitivityThreshold;
        private MaxMinMarginParameters maxMinMarginParameters;
        private MaxMinRelativeMarginParameters maxMinRelativeMarginParameters;
        private MnecParameters mnecParameters;
        private LoopFlowParameters loopFlowParameters;
        private UnoptimizedCnecParameters unoptimizedCnecParameters;
        private RaoParameters.Solver solver;
        private double relativeMipGap;
        private String solverSpecificParameters;
        private RaoParameters.PstOptimizationApproximation pstOptimizationApproximation;

        public LinearOptimizerParametersBuilder withObjectiveFunction(RaoParameters.ObjectiveFunction objectiveFunction) {
            this.objectiveFunction = objectiveFunction;
            return this;
        }

        public LinearOptimizerParametersBuilder withPstSensitivityThreshold(double pstSensitivityThreshold) {
            this.pstSensitivityThreshold = pstSensitivityThreshold;
            return this;
        }

        public LinearOptimizerParametersBuilder withHvdcSensitivityThreshold(double hvdcSensitivityThreshold) {
            this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxMinMarginParameters(MaxMinMarginParameters maxMinMarginParameters) {
            this.maxMinMarginParameters = maxMinMarginParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withMaxMinRelativeMarginParameters(MaxMinRelativeMarginParameters maxMinRelativeMarginParameters) {
            this.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
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

        public LinearOptimizerParametersBuilder withUnoptimizedCnecParameters(UnoptimizedCnecParameters unoptimizedCnecParameters) {
            this.unoptimizedCnecParameters = unoptimizedCnecParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withSolver(RaoParameters.Solver solver) {
            this.solver = solver;
            return this;
        }

        public LinearOptimizerParametersBuilder withRelativeMipGap(double relativeMipGap) {
            this.relativeMipGap = relativeMipGap;
            return this;
        }

        public LinearOptimizerParametersBuilder withSolverSpecificParameters(String solverSpecificParameters) {
            this.solverSpecificParameters = solverSpecificParameters;
            return this;
        }

        public LinearOptimizerParametersBuilder withPstOptimizationApproximation(RaoParameters.PstOptimizationApproximation pstOptimizationApproximation) {
            this.pstOptimizationApproximation = pstOptimizationApproximation;
            return this;
        }

        public LinearOptimizerParameters build() {
            if (objectiveFunction == null || pstSensitivityThreshold == null || hvdcSensitivityThreshold == null) {
                throw new FaraoException("Objective function, pst sensitivity threshold and hvdc sensitivity threshold are mandatory parameters.");
            }
            if (objectiveFunction.relativePositiveMargins() && maxMinRelativeMarginParameters == null) {
                throw new FaraoException("An objective function with relative margins requires parameters on relative margins.");
            }
            if (!objectiveFunction.relativePositiveMargins() && maxMinMarginParameters == null) {
                throw new FaraoException("An objective function without relative margins requires parameters on non-relative margins.");
            }
            LinearOptimizerParameters linearOptimizerParameters = new LinearOptimizerParameters();
            linearOptimizerParameters.objectiveFunction = objectiveFunction;
            linearOptimizerParameters.pstSensitivityThreshold = pstSensitivityThreshold;
            linearOptimizerParameters.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
            linearOptimizerParameters.maxMinMarginParameters = maxMinMarginParameters;
            linearOptimizerParameters.maxMinRelativeMarginParameters = maxMinRelativeMarginParameters;
            linearOptimizerParameters.mnecParameters = mnecParameters;
            linearOptimizerParameters.loopFlowParameters = loopFlowParameters;
            linearOptimizerParameters.unoptimizedCnecParameters = unoptimizedCnecParameters;
            linearOptimizerParameters.solver = solver;
            linearOptimizerParameters.relativeMipGap = relativeMipGap;
            linearOptimizerParameters.solverSpecificParameters = solverSpecificParameters;
            linearOptimizerParameters.pstOptimizationApproximation = pstOptimizationApproximation;
            return linearOptimizerParameters;
        }
    }

    public RaoParameters.ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public double getHvdcSensitivityThreshold() {
        return hvdcSensitivityThreshold;
    }

    public Unit getUnit() {
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

    public MnecParameters getMnecParameters() {
        return mnecParameters;
    }

    public MaxMinMarginParameters getMaxMinMarginParameters() {
        return maxMinMarginParameters;
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

    public RaoParameters.Solver getSolver() {
        return solver;
    }

    public double getRelativeMipGap() {
        return relativeMipGap;
    }

    public String getSolverSpecificParameters() {
        return solverSpecificParameters;
    }

    public RaoParameters.PstOptimizationApproximation getPstOptimizationApproximation() {
        return pstOptimizationApproximation;
    }
}
