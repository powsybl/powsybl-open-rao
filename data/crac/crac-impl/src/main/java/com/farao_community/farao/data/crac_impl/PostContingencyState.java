/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.fasterxml.jackson.annotation.*;

import java.util.Optional;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class PostContingencyState implements State {
    private String id;
    private Contingency contingency;
    private Instant instant;

    PostContingencyState(Contingency contingency, Instant instant) {
        this.id = contingency.getId() + " - " + instant.toString();
        this.contingency = contingency;
        this.instant = instant;
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
        return state.getInstant().equals(instant) && oContingency.map(value -> value.equals(contingency))
            .orElseGet(() -> contingency == null);
    }

    @Override
    public int hashCode() {
        return String.format("%s%s", contingency.getId(), instant.toString()).hashCode();
    }

    @Override
    public String toString() {
        return getId();
    }
}
