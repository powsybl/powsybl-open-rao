/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Range actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 *
 */
public class RangeActionsOptimizationParameters {

    // Default values
    private static final int DEFAULT_MAX_MIP_ITERATIONS = 10;
    private static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 1e-6;
    private static final PstModel DEFAULT_PST_MODEL = PstModel.CONTINUOUS;
    private static final double DEFAULT_HVDC_SENSITIVITY_THRESHOLD = 1e-6;
    private static final double DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD = 1e-6;
    private static final RaRangeShrinking DEFAULT_RA_RANGE_SHRINKING = RaRangeShrinking.DISABLED;
    // Attributes
    private int maxMipIterations = DEFAULT_MAX_MIP_ITERATIONS;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    private PstModel pstModel = DEFAULT_PST_MODEL;
    private double hvdcSensitivityThreshold = DEFAULT_HVDC_SENSITIVITY_THRESHOLD;
    private double injectionRaSensitivityThreshold = DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD;
    private LinearOptimizationSolver linearOptimizationSolver = new LinearOptimizationSolver();
    private RaRangeShrinking raRangeShrinking = DEFAULT_RA_RANGE_SHRINKING;

    public enum PstModel {
        CONTINUOUS,
        APPROXIMATED_INTEGERS
    }

    public enum RaRangeShrinking {
        DISABLED,
        ENABLED,
        ENABLED_IN_FIRST_PRAO_AND_CRAO
    }

    public static class LinearOptimizationSolver {
        private static final Solver DEFAULT_SOLVER = Solver.CBC;
        public static final double DEFAULT_RELATIVE_MIP_GAP = 0.0001;
        public static final String DEFAULT_SOLVER_SPECIFIC_PARAMETERS = null;
        private Solver solver = DEFAULT_SOLVER;
        private double relativeMipGap = DEFAULT_RELATIVE_MIP_GAP;
        private String solverSpecificParameters = DEFAULT_SOLVER_SPECIFIC_PARAMETERS;

        public Solver getSolver() {
            return solver;
        }

        public void setSolver(Solver solver) {
            this.solver = solver;
        }

        public double getRelativeMipGap() {
            return relativeMipGap;
        }

        public void setRelativeMipGap(double relativeMipGap) {
            this.relativeMipGap = relativeMipGap;
        }

        public String getSolverSpecificParameters() {
            return solverSpecificParameters;
        }

        public void setSolverSpecificParameters(String solverSpecificParameters) {
            this.solverSpecificParameters = solverSpecificParameters;
        }

        public static LinearOptimizationSolver load(PlatformConfig platformConfig) {
            Objects.requireNonNull(platformConfig);
            LinearOptimizationSolver parameters = new LinearOptimizationSolver();
            platformConfig.getOptionalModuleConfig(LINEAR_OPTIMIZATION_SOLVER_SECTION)
                .ifPresent(config -> {
                    parameters.setSolver(config.getEnumProperty(SOLVER, Solver.class, DEFAULT_SOLVER));
                    parameters.setRelativeMipGap(config.getDoubleProperty(RELATIVE_MIP_GAP, DEFAULT_RELATIVE_MIP_GAP));
                    parameters.setSolverSpecificParameters(config.getStringProperty(SOLVER_SPECIFIC_PARAMETERS, DEFAULT_SOLVER_SPECIFIC_PARAMETERS));
                });
            return parameters;
        }
    }

    public enum Solver {
        CBC,
        SCIP,
        XPRESS
    }

    // Getters and setters
    public int getMaxMipIterations() {
        return maxMipIterations;
    }

    public void setMaxMipIterations(int maxMipIterations) {
        this.maxMipIterations = maxMipIterations;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        if (pstSensitivityThreshold < 1e-6) {
            throw new OpenRaoException("pstSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.");
        }
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }

    public double getHvdcSensitivityThreshold() {
        return hvdcSensitivityThreshold;
    }

    public void setHvdcSensitivityThreshold(double hvdcSensitivityThreshold) {
        if (hvdcSensitivityThreshold < 1e-6) {
            throw new OpenRaoException("hvdcSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.");
        }
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
    }

    public double getInjectionRaSensitivityThreshold() {
        return injectionRaSensitivityThreshold;
    }

