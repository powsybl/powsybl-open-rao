/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.config.PlatformConfig;

import java.util.*;
import static com.farao_community.farao.rao_api.RaoParametersConstants.*;
/**
 * Not optimized cnecs parameters for RAO
 *
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class NotOptimizedCnecsParameters {
    private boolean doNotOptimizeCurativeCnecsForTsosWithoutCras;
    private Map<String, String> doNotOptimizeCnecsSecuredByTheirPst;

    private static final boolean DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS = false;
    private static final Map<String, String> DEFAULT_DO_NOT_OPTIMIZE_CNECS_SECURED_BY_THEIR_PST = new HashMap<>();

    public NotOptimizedCnecsParameters(boolean doNotOptimizeCurativeCnecsForTsosWithoutCras, Map<String, String> doNotOptimizeCnecsSecuredByTheirPst) {
        this.doNotOptimizeCurativeCnecsForTsosWithoutCras = doNotOptimizeCurativeCnecsForTsosWithoutCras;
        this.doNotOptimizeCnecsSecuredByTheirPst = doNotOptimizeCnecsSecuredByTheirPst;
    }

    public static NotOptimizedCnecsParameters loadDefault() {
        return new NotOptimizedCnecsParameters(DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS, DEFAULT_DO_NOT_OPTIMIZE_CNECS_SECURED_BY_THEIR_PST);
    }

    public Map<String, String> getDoNotOptimizeCnecsSecuredByTheirPst() {
        return doNotOptimizeCnecsSecuredByTheirPst;
    }

    public boolean getDoNotOptimizeCurativeCnecsForTsosWithoutCras() {
        return doNotOptimizeCurativeCnecsForTsosWithoutCras;
    }

    public static NotOptimizedCnecsParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);
        NotOptimizedCnecsParameters parameters = loadDefault();
        platformConfig.getOptionalModuleConfig(NOT_OPTIMIZED_CNECS)
                .ifPresent(config -> {
                    parameters.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(config.getBooleanProperty(DO_NOT_OPTIMIZE_CURATIVE_CNECS, DEFAULT_DO_NOT_OPTIMIZE_CURATIVE_CNECS_FOR_TSOS_WITHOUT_CRAS));
                    parameters.setDoNotOptimizeCnecsSecuredByTheirPst(ParametersUtil.convertListToMapStringString(config.getStringListProperty(DO_NOT_OPTIMIZE_CNECS_SECURED_BY_ITS_PST, ParametersUtil.convertMapStringStringToList(DEFAULT_DO_NOT_OPTIMIZE_CNECS_SECURED_BY_THEIR_PST))));
                });
        return parameters;
    }

    public void setDoNotOptimizeCurativeCnecsForTsosWithoutCras(boolean doNotOptimizeCurativeCnecsForTsosWithoutCras) {
        if (doNotOptimizeCurativeCnecsForTsosWithoutCras && !getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            throw new FaraoException("unoptimized-cnecs-in-series-with-psts and curative-rao-optimize-operators-not-sharing-cras are incompatible");
        }
        this.doNotOptimizeCurativeCnecsForTsosWithoutCras = doNotOptimizeCurativeCnecsForTsosWithoutCras;
    }

    public void setDoNotOptimizeCnecsSecuredByTheirPst(Map<String, String> doNotOptimizeCnecsSecuredByTheirPstEntry) {
        if (doNotOptimizeCnecsSecuredByTheirPstEntry != null && !doNotOptimizeCnecsSecuredByTheirPstEntry.isEmpty() && getDoNotOptimizeCurativeCnecsForTsosWithoutCras()) {
            throw new FaraoException("unoptimized-cnecs-in-series-with-psts and curative-rao-optimize-operators-not-sharing-cras are incompatible");
        }
        this.doNotOptimizeCnecsSecuredByTheirPst = Objects.requireNonNullElseGet(doNotOptimizeCnecsSecuredByTheirPstEntry, HashMap::new);
    }
}
