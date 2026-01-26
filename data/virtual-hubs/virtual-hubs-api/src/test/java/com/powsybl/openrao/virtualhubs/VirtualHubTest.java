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
class VirtualHubTest {
    @Test
    void checkThatVirtualHubIsCorrectlyCreated() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        final VirtualHub myVirtualHub = new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub");
        Assertions.assertThat(myVirtualHub.code()).isEqualTo("HubCode");
        Assertions.assertThat(myVirtualHub.eic()).isEqualTo("HubEic");
        Assertions.assertThat(myVirtualHub.isMcParticipant()).isTrue();
        Assertions.assertThat(myVirtualHub.isAhc()).isFalse();
        Assertions.assertThat(myVirtualHub.nodeName()).isEqualTo("HubNodeName");
        Assertions.assertThat(myVirtualHub.relatedMa()).isEqualTo(marketArea);
        Assertions.assertThat(myVirtualHub.oppositeHub()).isEqualTo("OppositeHub");

        final MarketArea otherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, false);
        final VirtualHub myOtherVirtualHub = new VirtualHub("OtherHubCode", "OtherHubEic", false, true, "OtherHubNodeName", otherMarketArea, null);
        Assertions.assertThat(myOtherVirtualHub.code()).isEqualTo("OtherHubCode");
        Assertions.assertThat(myOtherVirtualHub.eic()).isEqualTo("OtherHubEic");
        Assertions.assertThat(myOtherVirtualHub.isMcParticipant()).isFalse();
        Assertions.assertThat(myOtherVirtualHub.isAhc()).isTrue();
        Assertions.assertThat(myOtherVirtualHub.nodeName()).isEqualTo("OtherHubNodeName");
        Assertions.assertThat(myOtherVirtualHub.relatedMa()).isEqualTo(otherMarketArea);
        Assertions.assertThat(myOtherVirtualHub.oppositeHub()).isNull();
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenCodeIsNull() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub(null, "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub"))
            .withMessage("VirtualHub creation does not allow null code");
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenEicIsNull() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub("HubCode", null, true, false, "HubNodeName", marketArea, "OppositeHub"))
            .withMessage("VirtualHub creation does not allow null eic");
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenNodeNameIsNull() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub("HubCode", "HubEic", true, false, null, marketArea, "OppositeHub"))
            .withMessage("VirtualHub creation does not allow null nodeName");
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenMarketAreaIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", null, "OppositeHub"))
            .withMessage("VirtualHub creation does not allow null relatedMa");
    }

}
