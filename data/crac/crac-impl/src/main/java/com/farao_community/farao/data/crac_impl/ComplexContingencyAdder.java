/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

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
public class ComplexContingencyAdder extends AbstractIdentifiableAdder<ComplexContingencyAdder> implements ContingencyAdder {
    SimpleCrac parent;
    private Set<NetworkElement> networkElements = new HashSet<>();

    public ComplexContingencyAdder(SimpleCrac parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public NetworkElementAdder newNetworkElement() {
        return new NetworkElementAdderImpl<ContingencyAdder>(this);
    }

    @Override
    public Contingency add() {
        checkId();
        ComplexContingency contingency = new ComplexContingency(this.id, this.name, this.networkElements);
        parent.addContingency(contingency);
        return parent.getContingency(contingency.getId());
    }

    @Override
    public NetworkElement addNetworkElement(NetworkElement networkElement) {
        this.networkElements.add(networkElement);
        return networkElement;
    }
}