    public void setInjectionRaSensitivityThreshold(double injectionRaSensitivityThreshold) {
        if (injectionRaSensitivityThreshold < 1e-6) {
            throw new OpenRaoException("injectionRaSensitivityThreshold should be greater than 1e-6, to avoid numerical issues.");
        }
        this.injectionRaSensitivityThreshold = injectionRaSensitivityThreshold;
    }

    public LinearOptimizationSolver getLinearOptimizationSolver() {
        return linearOptimizationSolver;
    }

    public void setPstModel(PstModel pstModel) {
        this.pstModel = pstModel;
    }

    public PstModel getPstModel() {
        return pstModel;
    }

    public void setLinearOptimizationSolver(LinearOptimizationSolver linearOptimizationSolver) {
        this.linearOptimizationSolver = linearOptimizationSolver;
    }

    public void setRaRangeShrinking(RaRangeShrinking raRangeShrinking) {
        this.raRangeShrinking = raRangeShrinking;
    }

    public RaRangeShrinking getRaRangeShrinking() {
        return raRangeShrinking;
    }

    public static RangeActionsOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RangeActionsOptimizationParameters parameters = new RangeActionsOptimizationParameters();
        platformConfig.getOptionalModuleConfig(ST_RANGE_ACTIONS_OPTIMIZATION_SECTION)
            .ifPresent(config -> {
                parameters.setMaxMipIterations(config.getIntProperty(MAX_MIP_ITERATIONS, DEFAULT_MAX_MIP_ITERATIONS));
                parameters.setPstSensitivityThreshold(config.getDoubleProperty(PST_SENSITIVITY_THRESHOLD, DEFAULT_PST_SENSITIVITY_THRESHOLD));
                parameters.setPstModel(config.getEnumProperty(PST_MODEL, PstModel.class, DEFAULT_PST_MODEL));
                parameters.setHvdcSensitivityThreshold(config.getDoubleProperty(HVDC_SENSITIVITY_THRESHOLD, DEFAULT_HVDC_SENSITIVITY_THRESHOLD));
                parameters.setInjectionRaSensitivityThreshold(config.getDoubleProperty(INJECTION_RA_SENSITIVITY_THRESHOLD, DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD));
                parameters.setRaRangeShrinking(config.getEnumProperty(RA_RANGE_SHRINKING, RaRangeShrinking.class, DEFAULT_RA_RANGE_SHRINKING));
            });
        parameters.setLinearOptimizationSolver(LinearOptimizationSolver.load(platformConfig));
        return parameters;
    }

    public static PstModel getPstModel(RaoParameters raoParameters) {
        if (raoParameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return raoParameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getPstModel();
        }
        return DEFAULT_PST_MODEL;
    }

    public static RaRangeShrinking getRaRangeShrinking(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getRaRangeShrinking();
        }
        return DEFAULT_RA_RANGE_SHRINKING;
    }

    // TODO: do not set if default...
    public static LinearOptimizationSolver getLinearOptimizationSolver(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getLinearOptimizationSolver();
        }
        return new LinearOptimizationSolver();
    }

    public static int getMaxMipIterations(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getRangeActionsOptimizationParameters().getMaxMipIterations();
        }
        return DEFAULT_MAX_MIP_ITERATIONS;
    }

    public static PstModel getPstModel(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        if (!Objects.isNull(rangeActionsOptimizationParameters)) {
            return rangeActionsOptimizationParameters.getPstModel();
        }
        return DEFAULT_PST_MODEL;
    }

    public static double getPstSensitivityThreshold(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        if (!Objects.isNull(rangeActionsOptimizationParameters)) {
            return rangeActionsOptimizationParameters.getPstSensitivityThreshold();
        }
        return DEFAULT_PST_SENSITIVITY_THRESHOLD;
    }

    public static double getHvdcSensitivityThreshold(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        if (!Objects.isNull(rangeActionsOptimizationParameters)) {
            return rangeActionsOptimizationParameters.getHvdcSensitivityThreshold();
        }
        return DEFAULT_HVDC_SENSITIVITY_THRESHOLD;
    }

    public static double getInjectionRaSensitivityThreshold(RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        if (!Objects.isNull(rangeActionsOptimizationParameters)) {
            return rangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold();
        }
        return DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD;
    }
}
