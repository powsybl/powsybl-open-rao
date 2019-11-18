/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import java.util.List;

/**
 * Business object for a contingency in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class Contingency extends AbstractIdentifiable {

    private List<NetworkElement> networkElements;

    public Contingency(String id, String name, final List<NetworkElement> networkElements) {
        super(id, name);
        this.networkElements = networkElements;
    }

    public List<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    public void setNetworkElements(List<NetworkElement> networkElements) {
        this.networkElements = networkElements;
    }

    public void addNetworkElement(NetworkElement networkElement) {
        networkElements.add(networkElement);
    }
}
