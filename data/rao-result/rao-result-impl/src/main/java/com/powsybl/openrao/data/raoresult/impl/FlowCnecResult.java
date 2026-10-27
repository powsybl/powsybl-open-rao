/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class FlowCnecResult {

    private static final ElementaryFlowCnecResult DEFAULT_RESULT = new ElementaryFlowCnecResult(null);
    private final Map<Instant, ElementaryFlowCnecResult> results;
    private final AtomicBoolean flowSecurityStatus;
    private Instant lastInstantAdded;

    FlowCnecResult(RaoSecurityStatusHolder raoSecurityStatusHolder, boolean shouldIncludeCnec) {
        this.results = new HashMap<>();
        this.lastInstantAdded = null;
        this.flowSecurityStatus = new AtomicBoolean(true);
        if (shouldIncludeCnec) {
            raoSecurityStatusHolder.registerSecurityStatus(this.flowSecurityStatus, PhysicalParameter.FLOW);
        }
    }

    public ElementaryFlowCnecResult getResult(Instant optimizedInstant) {
        return results.getOrDefault(optimizedInstant, DEFAULT_RESULT);
    }

    public ElementaryFlowCnecResult getAndCreateIfAbsentResultForOptimizationState(Instant optimizedInstant) {
        boolean latestInstantUpdated = false;
        if (lastInstantAdded == null) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        } else if (optimizedInstant != null && !lastInstantAdded.comesAfter(optimizedInstant)) {
            lastInstantAdded = optimizedInstant;
            latestInstantUpdated = true;
        }

        // update security status holder only if posterior result added
        results.putIfAbsent(optimizedInstant, new ElementaryFlowCnecResult(latestInstantUpdated ? flowSecurityStatus : null));
        return results.get(optimizedInstant);
    }
}
