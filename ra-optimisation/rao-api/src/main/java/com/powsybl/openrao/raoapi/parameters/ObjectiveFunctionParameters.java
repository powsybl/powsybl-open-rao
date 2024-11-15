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
    private static final ObjectiveFunctionType DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunctionType.MAX_MIN_MARGIN;
    private static final Unit DEFAULT_UNIT = Unit.MEGAWATT;
    private static final double DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT = 0;
    private static final PreventiveStopCriterion DEFAULT_PREVENTIVE_STOP_CRITERION = PreventiveStopCriterion.SECURE;
    private static final boolean DEFAULT_ENFORCE_CURATIVE_SECURITY = false;
    // Attributes
    private ObjectiveFunctionType type = DEFAULT_OBJECTIVE_FUNCTION;
    private double curativeMinObjImprovement = DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT;
    private PreventiveStopCriterion preventiveStopCriterion = DEFAULT_PREVENTIVE_STOP_CRITERION;
    private boolean enforceCurativeSecurity = DEFAULT_ENFORCE_CURATIVE_SECURITY;
    private Unit unit = DEFAULT_UNIT;

    // Enum
    public enum ObjectiveFunctionType {
        MAX_MIN_MARGIN,
        MAX_MIN_RELATIVE_MARGIN;
        public boolean relativePositiveMargins() {
            return this.equals(MAX_MIN_RELATIVE_MARGIN);
        }
    }

    public enum PreventiveStopCriterion {
        MIN_OBJECTIVE,
        SECURE
    }

    // Getters and setters
    public ObjectiveFunctionType getType() {
        return type;
    }

    public void setType(ObjectiveFunctionType type) {
        this.type = type;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
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

    public boolean getEnforceCurativeSecurity() {
        return enforceCurativeSecurity;
    }

    public void setEnforceCurativeSecurity(boolean enforceCurativeSecurity) {
        this.enforceCurativeSecurity = enforceCurativeSecurity;
    }

    public static ObjectiveFunctionParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        ObjectiveFunctionParameters parameters = new ObjectiveFunctionParameters();
        platformConfig.getOptionalModuleConfig(OBJECTIVE_FUNCTION_SECTION)
                .ifPresent(config -> {
                    parameters.setType(config.getEnumProperty(TYPE, ObjectiveFunctionType.class,
                            DEFAULT_OBJECTIVE_FUNCTION));
                    parameters.setUnit(config.getEnumProperty(UNIT, Unit.class, DEFAULT_UNIT));
                    parameters.setCurativeMinObjImprovement(config.getDoubleProperty(CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT));
                    parameters.setPreventiveStopCriterion(config.getEnumProperty(PREVENTIVE_STOP_CRITERION, PreventiveStopCriterion.class,
                            DEFAULT_PREVENTIVE_STOP_CRITERION));
                    parameters.setEnforceCurativeSecurity(config.getBooleanProperty(ENFORCE_CURATIVE_SECURITY, DEFAULT_ENFORCE_CURATIVE_SECURITY));
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
