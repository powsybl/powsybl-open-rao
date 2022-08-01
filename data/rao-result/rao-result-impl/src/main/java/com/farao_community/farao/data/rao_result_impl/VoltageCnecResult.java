/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecResult {

    private static final ElementaryVoltageCnecResult DEFAULT_RESULT = new ElementaryVoltageCnecResult();
    private final Map<OptimizationState, ElementaryVoltageCnecResult> results;

    VoltageCnecResult() {
        results = new EnumMap<>(OptimizationState.class);
    }

    public ElementaryVoltageCnecResult getResult(OptimizationState optimizationState) {
        return results.getOrDefault(optimizationState, DEFAULT_RESULT);
    }

    public ElementaryVoltageCnecResult getAndCreateIfAbsentResultForOptimizationState(OptimizationState optimizationState) {
        results.putIfAbsent(optimizationState, new ElementaryVoltageCnecResult());
        return results.get(optimizationState);
    }
}
