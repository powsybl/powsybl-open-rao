/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * Range actions optimization parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 *
 */
public class RangeActionsOptimizationParameters {
    // TODO rename getters setters with intelliji refacto
    // Attributes
    private int maxMipIterations;
    private double pstPenaltyCost;
    private double pstSensitivityThreshold;
    private PstModel pstModel;
    private double hvdcPenaltyCost;
    private double hvdcSensitivityThreshold;
    private double injectionRaPenaltyCost;
    private double injectionRaSensitivityThreshold;
    private LinearOptimizationSolver linearOptimizationSolver;

    // Default values
    private static final int DEFAULT_MAX_MIP_ITERATIONS = 10;
    private static final double DEFAULT_PST_PENALTY_COST = 0.01;
    private static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    private static final PstModel DEFAULT_PST_MODEL = PstModel.CONTINUOUS;
    private static final double DEFAULT_HVDC_PENALTY_COST = 0.001;
    private static final double DEFAULT_HVDC_SENSITIVITY_THRESHOLD = 0.0;
    private static final double DEFAULT_INJECTION_RA_PENALTY_COST = 0.001;
    private static final double DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD = 0.0;

    public RangeActionsOptimizationParameters(int maxMipIterations, double pstPenaltyCost, double pstSensitivityThreshold,
                                              PstModel pstModel, double hvdcPenaltyCost, double hvdcSensitivityThreshold,
                                              double injectionRaPenaltyCost, double injectionRaSensitivityThreshold, LinearOptimizationSolver linearOptimizationSolver) {
        this.maxMipIterations = maxMipIterations;
        this.pstPenaltyCost = pstPenaltyCost;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        this.pstModel = pstModel;
        this.hvdcPenaltyCost = hvdcPenaltyCost;
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
        this.injectionRaPenaltyCost = injectionRaPenaltyCost;
        this.injectionRaSensitivityThreshold = injectionRaSensitivityThreshold;
        this.linearOptimizationSolver = linearOptimizationSolver;
    }

    public static RangeActionsOptimizationParameters loadDefault() {
        return new RangeActionsOptimizationParameters(DEFAULT_MAX_MIP_ITERATIONS, DEFAULT_PST_PENALTY_COST,
                DEFAULT_PST_SENSITIVITY_THRESHOLD, DEFAULT_PST_MODEL,
                DEFAULT_HVDC_PENALTY_COST, DEFAULT_HVDC_SENSITIVITY_THRESHOLD,
                DEFAULT_INJECTION_RA_PENALTY_COST, DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD,
                LinearOptimizationSolver.loadDefault());
    }

    // Enum
    public enum PstModel {
        CONTINUOUS,
        APPROXIMATED_INTEGERS
    }

    public static class LinearOptimizationSolver {
        private Solver solver;
        private double relativeMipGap;
        private String solverSpecificParameters;

        private static final Solver DEFAULT_SOLVER = Solver.CBC;
        public static final double DEFAULT_RELATIVE_MIP_GAP = 0.0001;
        public static final String DEFAULT_SOLVER_SPECIFIC_PARAMETERS = null;

        public LinearOptimizationSolver(Solver solver, double relativeMipGap, String solverSpecificParameters) {
            this.solver = solver;
            this.relativeMipGap = relativeMipGap;
            this.solverSpecificParameters = solverSpecificParameters;
        }

        public static LinearOptimizationSolver loadDefault() {
            return new LinearOptimizationSolver(DEFAULT_SOLVER, DEFAULT_RELATIVE_MIP_GAP, DEFAULT_SOLVER_SPECIFIC_PARAMETERS);
        }

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
            LinearOptimizationSolver parameters = loadDefault();
            platformConfig.getOptionalModuleConfig(LINEAR_OPTIMIZATION_SOLVER)
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

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public void setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = pstPenaltyCost;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public void setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
    }

    public double getHvdcPenaltyCost() {
        return hvdcPenaltyCost;
    }

    public void setHvdcPenaltyCost(double hvdcPenaltyCost) {
        this.hvdcPenaltyCost = hvdcPenaltyCost;
    }

    public double getHvdcSensitivityThreshold() {
        return hvdcSensitivityThreshold;
    }

    public void setHvdcSensitivityThreshold(double hvdcSensitivityThreshold) {
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
    }

    public double getInjectionRaPenaltyCost() {
        return injectionRaPenaltyCost;
    }

    public void setInjectionRaPenaltyCost(double injectionRaPenaltyCost) {
        this.injectionRaPenaltyCost = injectionRaPenaltyCost;
    }

    public double getInjectionRaSensitivityThreshold() {
        return injectionRaSensitivityThreshold;
    }

    public void setInjectionRaSensitivityThreshold(double injectionRaSensitivityThreshold) {
        this.injectionRaSensitivityThreshold = injectionRaSensitivityThreshold;
    }

    public LinearOptimizationSolver getLinearOptimizationSolver() {
        return linearOptimizationSolver;
    }

    public void setLinearOptimizationSolverSolver(Solver solver) {
        this.linearOptimizationSolver.setSolver(solver);
    }

    public void setLinearOptimizationSolverRelativeMipGap(double relativeMipGap) {
        this.linearOptimizationSolver.setRelativeMipGap(relativeMipGap);
    }

    public void setLinearOptimizationSolverSpecificParameters(String solverSpecificParameters) {
        this.linearOptimizationSolver.setSolverSpecificParameters(solverSpecificParameters);
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

    public static RangeActionsOptimizationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        RangeActionsOptimizationParameters parameters = loadDefault();
        platformConfig.getOptionalModuleConfig("range-actions-optimization")
                .ifPresent(config -> {
                    parameters.setMaxMipIterations(config.getIntProperty("max-mip-iterations", DEFAULT_MAX_MIP_ITERATIONS));
                    parameters.setPstPenaltyCost(config.getDoubleProperty("pst-penalty-cost", DEFAULT_PST_PENALTY_COST));
                    parameters.setPstSensitivityThreshold(config.getDoubleProperty("pst-sensitivity-threshold", DEFAULT_PST_SENSITIVITY_THRESHOLD));
                    parameters.setPstModel(config.getEnumProperty("pst-model", PstModel.class, DEFAULT_PST_MODEL));
                    parameters.setHvdcPenaltyCost(config.getDoubleProperty("hvdc-penalty-cost", DEFAULT_HVDC_PENALTY_COST));
                    parameters.setHvdcSensitivityThreshold(config.getDoubleProperty("hvdc-sensitivity-threshold", DEFAULT_HVDC_SENSITIVITY_THRESHOLD));
                    parameters.setInjectionRaPenaltyCost(config.getDoubleProperty("injection-ra-penalty-cost", DEFAULT_INJECTION_RA_PENALTY_COST));
                    parameters.setInjectionRaSensitivityThreshold(config.getDoubleProperty("injection-ra-sensitivity-threshold", DEFAULT_INJECTION_RA_SENSITIVITY_THRESHOLD));
                });
        parameters.setLinearOptimizationSolver(LinearOptimizationSolver.load(platformConfig));
        return parameters;
    }
}
