/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.virtual_hubs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubsConfigurationTest {
    @Test
    public void checkThatConfigurationManipulationWorksAsExpected() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, "HibNodeName", marketArea));

        assertEquals(2, configuration.getMarketAreas().size());
        assertEquals(1, configuration.getVirtualHubs().size());
        assertTrue(configuration.getMarketAreas().contains(marketArea));
    }

    @Test
    public void checkThatAddingNullMarketAreaInConfigurationThrows() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> configuration.addMarketArea(null),
            "Null market area addition in configuration should throw but does not"
        );
        assertEquals("Virtual hubs configuration does not allow adding null market area", thrown.getMessage());
    }

    @Test
    public void checkThatAddingNullVirtualHubInConfigurationThrows() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> configuration.addVirtualHub(null),
            "Null virtual hub addition in configuration should throw but does not"
        );
        assertEquals("Virtual hubs configuration does not allow adding null virtual hub", thrown.getMessage());
    }
}
