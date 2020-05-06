package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderToCrac {
    Crac parent;
    private String id = null;
    private String name = null;

    public NetworkElementAdderToCrac(Crac parent) {
        this.parent = parent;
    }

    public NetworkElementAdderToCrac setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    public NetworkElementAdderToCrac setName(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    public Crac add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add a network element with no specified id");
        }
        else if (this.name == null) {
            this.name = this.id;
        }
        parent.addNetworkElement(this.id, this.name);
        return parent;
    }
}
