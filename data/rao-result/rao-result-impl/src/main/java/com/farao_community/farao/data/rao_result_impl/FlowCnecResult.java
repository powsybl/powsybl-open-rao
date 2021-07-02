/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.rao_result_api.OptimizationState;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecResult {

    private static final ElementaryFlowCnecResult DEFAULT_RESULT = new ElementaryFlowCnecResult();
    private Map<OptimizationState, ElementaryFlowCnecResult> results;

    FlowCnecResult() {
        results = new EnumMap<>(OptimizationState.class);
    }

    public ElementaryFlowCnecResult getResult(OptimizationState optimizationState) {
        return results.getOrDefault(optimizationState, DEFAULT_RESULT);
    }

    public ElementaryFlowCnecResult getAndCreateIfAbsentResultForOptimizationState(OptimizationState optimizationState) {
        results.putIfAbsent(optimizationState, new ElementaryFlowCnecResult());
        return results.get(optimizationState);
    }
}
