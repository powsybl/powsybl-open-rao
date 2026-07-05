/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters.extensions;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.reports.RaoApiReports;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.CURATIVE_MIN_OBJ_IMPROVEMENT;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.ST_OBJECTIVE_FUNCTION_SECTION;

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

    public static SearchTreeRaoObjectiveFunctionParameters load(final PlatformConfig platformConfig, final ReportNode reportNode) {
        SearchTreeRaoObjectiveFunctionParameters parameters = new SearchTreeRaoObjectiveFunctionParameters();
        platformConfig.getOptionalModuleConfig(ST_OBJECTIVE_FUNCTION_SECTION)
            .ifPresent(config -> parameters.setCurativeMinObjImprovement(config.getDoubleProperty(CURATIVE_MIN_OBJ_IMPROVEMENT, DEFAULT_CURATIVE_MIN_OBJ_IMPROVEMENT), reportNode));
        return parameters;
    }

    public void setCurativeMinObjImprovement(final double curativeRaoMinObjImprovement, final ReportNode reportNode) {
        if (curativeRaoMinObjImprovement < 0) {
            RaoApiReports.reportNegativeMinimumObjectiveImprovement(reportNode, curativeRaoMinObjImprovement);
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
