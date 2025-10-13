/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.data.crac.api.Instant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class AngleCnecResult {

    private static final ElementaryAngleCnecResult DEFAULT_RESULT = new ElementaryAngleCnecResult();
    private final Map<Instant, ElementaryAngleCnecResult> results;

    AngleCnecResult() {
        results = new HashMap<>();
    }

    public ElementaryAngleCnecResult getResult(Instant optimizedInstant) {
        return results.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryAngleCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        results.putIfAbsent(optimizedInstant, new ElementaryAngleCnecResult());
        return results.get(optimizedInstant);
    }
}
