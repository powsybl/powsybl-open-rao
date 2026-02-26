/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PostContingencyState implements State {
    private String id;
    private Contingency contingency;
    private Instant instant;
    private OffsetDateTime timestamp;

    public PostContingencyState(Contingency contingency, Instant instant, OffsetDateTime timestamp) {
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
     * Check if states are equal. States are considered equal when instant and contingency are equal if
     * contingency is present. Otherwise, they are considered equal when instants are equal.
     *
     * @param o If it's null or another object than State it will return false.
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
        // Check for contingency ID & elements IDs equality, because two same contingencies can have
        // different implementations (e.g. LineContingency & BranchContingency)
        return state.getInstant().equals(instant)
            && oContingency.isPresent() && oContingency.get().getId().equals(contingency.getId())
            && oContingency.get().getElements().stream().map(ContingencyElement::getId).collect(Collectors.toSet()).equals(contingency.getElements().stream().map(ContingencyElement::getId).collect(Collectors.toSet()))
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
