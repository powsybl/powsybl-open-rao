package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantAdder;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class InstantAdderImpl implements InstantAdder {

    private SimpleCrac parent;
    private String id;
    private Integer seconds;

    public InstantAdderImpl(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public InstantAdder setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    @Override
    public InstantAdder setSeconds(Integer seconds) {
        Objects.requireNonNull(seconds);
        this.seconds = seconds;
        return this;
    }

    @Override
    public Instant add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add an instant without an id. Please use setId.");
        }
        if (this.seconds == null) {
            throw new FaraoException("Cannot add an instant without a number of seconds. Please use setSeconds.");
        }
        Instant instant = new Instant(this.id, this.seconds);
        parent.addInstant(instant);
        return instant;
    }
}
