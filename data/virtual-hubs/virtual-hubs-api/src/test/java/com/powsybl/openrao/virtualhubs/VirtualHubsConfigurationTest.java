/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationTest {
    @Test
    void checkThatConfigurationManipulationWorksAsExpected() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        configuration.addMarketArea(marketArea);
        configuration.addMarketArea(new MarketArea("OtherAreaCode", "OtherAreaEic", false, false));
        configuration.addVirtualHub(new VirtualHub("HubCode", "HubEic", true, false, "HibNodeName", marketArea, null));
        final BorderDirection borderDirection = new BorderDirection("Paris", "Berlin", false);
        configuration.addBorderDirection(borderDirection);

        Assertions.assertThat(configuration.getMarketAreas()).hasSize(2);
        Assertions.assertThat(configuration.getVirtualHubs()).hasSize(1);
        Assertions.assertThat(configuration.getBorderDirections()).hasSize(1);
        Assertions.assertThat(configuration.getMarketAreas()).contains(marketArea);
        Assertions.assertThat(configuration.getBorderDirections()).contains(borderDirection);
    }

    @Test
    void checkThatAddingNullMarketAreaInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addMarketArea(null))
            .withMessage("Virtual hubs configuration does not allow adding null market area");
    }

    @Test
    void checkThatAddingNullVirtualHubInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addVirtualHub(null))
            .withMessage("Virtual hubs configuration does not allow adding null virtual hub");
    }

    @Test
    void checkThatAddingNullBorderDirectionInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addBorderDirection(null))
            .withMessage("Virtual hubs configuration does not allow adding null border direction");
    }
}
