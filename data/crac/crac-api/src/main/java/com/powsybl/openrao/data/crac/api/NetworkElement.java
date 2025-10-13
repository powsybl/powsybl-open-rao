/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Set;

/**
 * Element of the network referenced in the Crac
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public interface NetworkElement extends Identifiable<NetworkElement> {

    /**
     * Returns the location of the cnec, as a set of countries
     *
     * @param network the network object used to look for the location of the network element of the Cnec
     * @return a set of countries containing the cnec location(s). Note that a Cnec on a interconnection can
     * belong to two countries.
     */
    Set<Country> getLocation(Network network);
}
