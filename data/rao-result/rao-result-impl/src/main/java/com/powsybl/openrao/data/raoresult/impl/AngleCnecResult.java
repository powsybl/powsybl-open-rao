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
public class AngleCnecResult {

    private static final ElementaryAngleCnecResult DEFAULT_RESULT = new ElementaryAngleCnecResult(null);
    private final Map<Instant, ElementaryAngleCnecResult> results;
    private Instant lastInstantAdded;
    private final AtomicBoolean angleSecurityStatus;

    AngleCnecResult(RaoSecurityStatusHolder raoSecurityStatusHolder) {
        this.results = new HashMap<>();
        this.lastInstantAdded = null;
        this.angleSecurityStatus = new AtomicBoolean(true);
        raoSecurityStatusHolder.registerSecurityStatus(this.angleSecurityStatus, PhysicalParameter.ANGLE);
    }

    public ElementaryAngleCnecResult getResult(Instant optimizedInstant) {
        return results.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryAngleCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        boolean latestInstantUpdated = false;
        if (lastInstantAdded == null) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        } else if (optimizedInstant != null && !lastInstantAdded.comesAfter(optimizedInstant)) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        }

        // update security status holder only if posterior result added
        results.putIfAbsent(optimizedInstant, new ElementaryAngleCnecResult(latestInstantUpdated ? angleSecurityStatus : null));
        return results.get(optimizedInstant);
    }
}
