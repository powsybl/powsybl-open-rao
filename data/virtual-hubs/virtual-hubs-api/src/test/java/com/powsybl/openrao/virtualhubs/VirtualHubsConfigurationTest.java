/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubsConfigurationTest {
    @Test
    void checkThatConfigurationManipulationWorksAsExpected() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();

        final Region region = new Region("RegionCode", "RegionEic");
        configuration.setRegion(region);
        final SynchronousArea synchronousArea = new SynchronousArea("saCode", "saEic", false, false, null, null);
        configuration.addSynchronousArea(synchronousArea);
        final ControlArea controlArea = new ControlArea("caCode", "caEic", false, false, false);
        configuration.addControlArea(controlArea);
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false, false, false, synchronousArea, controlArea, false, false);
        configuration.addMarketArea(marketArea);
        final MarketArea otherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, false, false, false, null, null, false, false);
        configuration.addMarketArea(otherMarketArea);
        final VirtualHub virtualHub = new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", marketArea, null, false, null, null, null);
        configuration.addVirtualHub(virtualHub);
        final BorderDirection borderDirection = new BorderDirection("Paris", "Berlin", false, false);
        configuration.addBorderDirection(borderDirection);
        final InternalHvdc internalHvdc = new InternalHvdc("HVDC_EIC", "HVDC_CODE", List.of(), marketArea, false, false);
        configuration.addInternalHvdc(internalHvdc);

        Assertions.assertThat(configuration.getRegion()).isEqualTo(region);
        Assertions.assertThat(configuration.getSynchronousAreas()).containsExactly(synchronousArea);
        Assertions.assertThat(configuration.getControlAreas()).containsExactly(controlArea);
        Assertions.assertThat(configuration.getMarketAreas()).containsExactlyInAnyOrder(marketArea, otherMarketArea);
        Assertions.assertThat(configuration.getVirtualHubs()).containsExactly(virtualHub);
        Assertions.assertThat(configuration.getBorderDirections()).containsExactly(borderDirection);
        Assertions.assertThat(configuration.getInternalHvdcs()).containsExactly(internalHvdc);
    }

    @Test
    void checkThatAddingNullSynchronousAreaInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addSynchronousArea(null))
            .withMessage("Virtual hubs configuration does not allow adding null synchronous area");
    }

    @Test
    void checkThatAddingNullControlAreaInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addControlArea(null))
            .withMessage("Virtual hubs configuration does not allow adding null control area");
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

    @Test
    void checkThatAddingNullInternalHvdcInConfigurationThrows() {
        final VirtualHubsConfiguration configuration = new VirtualHubsConfiguration();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> configuration.addInternalHvdc(null))
            .withMessage("Virtual hubs configuration does not allow adding null internal hvdc");
    }
}
