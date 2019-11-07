/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_file_impl;

import java.util.List;

/**
 * Business object for a contingency in the CRAC file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class Contingency extends AbstractIdentifiable {

    private List<NetworkElement> elementsId;

    public Contingency(String id, String name, final List<NetworkElement> elementsId) {
        super(id, name);
        this.elementsId = elementsId;
    }

    @Override
    protected String getTypeDescription() {
        return "Contingency";
    }

    public List<NetworkElement> getElementsId() {
        return elementsId;
    }

    public void setElementsId(List<NetworkElement> elementsId) {
        this.elementsId = elementsId;
    }

    public void addNetworkElement(NetworkElement networkElement) {
        elementsId.add(networkElement);
    }
}
