/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.NetworkElementAdder;
import com.farao_community.farao.data.crac_api.NetworkElementParent;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderImpl<T extends NetworkElementParent> implements NetworkElementAdder<T> {
    T parent;
    private String id = null;
    private String name = null;

    public NetworkElementAdderImpl(T parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public NetworkElementAdderImpl<T> setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    @Override
    public NetworkElementAdderImpl<T> setName(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    @Override
    public T add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add a network element with no specified id. Please use setId.");
        } else if (this.name == null) {
            this.name = this.id;
        }
        parent.addNetworkElement(new NetworkElement(this.id, this.name));
        return parent;
    }
}
