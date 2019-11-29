/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.InjectionStrategy;
import com.farao_community.farao.flow_decomposition.full_line_decomposition.FullLineDecompositionParameters.PstStrategy;

import java.util.Objects;
import java.util.Optional;

/**
 * Plugin dealing with full line decomposition parameters extension loading
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(FlowDecompositionParameters.ConfigLoader.class)
public class FullLineDecompositionParametersConfigLoader implements FlowDecompositionParameters.ConfigLoader<FullLineDecompositionParameters> {

    private static final String MODULE_NAME = "full-line-decomposition-parameters";

    @Override
    public FullLineDecompositionParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        FullLineDecompositionParameters parameters = new FullLineDecompositionParameters();
        Optional<ModuleConfig> configOptional = platformConfig.getOptionalModuleConfig(MODULE_NAME);
        if (configOptional.isPresent()) {
            ModuleConfig config = configOptional.get();
            parameters.setInjectionStrategy(config.getEnumProperty("injectionStrategy", InjectionStrategy.class, FullLineDecompositionParameters.DEFAULT_INJECTION_STRATEGY));
            parameters.setPexMatrixTolerance(config.getDoubleProperty("pexMatrixTolerance", FullLineDecompositionParameters.DEFAULT_PEX_MATRIX_TOLERANCE));
            parameters.setThreadsNumber(config.getIntProperty("threadsNumber", FullLineDecompositionParameters.DEFAULT_THREADS_NUMBER));
            parameters.setPstStrategy(config.getEnumProperty("pstStrategy", PstStrategy.class, FullLineDecompositionParameters.DEFAULT_PST_STRATEGY));
        }
        return parameters;
    }

    @Override
    public String getExtensionName() {
        return "FullLineDecompositionParameters";
    }

    @Override
    public String getCategoryName() {
        return "flow-decomposition-parameters";
    }

    @Override
    public Class<? super FullLineDecompositionParameters> getExtensionClass() {
        return FullLineDecompositionParameters.class;
    }
}
