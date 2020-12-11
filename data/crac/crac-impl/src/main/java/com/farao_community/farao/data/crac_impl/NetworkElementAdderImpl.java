/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.NetworkElementAdder;
import com.farao_community.farao.data.crac_api.NetworkElementParent;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class NetworkElementAdderImpl<T extends NetworkElementParent<T>> extends AbstractIdentifiableAdder<NetworkElementAdder<T>> implements NetworkElementAdder<T> {
    T parent;

    public NetworkElementAdderImpl(T parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public T add() {
        checkId();
        parent.addNetworkElement(new NetworkElement(this.id, this.name));
        return parent;
    }
}
