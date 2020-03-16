/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.SimpleCrac;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class DeserializerUtils {

    private DeserializerUtils() { }

    static Set<NetworkElement> getNetworkElementsFromIds(Set<String> networkElementsIds, SimpleCrac simpleCrac) {
        Set<NetworkElement> networkElements = new HashSet<>();
        networkElementsIds.forEach(neId -> {
            NetworkElement ne = simpleCrac.getNetworkElement(neId);
            if (ne == null) {
                throw new FaraoException(String.format("The network element [%s] is not defined in the Crac", neId));
            }
            networkElements.add(ne);
        });
        return networkElements;
    }
}
