/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;

/**
 * Objective function parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class SearchTreeRaoObjectiveFunctionParameters {
    private static final double DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT = 0;
    private double curativeMinObjImprovement = DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT;

    public double getCurativeMinObjImprovement() {
        return curativeMinObjImprovement;
    }

    public static SearchTreeRaoObjectiveFunctionParameters load(PlatformConfig platformConfig) {
        SearchTreeRaoObjectiveFunctionParameters parameters = new SearchTreeRaoObjectiveFunctionParameters();
        platformConfig.getOptionalModuleConfig(ST_OBJECTIVE_FUNCTION_SECTION)
            .ifPresent(config -> parameters.setCurativeMinObjImprovement(config.getDoubleProperty(CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT)));
        return parameters;
    }

    public void setCurativeMinObjImprovement(double curativeRaoMinObjImprovement) {
        if (curativeRaoMinObjImprovement < 0) {
            BUSINESS_WARNS.warn("The value {} provided for curative RAO minimum objective improvement is smaller than 0. It will be set to + {}", curativeRaoMinObjImprovement, -curativeRaoMinObjImprovement);
        }
        this.curativeMinObjImprovement = Math.abs(curativeRaoMinObjImprovement);
    }

    public static double getCurativeMinObjImprovement(RaoParameters parameters) {
        if (parameters.hasExtension(OpenRaoSearchTreeParameters.class)) {
            return parameters.getExtension(OpenRaoSearchTreeParameters.class).getObjectiveFunctionParameters().getCurativeMinObjImprovement();
        }
        return DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT;
    }
}
