/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class ComplexContingencyAdder implements ContingencyAdder {
    SimpleCrac parent;
    private String id = null;
    private String name = null;
    private Set<NetworkElement> networkElements;

    public ComplexContingencyAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
        this.networkElements = new HashSet<>();
    }

    @Override
    public ComplexContingencyAdder setId(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }

    @Override
    public ComplexContingencyAdder setName(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }

    @Override
    public NetworkElementAdder newNetworkElement() {
        return new NetworkElementAdderImpl<ContingencyAdder>(this);
    }

    @Override
    public Contingency add() {
        if (this.id == null) {
            throw new FaraoException("Cannot add a contingency with no specified id. Please use setId.");
        } else if (this.name == null) {
            this.name = this.id;
        }
        ComplexContingency contingency = new ComplexContingency(this.id, this.name, this.networkElements);
        parent.addContingency(contingency);
        return contingency;
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        this.networkElements.add(networkElement);
        return networkElement;
    }
}
