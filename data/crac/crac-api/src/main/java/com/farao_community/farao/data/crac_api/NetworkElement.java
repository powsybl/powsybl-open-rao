/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Optional;
import java.util.Set;

/**
 * Element of the network in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public interface NetworkElement extends Identifiable<NetworkElement> {
    /**
     * Returns the location of the network element, as a set of optional countries
     * @param network: the network object used to look for the network element
     * @return a set of optional countries containing the network element
     */
    Set<Optional<Country>> getLocation(Network network);
}
