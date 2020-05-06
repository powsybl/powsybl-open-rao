package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class StateAdder {
    Crac parent;
    private Instant instant = null;
    private Contingency contingency = null;

    public StateAdder(Crac parent) {
        this.parent = parent;
    }

    public StateAdder setInstant(Instant instant) {
        Objects.requireNonNull(instant);
        this.instant = instant;
        return this;
    }

    public StateAdder setContingency(Contingency contingency) {
        Objects.requireNonNull(contingency);
        this.contingency = contingency;
        return this;
    }

    public Crac add() {
        if (this.instant == null) {
            throw new FaraoException("Cannot add a state with no specified instant");
        }
        parent.addState(this.contingency, this.instant);
        return parent;
    }
}
