/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import com.farao_community.farao.data.crac_api.Instant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecResult {

    private static final ElementaryFlowCnecResult DEFAULT_RESULT = new ElementaryFlowCnecResult();
    private final Map<Instant, ElementaryFlowCnecResult> results;

    FlowCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryFlowCnecResult getResult(Instant optimizedInstant) {
        return results.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryFlowCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        results.putIfAbsent(optimizedInstant, new ElementaryFlowCnecResult());
        return results.get(optimizedInstant);
    }
}
