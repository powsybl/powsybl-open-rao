/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.contingency.Contingency;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MockState implements State {
    private final Instant instant;
    private final Contingency contingency;
    private final OffsetDateTime timestamp;

    public MockState(Instant instant, Contingency contingency, OffsetDateTime timestamp) {
        this.instant = instant;
        this.contingency = contingency;
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return "";
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.ofNullable(contingency);
    }

    @Override
    public Optional<OffsetDateTime> getTimestamp() {
        return Optional.ofNullable(timestamp);
    }
}
