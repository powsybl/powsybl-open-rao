/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationTest {
    @Test
    void checkThatConfigurationManipulationWorksAsExpected() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, false, "HibNodeName", marketArea, null));
        BorderDirection borderDirection = new BorderDirection("Paris", "Berlin", false);
        configuration.addBorderDirection(borderDirection);

        assertEquals(2, configuration.getMarketAreas().size());
        assertEquals(1, configuration.getVirtualHubs().size());
        assertEquals(1, configuration.getBorderDirections().size());
        assertTrue(configuration.getMarketAreas().contains(marketArea));
        assertTrue(configuration.getBorderDirections().contains(borderDirection));
    }

    @Test
    void checkThatAddingNullMarketAreaInConfigurationThrows() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> configuration.addMarketArea(null),
            "Null market area addition in configuration should throw but does not"
        );
        assertEquals("Virtual hubs configuration does not allow adding null market area", thrown.getMessage());
    }

    @Test
    void checkThatAddingNullVirtualHubInConfigurationThrows() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> configuration.addVirtualHub(null),
            "Null virtual hub addition in configuration should throw but does not"
        );
        assertEquals("Virtual hubs configuration does not allow adding null virtual hub", thrown.getMessage());
    }

    @Test
    void checkThatAddingNullBorderDirectionInConfigurationThrows() {
        VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> configuration.addBorderDirection(null),
            "Null border direction addition in configuration should throw but does not"
        );
        assertEquals("Virtual hubs configuration does not allow adding null border direction", thrown.getMessage());
    }
}
