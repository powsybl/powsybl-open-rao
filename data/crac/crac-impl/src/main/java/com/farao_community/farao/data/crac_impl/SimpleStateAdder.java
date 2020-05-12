package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleStateAdder implements StateAdder {
    SimpleCrac parent;
    private Instant instant = null;
    private Contingency contingency = null;

    public SimpleStateAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public StateAdder setInstant(Instant instant) {
        Objects.requireNonNull(instant);
        this.instant = instant;
        return this;
    }

    @Override
    public StateAdder setContingency(Contingency contingency) {
        Objects.requireNonNull(contingency);
        this.contingency = contingency;
        return this;
    }

    @Override
    public State add() {
        if (this.instant == null) {
            throw new FaraoException("Cannot add a state with no specified instant. Please use setInstant.");
        }
        SimpleState state;
        if (this.contingency != null) {
            state = new SimpleState(Optional.of(this.contingency), this.instant);
        } else {
            state = new SimpleState(Optional.empty(), this.instant);
        }
        parent.addState(state);
        return state;
    }
}
