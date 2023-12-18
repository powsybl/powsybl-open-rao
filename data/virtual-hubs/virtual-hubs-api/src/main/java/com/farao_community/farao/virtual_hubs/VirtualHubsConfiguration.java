/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Virtual hubs configuration POJO
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
public class VirtualHubsConfiguration {
    private final List<MarketArea> marketAreas = new ArrayList<>();
    private final List<VirtualHub> virtualHubs = new ArrayList<>();

    public void addMarketArea(MarketArea marketArea) {
        marketAreas.add(Objects.requireNonNull(marketArea, "Virtual hubs configuration does not allow adding null market area"));
    }

    public void addVirtualHub(VirtualHub virtualHub) {
        virtualHubs.add(Objects.requireNonNull(virtualHub, "Virtual hubs configuration does not allow adding null virtual hub"));
    }

    public List<MarketArea> getMarketAreas() {
        return marketAreas;
    }

    public List<VirtualHub> getVirtualHubs() {
        return virtualHubs;
    }
}
