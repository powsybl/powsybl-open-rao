/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.NetworkElement;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class NetworkElementImpl extends AbstractIdentifiable<NetworkElement> implements NetworkElement {

    NetworkElementImpl(String id, String name) {
        super(id, name);
    }

    NetworkElementImpl(String id) {
        this(id, id);
    }

    /**
     * Check if network elements are equals. Network elements are considered equals when IDs are equals.
     *
     * @param o If it's null or another object than NetworkElement it will return false.
     * @return A boolean true if objects are equals, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NetworkElementImpl networkElement = (NetworkElementImpl) o;
        return super.equals(networkElement);
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
