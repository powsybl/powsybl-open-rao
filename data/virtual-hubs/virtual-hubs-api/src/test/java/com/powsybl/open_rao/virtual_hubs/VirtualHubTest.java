/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.virtual_hubs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class VirtualHubTest {
    @Test
    void checkThatVirtualHubIsCorrectlyCreated() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        VirtualHub myVirtualHub = new VirtualHub("HubCode", "HubEic", true, "HubNodeName", marketArea);
        assertEquals("HubCode", myVirtualHub.getCode());
        assertEquals("HubEic", myVirtualHub.getEic());
        assertTrue(myVirtualHub.isMcParticipant());
        assertEquals("HubNodeName", myVirtualHub.getNodeName());
        assertEquals(marketArea, myVirtualHub.getRelatedMa());

        MarketArea otherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false);
        VirtualHub myOtherVirtualHub = new VirtualHub("OtherHubCode", "OtherHubEic", false, "OtherHubNodeName", otherMarketArea);
        assertEquals("OtherHubCode", myOtherVirtualHub.getCode());
        assertEquals("OtherHubEic", myOtherVirtualHub.getEic());
        assertFalse(myOtherVirtualHub.isMcParticipant());
        assertEquals("OtherHubNodeName", myOtherVirtualHub.getNodeName());
        assertEquals(otherMarketArea, myOtherVirtualHub.getRelatedMa());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenCodeIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub(null, "HubEic", true, "HubNodeName", marketArea),
            "Null code in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null code", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenEicIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", null, true, "HubNodeName", marketArea),
            "Null code in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null eic", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenNodeNameIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", "HubEic", true, null, marketArea),
            "Null nodeName in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null nodeName", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenMarketAreaIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", "HubEic", true, "HubNodeName", null),
            "Null relatedMa in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null relatedMa", thrown.getMessage());
    }

}
