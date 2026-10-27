/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.impl;

import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.data.crac.api.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class VoltageCnecResult {

    private static final ElementaryVoltageCnecResult DEFAULT_RESULT = new ElementaryVoltageCnecResult(null);
    private final Map<Instant, ElementaryVoltageCnecResult> results;
    private Instant lastInstantAdded;
    private final AtomicBoolean voltageSecurityStatus;

    VoltageCnecResult(RaoSecurityStatusHolder raoSecurityStatusHolder) {
        this.results = new HashMap<>();
        this.lastInstantAdded = null;
        this.voltageSecurityStatus = new AtomicBoolean(true);
        raoSecurityStatusHolder.registerSecurityStatus(this.voltageSecurityStatus, PhysicalParameter.VOLTAGE);
    }

    public ElementaryVoltageCnecResult getResult(Instant optimizedInstant) {
        return results.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryVoltageCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        boolean latestInstantUpdated = false;
        if (lastInstantAdded == null) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        } else if (optimizedInstant != null && !lastInstantAdded.comesAfter(optimizedInstant)) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        }

        // update security status holder only if posterior result added
        results.putIfAbsent(optimizedInstant, new ElementaryVoltageCnecResult(latestInstantUpdated ? voltageSecurityStatus : null));
        return results.get(optimizedInstant);
    }
}
