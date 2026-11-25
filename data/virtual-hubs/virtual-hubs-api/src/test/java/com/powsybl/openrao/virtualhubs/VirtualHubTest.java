/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class VirtualHubTest {
    @Test
    void checkThatVirtualHubIsCorrectlyCreated() {
        final MarketArea marketArea = new MarketArea(
            "AreaCode",
            "AreaEic",
            true,
            false,
            false,
            false,
            null,
            null,
            false,
            false
        );
        final VirtualHub myVirtualHub = new VirtualHub(
            "HubCode",
            "HubEic",
            true,
            false,
            "HubNodeName",
            marketArea,
            "OppositeHub",
            false,
            null,
            null,
            null
        );
        Assertions.assertThat(myVirtualHub.code()).isEqualTo("HubCode");
        Assertions.assertThat(myVirtualHub.eic()).isEqualTo("HubEic");
        Assertions.assertThat(myVirtualHub.isMcParticipant()).isTrue();
        Assertions.assertThat(myVirtualHub.isAhc()).isFalse();
        Assertions.assertThat(myVirtualHub.nodeName()).isEqualTo("HubNodeName");
        Assertions.assertThat(myVirtualHub.relatedMa()).isEqualTo(marketArea);
        Assertions.assertThat(myVirtualHub.oppositeHub()).isEqualTo("OppositeHub");
        Assertions.assertThat(myVirtualHub.isSdac()).isFalse();
        Assertions.assertThat(myVirtualHub.splittingFactor()).isNull();
        Assertions.assertThat(myVirtualHub.lf()).isNull();
        Assertions.assertThat(myVirtualHub.rlf()).isNull();

        final MarketArea otherMarketArea = new MarketArea(
            "OtherAreaCode",
            "OtherAreaEic",
            false,
            false,
            false,
            false,
            null,
            null,
            false,
            false
        );
        final VirtualHub myOtherVirtualHub = new VirtualHub(
            "OtherHubCode",
            "OtherHubEic",
            false,
            true,
            "OtherHubNodeName",
            otherMarketArea,
            null,
            true,
            new BigDecimal(1),
            new BigDecimal(2),
            new BigDecimal(3)
        );
        Assertions.assertThat(myOtherVirtualHub.code()).isEqualTo("OtherHubCode");
        Assertions.assertThat(myOtherVirtualHub.eic()).isEqualTo("OtherHubEic");
        Assertions.assertThat(myOtherVirtualHub.isMcParticipant()).isFalse();
        Assertions.assertThat(myOtherVirtualHub.isAhc()).isTrue();
        Assertions.assertThat(myOtherVirtualHub.nodeName()).isEqualTo("OtherHubNodeName");
        Assertions.assertThat(myOtherVirtualHub.relatedMa()).isEqualTo(otherMarketArea);
        Assertions.assertThat(myOtherVirtualHub.oppositeHub()).isNull();
        Assertions.assertThat(myOtherVirtualHub.isSdac()).isTrue();
        Assertions.assertThat(myOtherVirtualHub.splittingFactor()).isEqualTo(new BigDecimal(1));
        Assertions.assertThat(myOtherVirtualHub.lf()).isEqualTo(new BigDecimal(2));
        Assertions.assertThat(myOtherVirtualHub.rlf()).isEqualTo(new BigDecimal(3));
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenCodeIsNull() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false, false, false, null, null, false, false);
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub(null, "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub", false, null, null, null))
            .withMessage("VirtualHub creation does not allow null attribute 'code'");
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenEicIsNull() {
        final MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false, false, false, null, null, false, false);
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub("HubCode", null, true, false, "HubNodeName", marketArea, "OppositeHub", false, null, null, null))
            .withMessage("VirtualHub creation does not allow null attribute 'eic'");
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenMarketAreaIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", null, "OppositeHub", false, null, null, null))
            .withMessage("VirtualHub creation does not allow null attribute 'relatedMa'");
    }
}
