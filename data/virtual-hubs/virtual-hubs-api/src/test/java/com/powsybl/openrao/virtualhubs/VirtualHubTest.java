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
class VirtualHubTest {
    @Test
    void checkThatVirtualHubIsCorrectlyCreated() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        VirtualHub myVirtualHub = new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub");
        assertEquals("HubCode", myVirtualHub.code());
        assertEquals("HubEic", myVirtualHub.eic());
        assertTrue(myVirtualHub.isMcParticipant());
        assertFalse(myVirtualHub.isAhc());
        assertEquals("HubNodeName", myVirtualHub.nodeName());
        assertEquals(marketArea, myVirtualHub.relatedMa());
        assertEquals("OppositeHub", myVirtualHub.oppositeHub());

        MarketArea otherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, false);
        VirtualHub myOtherVirtualHub = new VirtualHub("OtherHubCode", "OtherHubEic", false, true, "OtherHubNodeName", otherMarketArea, null);
        assertEquals("OtherHubCode", myOtherVirtualHub.code());
        assertEquals("OtherHubEic", myOtherVirtualHub.eic());
        assertFalse(myOtherVirtualHub.isMcParticipant());
        assertTrue(myOtherVirtualHub.isAhc());
        assertEquals("OtherHubNodeName", myOtherVirtualHub.nodeName());
        assertEquals(otherMarketArea, myOtherVirtualHub.relatedMa());
        assertNull(myOtherVirtualHub.oppositeHub());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenCodeIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub(null, "HubEic", true, false, "HubNodeName", marketArea, "OppositeHub"),
            "Null code in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null code", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenEicIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", null, true, false, "HubNodeName", marketArea, "OppositeHub"),
            "Null code in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null eic", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenNodeNameIsNull() {
        MarketArea marketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", "HubEic", true, false, null, marketArea, "OppositeHub"),
            "Null nodeName in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null nodeName", thrown.getMessage());
    }

    @Test
    void checkThatVirtualHubCreationThrowsWhenMarketAreaIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new VirtualHub("HubCode", "HubEic", true, false, "HubNodeName", null, "OppositeHub"),
            "Null relatedMa in VirtualHub creation should throw but does not"
        );
        assertEquals("VirtualHub creation does not allow null relatedMa", thrown.getMessage());
    }

}
