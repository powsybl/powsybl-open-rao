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
    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public Optional<Contingency> getContingency() {
        return Optional.ofNullable(contingency);
    }

    @Override
    public void setContingency(Optional<Contingency> contingency) {
        this.contingency = contingency.orElse(null);
    }

    @Override
    public int compareTo(State state) {
        return (int) (instant.getDuration() - state.getInstant().getDuration());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        State state = (State) o;

        if (!getContingency().isPresent() && !state.getContingency().isPresent()) {
            return getInstant().getDuration() == state.getInstant().getDuration();
        } else if (getContingency().isPresent() && state.getContingency().isPresent()) {
            return getContingency().get().getId().equals(state.getContingency().get().getId()) &&
                getInstant().getDuration() == state.getInstant().getDuration();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        if (contingency != null) {
            return String.format("%s at instant %f", contingency.getId(), instant.getDuration()).hashCode();
        } else {
            return String.format("preventive at instant %f", instant.getDuration()).hashCode();
        }
    }

    @Override
    public String toString() {
        String name = instant.getId();
        if (contingency != null) {
            name += String.format(" - %s", contingency.getId());
        }
        return name;
    }
}
