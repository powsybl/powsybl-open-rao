/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_impl;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecResult {

    private static final ElementaryAngleCnecResult DEFAULT_RESULT = new ElementaryAngleCnecResult();
    private final Map<String, ElementaryAngleCnecResult> results;

    AngleCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryAngleCnecResult getResult(String optimizedInstantId) {
        return results.getOrDefault(optimizedInstantId, DEFAULT_RESULT);
    }

    public ElementaryAngleCnecResult getAndCreateIfAbsentResultForOptimizationState(String optimizedInstantId) {
        results.putIfAbsent(optimizedInstantId, new ElementaryAngleCnecResult());
        return results.get(optimizedInstantId);
    }
}
