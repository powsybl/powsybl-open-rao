/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.ContingencyAdder;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.NetworkElementAdder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SimpleContingencyAdder extends AbstractIdentifiableAdder<ContingencyAdder> implements ContingencyAdder {
    SimpleCrac parent;
    private final Set<NetworkElement> networkElements = new HashSet<>();
    private final Set<String> xnodeIds = new HashSet<>();

    public SimpleContingencyAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public NetworkElementAdder<ContingencyAdder> newNetworkElement() {
        return new NetworkElementAdderImpl<>(this);
    }

    @Override
    public Contingency add() {
        checkId();
        Contingency contingency;
        if (!this.xnodeIds.isEmpty()) {
            contingency = new XnodeContingency(this.id, this.name, this.xnodeIds);
        } else {
            // this also applies if both sets are empty
            contingency = new ComplexContingency(this.id, this.name, this.networkElements);
        }
        parent.addContingency(contingency);
        return parent.getContingency(contingency.getId());
    }

    @Override
    public ContingencyAdder addNetworkElement(NetworkElement networkElement) {
        if (!this.xnodeIds.isEmpty()) {
            throw new FaraoException("You cannot mix Xnodes and NetworkElements in the contingency adder");
        }
        this.networkElements.add(networkElement);
        return this;
    }

    @Override
    public ContingencyAdder addXnode(String xnode) {
        if (!this.networkElements.isEmpty()) {
            throw new FaraoException("You cannot mix Xnodes and NetworkElements in the contingency adder");
        }
        this.xnodeIds.add(xnode);
        return this;
    }
}
