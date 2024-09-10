/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * Objective function parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class ObjectiveFunctionParameters {
    // Default values
    private static final ObjectiveFunctionType DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT;
    private static final boolean DEFAULT_FORBID_COST_INCREASE = false;
    private static final double DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT = 0;
    private static final PreventiveStopCriterion DEFAULT_PREVENTIVE_STOP_CRITERION = PreventiveStopCriterion.SECURE;
    private static final CurativeStopCriterion DEFAULT_CURATIVE_STOP_CRITERION = CurativeStopCriterion.MIN_OBJECTIVE;
    private static final boolean DEFAULT_OPTIMIZE_CURATIVE_IF_PREVENTIVE_UNSECURE = false;
    // Attributes
    private ObjectiveFunctionType type = DEFAULT_OBJECTIVE_FUNCTION;
    private boolean forbidCostIncrease = DEFAULT_FORBID_COST_INCREASE;
    private double curativeMinObjImprovement = DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT;
    private PreventiveStopCriterion preventiveStopCriterion = DEFAULT_PREVENTIVE_STOP_CRITERION;
    private CurativeStopCriterion curativeStopCriterion = DEFAULT_CURATIVE_STOP_CRITERION;
    private boolean optimizeCurativeIfPreventiveUnsecure = DEFAULT_OPTIMIZE_CURATIVE_IF_PREVENTIVE_UNSECURE;

    // Enum
    public enum ObjectiveFunctionType {
        MAX_MIN_MARGIN_IN_MEGAWATT(Unit.MEGAWATT),
        MAX_MIN_MARGIN_IN_AMPERE(Unit.AMPERE),
        MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT(Unit.MEGAWATT),
        MAX_MIN_RELATIVE_MARGIN_IN_AMPERE(Unit.AMPERE),
        // Unit is used for flow values, not for the cost
        MIN_COST_MEGAWATT(Unit.MEGAWATT),
        MIN_COST_AMPERE(Unit.AMPERE);

        private final Unit unit;

        ObjectiveFunctionType(Unit unit) {
            this.unit = unit;
        }

        public Unit getUnit() {
            return unit;
        }

        public boolean relativePositiveMargins() {
            return this.equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT) || this.equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        }

        public boolean isMinCost() {
            return this.equals(MIN_COST_MEGAWATT) || this.equals(MIN_COST_AMPERE);
        }
    }

    public enum PreventiveStopCriterion {
        MIN_OBJECTIVE,
        SECURE
    }

    public enum CurativeStopCriterion {
        MIN_OBJECTIVE, // only stop after minimizing objective
        SECURE, //stop when objective is strictly negative
        PREVENTIVE_OBJECTIVE, // stop when preventive objective is reached, or bested by curativeRaoMinObjImprovement
        PREVENTIVE_OBJECTIVE_AND_SECURE // stop when preventive objective is reached or bested by curativeRaoMinObjImprovement, and the situation is secure
    }

    // Getters and setters
    public ObjectiveFunctionType getType() {
        return type;
    }

    public void setType(ObjectiveFunctionType type) {
        this.type = type;
    }

    public boolean getForbidCostIncrease() {
        return forbidCostIncrease;
    }

    public void setForbidCostIncrease(boolean forbidCostIncrease) {
        this.forbidCostIncrease = forbidCostIncrease;
    }

    public void setPreventiveStopCriterion(PreventiveStopCriterion preventiveStopCriterion) {
        this.preventiveStopCriterion = preventiveStopCriterion;
    }

    public double getCurativeMinObjImprovement() {
        return curativeMinObjImprovement;
    }

    public PreventiveStopCriterion getPreventiveStopCriterion() {
        return preventiveStopCriterion;
    }

    public CurativeStopCriterion getCurativeStopCriterion() {
        return curativeStopCriterion;
    }

    public void setCurativeStopCriterion(CurativeStopCriterion curativeStopCriterion) {
        this.curativeStopCriterion = curativeStopCriterion;
    }

    public boolean getOptimizeCurativeIfPreventiveUnsecure() {
        return optimizeCurativeIfPreventiveUnsecure;
    }

    public void setOptimizeCurativeIfPreventiveUnsecure(boolean optimizeCurativeIfPreventiveUnsecure) {
        this.optimizeCurativeIfPreventiveUnsecure = optimizeCurativeIfPreventiveUnsecure;
    }

    public static ObjectiveFunctionParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        ObjectiveFunctionParameters parameters = new ObjectiveFunctionParameters();
        platformConfig.getOptionalModuleConfig(OBJECTIVE_FUNCTION_SECTION)
                .ifPresent(config -> {
                    parameters.setType(config.getEnumProperty(TYPE, ObjectiveFunctionType.class,
                            DEFAULT_OBJECTIVE_FUNCTION));
                    parameters.setForbidCostIncrease(config.getBooleanProperty(FORBID_COST_INCREASE, DEFAULT_FORBID_COST_INCREASE));
                    parameters.setCurativeMinObjImprovement(config.getDoubleProperty(CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT));
                    parameters.setPreventiveStopCriterion(config.getEnumProperty(PREVENTIVE_STOP_CRITERION, PreventiveStopCriterion.class,
                            DEFAULT_PREVENTIVE_STOP_CRITERION));
                    parameters.setCurativeStopCriterion(config.getEnumProperty(CURATIVE_STOP_CRITERION, CurativeStopCriterion.class,
                            DEFAULT_CURATIVE_STOP_CRITERION));
                    parameters.setOptimizeCurativeIfPreventiveUnsecure(config.getBooleanProperty(OPTIMIZE_CURATIVE_IF_PREVENTIVE_UNSECURE, DEFAULT_OPTIMIZE_CURATIVE_IF_PREVENTIVE_UNSECURE));
                });
        return parameters;
    }

    public void setCurativeMinObjImprovement(double curativeRaoMinObjImprovement) {
        if (curativeRaoMinObjImprovement < 0) {
            BUSINESS_WARNS.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
        }
        this.curativeMinObjImprovement = Math.abs(curativeRaoMinObjImprovement);
    }
}
