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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

/**
 * Business object for a state (instant and contingency) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class SimpleState implements State {

    private Contingency contingency;
    private Instant instant;

    @JsonCreator
    public SimpleState(@JsonProperty("contingency") Optional<Contingency> contingency,
                       @JsonProperty("instant") Instant instant) {
        this.contingency = contingency.orElse(null);
        this.instant = instant;
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
    public int compareTo(State state) {
        return instant.getSeconds() - state.getInstant().getSeconds();
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
        if (contingency != null) {
            return String.format("%s%s", contingency.getId(), instant.getId()).hashCode();
        } else {
            return String.format("preventive%s", instant.getId()).hashCode();
        }
    }

    @Override
    public String toString() {
        String name = instant.getId();
        if (contingency != null) {
            name += String.format(" - %s", contingency.getId());
        } else {
            name += " - preventive";
        }
        return name;
    }
}
