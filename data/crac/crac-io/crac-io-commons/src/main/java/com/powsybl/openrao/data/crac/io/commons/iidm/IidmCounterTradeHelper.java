/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.iidm;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.CountryGraph;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;

import java.util.List;

/**
 * Utility class to be used in Crac creators.
 * It exposes useful functions to import counter-trade range actions.
 *
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public class IidmCounterTradeHelper {

    private final String areaId;
    private final List<String> neighbors;

    public IidmCounterTradeHelper(String areaId, Network network) {
        this.areaId = areaId;
        this.neighbors = new CountryGraph(network).getNeighbors(toCountry(areaId)).stream()
            .map(Country::toString)
            .sorted()
            .toList();
    }

    private static Country toCountry(String areaId) {
        try {
            return Country.valueOf(areaId);
        } catch (IllegalArgumentException e) {
            throw new OpenRaoException(String.format("Area %s is not a country, its neighbors cannot be computed from the network", areaId));
        }
    }

    /**
     * Returns the areas sharing a border with the area in the network, sorted to get a deterministic order.
     */
    public List<String> getNeighbors() {
        return neighbors;
    }

    /**
     * Checks that each connected area of the counter-trade range action shares a border with the area in the network.
     */
    public void validateConnectedAreas(CounterTradeRangeAction counterTradeRangeAction) {
        for (ConnectedArea connectedArea : counterTradeRangeAction.getConnectedAreas()) {
            if (!neighbors.contains(connectedArea.getAreaId())) {
                throw new OpenRaoException(String.format("Connected area %s of CounterTradeRangeAction %s does not share a border with area %s in the network",
                    connectedArea.getAreaId(), counterTradeRangeAction.getId(), areaId));
            }
        }
    }
}
