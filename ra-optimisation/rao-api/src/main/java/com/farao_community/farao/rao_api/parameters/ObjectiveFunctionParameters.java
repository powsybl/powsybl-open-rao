/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.Unit;
import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Objective function parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
// TODO : rapatrier commentaires
public class ObjectiveFunctionParameters {
    // Attributes
    private ObjectiveFunctionType objectiveFunctionType;
    // Fallback to initial solution if RAO caused cost to increase (ie in curative)
    private boolean forbidCostIncrease;
    private double curativeMinObjImprovement;
    private PreventiveStopCriterion preventiveStopCriterion;
    private CurativeStopCriterion curativeStopCriterion;

    // Default values
    static final ObjectiveFunctionType DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunctionType.MAX_MIN_MARGIN_IN_MEGAWATT;
    static final boolean DEFAULT_FORBID_COST_INCREASE = false;
    static final double DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT = 0;
    static final PreventiveStopCriterion DEFAULT_PREVENTIVE_STOP_CRITERION = PreventiveStopCriterion.SECURE;
    static final CurativeStopCriterion DEFAULT_CURATIVE_STOP_CRITERION = CurativeStopCriterion.MIN_OBJECTIVE;

    public ObjectiveFunctionParameters(ObjectiveFunctionType objectiveFunctionType, boolean forbidCostIncrease, double curativeMinObjImprovement, PreventiveStopCriterion preventiveStopCriterion, CurativeStopCriterion curativeStopCriterion) {
        this.objectiveFunctionType = objectiveFunctionType;
        this.forbidCostIncrease = forbidCostIncrease;
        this.curativeMinObjImprovement = curativeMinObjImprovement;
        this.preventiveStopCriterion = preventiveStopCriterion;
        this.curativeStopCriterion = curativeStopCriterion;
    }

    public static ObjectiveFunctionParameters loadDefault() {
        return new ObjectiveFunctionParameters(DEFAULT_OBJECTIVE_FUNCTION, DEFAULT_FORBID_COST_INCREASE,
                DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_PREVENTIVE_STOP_CRITERION, DEFAULT_CURATIVE_STOP_CRITERION);
    }

    // Enum
    public enum ObjectiveFunctionType {
        MAX_MIN_MARGIN_IN_MEGAWATT(Unit.MEGAWATT, false),
        MAX_MIN_MARGIN_IN_AMPERE(Unit.AMPERE, false),
        MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT(Unit.MEGAWATT, true),
        MAX_MIN_RELATIVE_MARGIN_IN_AMPERE(Unit.AMPERE, true);

        private Unit unit;
        private boolean requirePtdf;

        ObjectiveFunctionType(Unit unit, boolean requirePtdf) {
            this.unit = unit;
            this.requirePtdf = requirePtdf;
        }

        public Unit getUnit() {
            return unit;
        }

        // TODO : doesRequirePtdf = relativePositiveMargins => simplify
        public boolean doesRequirePtdf() {
            return requirePtdf;
        }

        public boolean relativePositiveMargins() {
            return this.equals(MAX_MIN_RELATIVE_MARGIN_IN_MEGAWATT) || this.equals(MAX_MIN_RELATIVE_MARGIN_IN_AMPERE);
        }
        // TODO : add check to check that getObjectiveFunctionType is consisten with the presence of RelativeMarginsParameters extension
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

    public ObjectiveFunctionType getObjectiveFunctionType() {
        return objectiveFunctionType;
    }

    public void setObjectiveFunctionType(ObjectiveFunctionType objectiveFunctionType) {
        this.objectiveFunctionType = objectiveFunctionType;
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

    public static ObjectiveFunctionParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        ObjectiveFunctionParameters parameters = loadDefault();
        platformConfig.getOptionalModuleConfig(OBJECTIVE_FUNCTION)
                .ifPresent(config -> {
                    parameters.setObjectiveFunctionType(config.getEnumProperty(TYPE, ObjectiveFunctionType.class,
                            DEFAULT_OBJECTIVE_FUNCTION));
                    parameters.setForbidCostIncrease(config.getBooleanProperty(FORBID_COST_INCREASE, DEFAULT_FORBID_COST_INCREASE));
                    parameters.setCurativeMinObjImprovement(config.getDoubleProperty(CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT));
                    parameters.setPreventiveStopCriterion(config.getEnumProperty(PREVENTIVE_STOP_CRITERION, PreventiveStopCriterion.class,
                            DEFAULT_PREVENTIVE_STOP_CRITERION));
                    parameters.setCurativeStopCriterion(config.getEnumProperty(CURATIVE_STOP_CRITERION, CurativeStopCriterion.class,
                            DEFAULT_CURATIVE_STOP_CRITERION));
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
