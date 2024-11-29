/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.openrao.raoapi.parameters.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;


/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
@AutoService(RaoParameters.ConfigLoader.class)
public class OpenRaoSearchTreeParametersConfigLoader implements RaoParameters.ConfigLoader<OpenRaoSearchTreeParameters> {

    @Override
    public OpenRaoSearchTreeParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        List<String> searchTreeParams = Arrays.asList(ST_OBJECTIVE_FUNCTION_SECTION, ST_RANGE_ACTIONS_OPTIMIZATION_SECTION, ST_TOPOLOGICAL_ACTIONS_OPTIMIZATION_SECTION, MULTI_THREADING_SECTION, SECOND_PREVENTIVE_RAO_SECTION, LOAD_FLOW_AND_SENSITIVITY_COMPUTATION_SECTION, ST_MNEC_PARAMETERS_SECTION, ST_RELATIVE_MARGINS_SECTION, ST_LOOP_FLOW_PARAMETERS_SECTION);
        boolean anySearchTreeParams = searchTreeParams.stream().map(platformConfig::getOptionalModuleConfig).anyMatch(Optional::isPresent);
        if (!anySearchTreeParams) {
            return null;
        }
        OpenRaoSearchTreeParameters parameters = new OpenRaoSearchTreeParameters();
        parameters.setObjectiveFunctionParameters(ObjectiveFunctionParameters.load(platformConfig));
        parameters.setRangeActionsOptimizationParameters(RangeActionsOptimizationParameters.load(platformConfig));
        parameters.setTopoOptimizationParameters(TopoOptimizationParameters.load(platformConfig));
        parameters.setMultithreadingParameters(MultithreadingParameters.load(platformConfig));
        parameters.setSecondPreventiveRaoParameters(SecondPreventiveRaoParameters.load(platformConfig));
        parameters.setLoadFlowAndSensitivityParameters(LoadFlowAndSensitivityParameters.load(platformConfig));
        MnecParameters.load(platformConfig).ifPresent(parameters::setMnecParameters);
        RelativeMarginsParameters.load(platformConfig).ifPresent(parameters::setRelativeMarginsParameters);
        LoopFlowParameters.load(platformConfig).ifPresent(parameters::setLoopFlowParameters);
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return SEARCH_TREE_PARAMETERS;
    }

    @Override
    public String getCategoryName() {
        return "rao-parameters";
    }

    @Override
    public Class<? super OpenRaoSearchTreeParameters> getExtensionClass() {
        return OpenRaoSearchTreeParameters.class;
    }
}
