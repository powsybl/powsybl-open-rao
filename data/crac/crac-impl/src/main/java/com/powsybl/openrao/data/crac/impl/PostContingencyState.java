/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
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
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PostContingencyState implements State {
    private static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private String id;
    private Contingency contingency;
    private Instant instant;
    private OffsetDateTime timestamp;

    PostContingencyState(Contingency contingency, Instant instant, OffsetDateTime timestamp) {
        if (instant.isPreventive()) {
            throw new OpenRaoException("Instant cannot be preventive");
        }
        this.id = StateIdHelper.getStateId(contingency, instant, timestamp);
        this.contingency = contingency;
        this.instant = instant;
        this.timestamp = timestamp;
    }

    public final String getId() {
        return id;
    }

    @Override
    public Instant getInstant() {
        return instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.of(contingency);
    }

    @Override
    public Optional<OffsetDateTime> getTimestamp() {
        return Optional.ofNullable(timestamp);
    }

    /**
     * Check if states are equals. States are considered equals when instant and contingency are equals if
     * contingency is present. Otherwise they are considered equals when instant are equals.
     *
     * @param o: If it's null or another object than State it will return false.
     * @return A boolean true if objects are equals, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        State state = (State) o;
        Optional<Contingency> oContingency = state.getContingency();
        return state.getInstant().equals(instant)
            && oContingency.map(value -> value.equals(contingency)).orElseGet(() -> contingency == null)
            && state.getTimestamp().equals(Optional.ofNullable(timestamp));
    }

    @Override
    public int hashCode() {
        return contingency.hashCode() * 19 + instant.hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
