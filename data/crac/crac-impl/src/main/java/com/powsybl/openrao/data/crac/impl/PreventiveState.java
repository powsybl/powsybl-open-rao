/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PreventiveState implements State {

    private final Instant instant;
    private final OffsetDateTime timestamp;
    private final String id;

    PreventiveState(Instant instant, OffsetDateTime timestamp) {
        if (!instant.isPreventive()) {
            throw new OpenRaoException("Instant must be preventive");
        }
        this.instant = instant;
        this.timestamp = timestamp;
        this.id = StateIdHelper.getStateId(instant, timestamp);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.empty();
    }

    @Override
    public Optional<OffsetDateTime> getTimestamp() {
        return Optional.ofNullable(timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
