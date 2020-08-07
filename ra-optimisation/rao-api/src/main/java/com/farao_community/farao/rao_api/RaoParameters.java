/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api;

import com.farao_community.farao.commons.Unit;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.sensitivity.SensitivityComputationParameters;

import java.util.Objects;

import static java.lang.Math.max;

/**
 * Parameters for rao
 * Extensions may be added, for instance for implementation-specific parameters.
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class RaoParameters extends AbstractExtendable<RaoParameters> {

    public enum ObjectiveFunction {
        MAX_MIN_MARGIN_IN_MEGAWATT(Unit.MEGAWATT),
        MAX_MIN_MARGIN_IN_AMPERE(Unit.AMPERE);

        private Unit unit;

        ObjectiveFunction(Unit unit) {
            this.unit = unit;
        }

        public Unit getUnit() {
            return unit;
        }
    }

    public static final ObjectiveFunction DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunction.MAX_MIN_MARGIN_IN_MEGAWATT;
    public static final int DEFAULT_MAX_ITERATIONS = 10;
    public static final double DEFAULT_FALLBACK_OVER_COST = 0;
    public static final boolean DEFAULT_RAO_WITH_LOOP_FLOW_LIMITATION = false; //loop flow is for CORE D2CC, default value set to false
    public static final boolean DEFAULT_SECURITY_ANALYSIS_WITHOUT_RAO = false;
    public static final double DEFAULT_PST_SENSITIVITY_THRESHOLD = 0.0;
    public static final boolean DEFAULT_LOOP_FLOW_APPROXIMATION = true;
    public static final double DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;
    public static final double DEFAULT_LOOP_FLOW_VIOLATION_COST = 0.0;
    public static final double DEFAULT_PST_PENALTY_COST = 0.01;
    private static final double DEFAULT_MNEC_ACCEPTABLE_MARGIN_DIMINUTION = 50.0;
    private static final double DEFAULT_MNEC_VIOLATION_COST = 10.0;
    private static final double DEFAULT_MNEC_CONSTRAINT_ADJUSTMENT_COEFFICIENT = 0.0;

    private ObjectiveFunction objectiveFunction = DEFAULT_OBJECTIVE_FUNCTION;
    private int maxIterations = DEFAULT_MAX_ITERATIONS;
    private double pstPenaltyCost = DEFAULT_PST_PENALTY_COST;
    private double pstSensitivityThreshold = DEFAULT_PST_SENSITIVITY_THRESHOLD;
    private double fallbackOverCost = DEFAULT_FALLBACK_OVER_COST;
    private boolean raoWithLoopFlowLimitation = DEFAULT_RAO_WITH_LOOP_FLOW_LIMITATION;
    private boolean loopFlowApproximation = DEFAULT_LOOP_FLOW_APPROXIMATION;
    private double loopFlowConstraintAdjustmentCoefficient = DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT;
    private double loopFlowViolationCost = DEFAULT_LOOP_FLOW_VIOLATION_COST;
    private double mnecAcceptableMarginDiminution = DEFAULT_MNEC_ACCEPTABLE_MARGIN_DIMINUTION; // always in MW
    private double mnecViolationCost = DEFAULT_MNEC_VIOLATION_COST; // "A equivalent cost per A violation" or "MW per MW", depending on the objective function
    private double mnecConstraintAdjustmentCoefficient = DEFAULT_MNEC_CONSTRAINT_ADJUSTMENT_COEFFICIENT; // always in MW
    private SensitivityComputationParameters defaultSensitivityComputationParameters = new SensitivityComputationParameters();
    private SensitivityComputationParameters fallbackSensitivityComputationParameters; // Must be null by default

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public RaoParameters setObjectiveFunction(ObjectiveFunction objectiveFunction) {
        this.objectiveFunction = objectiveFunction;
        return this;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public RaoParameters setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public RaoParameters setPstPenaltyCost(double pstPenaltyCost) {
        this.pstPenaltyCost = max(0.0, pstPenaltyCost);
        return this;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public RaoParameters setPstSensitivityThreshold(double pstSensitivityThreshold) {
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        return this;
    }

    public double getFallbackOverCost() {
        return fallbackOverCost;
    }

    public RaoParameters setFallbackOverCost(double overCost) {
        this.fallbackOverCost = max(0.0, overCost);
        return this;
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return raoWithLoopFlowLimitation;
    }

    public RaoParameters setRaoWithLoopFlowLimitation(boolean raoWithLoopFlowLimitation) {
        this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
        return this;
    }

    public boolean isLoopFlowApproximation() {
        return loopFlowApproximation;
    }

    public RaoParameters setLoopFlowApproximation(boolean loopFlowApproximation) {
        this.loopFlowApproximation = loopFlowApproximation;
        return this;
    }

    public double getLoopFlowConstraintAdjustmentCoefficient() {
        return loopFlowConstraintAdjustmentCoefficient;
    }

    public RaoParameters setLoopFlowConstraintAdjustmentCoefficient(double loopFlowConstraintAdjustmentCoefficient) {
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
        return this;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public void setLoopFlowViolationCost(double loopflowViolationCost) {
        this.loopFlowViolationCost = loopflowViolationCost;
    }

    public SensitivityComputationParameters getDefaultSensitivityComputationParameters() {
        return defaultSensitivityComputationParameters;
    }

    public RaoParameters setDefaultSensitivityComputationParameters(SensitivityComputationParameters sensiParameters) {
        this.defaultSensitivityComputationParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }

    public SensitivityComputationParameters getFallbackSensitivityComputationParameters() {
        return fallbackSensitivityComputationParameters;
    }

    public RaoParameters setFallbackSensitivityComputationParameters(SensitivityComputationParameters sensiParameters) {
        this.fallbackSensitivityComputationParameters = Objects.requireNonNull(sensiParameters);
        return this;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public void setMnecAcceptableMarginDiminution(double mnecAcceptableMarginDiminution) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }

    public void setMnecConstraintAdjustmentCoefficient(double mnecConstraintAdjustmentCoefficient) {
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public void setMnecViolationCost(double mnecViolationCost) {
        this.mnecViolationCost = mnecViolationCost;
    }

    /**
     * A configuration loader interface for the RaoParameters extensions loaded from the platform configuration
     * @param <E> The extension class
     */
    public static interface ConfigLoader<E extends Extension<RaoParameters>> extends ExtensionConfigLoader<RaoParameters, E> {
    }

    public static final String VERSION = "1.0";

    private static final Supplier<ExtensionProviders<ConfigLoader>> PARAMETERS_EXTENSIONS_SUPPLIER =
        Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "rao-parameters"));

    /**
     * @return RaoParameters from platform default config.
     */
    public static RaoParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * @param platformConfig PlatformConfig where the RaoParameters should be read from
     * @return RaoParameters from the provided platform config
     */
    public static RaoParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        RaoParameters parameters = new RaoParameters();
        load(parameters, platformConfig);
        parameters.readExtensions(platformConfig);

        return parameters;
    }

    protected static void load(RaoParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);

        platformConfig.getOptionalModuleConfig("rao-parameters")
            .ifPresent(config -> {
                parameters.setObjectiveFunction(config.getEnumProperty("objective-function", ObjectiveFunction.class, DEFAULT_OBJECTIVE_FUNCTION));
                parameters.setMaxIterations(config.getIntProperty("max-number-of-iterations", DEFAULT_MAX_ITERATIONS));
                parameters.setPstPenaltyCost(config.getDoubleProperty("pst-penalty-cost", DEFAULT_PST_PENALTY_COST));
                parameters.setPstSensitivityThreshold(config.getDoubleProperty("pst-sensitivity-threshold", DEFAULT_PST_SENSITIVITY_THRESHOLD));
                parameters.setFallbackOverCost(config.getDoubleProperty("sensitivity-fallback-over-cost", DEFAULT_FALLBACK_OVER_COST));
                parameters.setRaoWithLoopFlowLimitation(config.getBooleanProperty("rao-with-loop-flow-limitation", DEFAULT_RAO_WITH_LOOP_FLOW_LIMITATION));
                parameters.setLoopFlowApproximation(config.getBooleanProperty("loop-flow-approximation", DEFAULT_LOOP_FLOW_APPROXIMATION));
                parameters.setLoopFlowConstraintAdjustmentCoefficient(config.getDoubleProperty("loop-flow-constraint-adjustment-coefficient", DEFAULT_LOOP_FLOW_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
                parameters.setLoopFlowViolationCost(config.getDoubleProperty("loop-flow-violation-cost", DEFAULT_LOOP_FLOW_VIOLATION_COST));
                parameters.setMnecAcceptableMarginDiminution(config.getDoubleProperty("mnec-acceptable-margin-diminution", DEFAULT_MNEC_ACCEPTABLE_MARGIN_DIMINUTION));
                parameters.setMnecViolationCost(config.getDoubleProperty("mnec-violation-cost", DEFAULT_MNEC_VIOLATION_COST));
                parameters.setMnecConstraintAdjustmentCoefficient(config.getDoubleProperty("mnec-constraint-adjustment-coefficient", DEFAULT_MNEC_CONSTRAINT_ADJUSTMENT_COEFFICIENT));
            });

        // NB: Only the default sensitivity parameters are loaded, not the fallback ones...
        parameters.setDefaultSensitivityComputationParameters(SensitivityComputationParameters.load(platformConfig));
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : PARAMETERS_EXTENSIONS_SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }
}
