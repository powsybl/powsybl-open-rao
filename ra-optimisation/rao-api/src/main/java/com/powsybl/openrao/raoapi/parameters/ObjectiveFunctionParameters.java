/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Objective function parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class ObjectiveFunctionParameters {
    // Default values
    private static final ObjectiveFunctionType DEFAULT_OBJECTIVE_FUNCTION = ObjectiveFunctionType.SECURE_FLOW;
    private static final boolean DEFAULT_ENFORCE_CURATIVE_SECURITY = false;
    // Attributes
    private ObjectiveFunctionType type = DEFAULT_OBJECTIVE_FUNCTION;
    private boolean enforceCurativeSecurity = DEFAULT_ENFORCE_CURATIVE_SECURITY;

    // Enum
    public enum ObjectiveFunctionType {
        SECURE_FLOW,
        MAX_MIN_MARGIN,
        MAX_MIN_RELATIVE_MARGIN,
        MIN_COST;

        // TODO: positive margin is rather for SECURE_FLOW, isn't it?
        public boolean relativePositiveMargins() {
            return this.equals(MAX_MIN_RELATIVE_MARGIN);
        }

        public boolean costOptimization() {
            return this.equals(MIN_COST);
        }
    }

    // Getters and setters
    public ObjectiveFunctionType getType() {
        return type;
    }

    public void setType(ObjectiveFunctionType type) {
        this.type = type;
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
                    parameters.setEnforceCurativeSecurity(config.getBooleanProperty(ENFORCE_CURATIVE_SECURITY, DEFAULT_ENFORCE_CURATIVE_SECURITY));
                });
        return parameters;
    }
}
