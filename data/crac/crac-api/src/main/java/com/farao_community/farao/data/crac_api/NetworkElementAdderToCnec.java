package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderToCnec {
    Cnec parent;
    private String id = null;
    private String name = null;

    public NetworkElementAdderToCnec(Cnec parent) {
        this.parent = parent;
    }

    public NetworkElementAdderToCnec setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    public NetworkElementAdderToCnec setName(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    public Cnec add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add a network element with no specified id");
        }
        else if (this.name == null) {
            this.name = this.id;
        }
        parent.setNetworkElement(new NetworkElement(this.id, this.name));
        return parent;
    }
}
