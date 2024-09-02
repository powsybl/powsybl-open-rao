/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoapi.parameters;

import com.powsybl.commons.config.PlatformConfig;

import java.util.*;
import static com.powsybl.openrao.raoapi.RaoParametersCommons.*;
/**
 * Not optimized cnecs parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class NotOptimizedCnecsParameters {

    private static final boolean DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS = false;
    private boolean doNotOptimizeCurativeCnecsForTsosWithoutCras = DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS;

    public boolean getDoNotOptimizeCurativeCnecsForTsosWithoutCras() {
        return doNotOptimizeCurativeCnecsForTsosWithoutCras;
    }

    public static NotOptimizedCnecsParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        NotOptimizedCnecsParameters parameters = new NotOptimizedCnecsParameters();
        platformConfig.getOptionalModuleConfig(NOT_OPTIMIZED_CNECS_SECTION)
            .ifPresent(config ->
                parameters.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(config.getBooleanProperty(DO_NOT_OPTIMIZE_CURATIVE_CNECS, DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS))
            );
        return parameters;
    }

    public void setDoNotOptimizeCurativeCnecsForTsosWithoutCras(boolean doNotOptimizeCurativeCnecsForTsosWithoutCras) {
        this.doNotOptimizeCurativeCnecsForTsosWithoutCras = doNotOptimizeCurativeCnecsForTsosWithoutCras;
    }
}
