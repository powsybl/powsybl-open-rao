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
class MarketAreaTest {
    @Test
    void checkThatMarketAreaIsCorrectlyCreated() {
        MarketArea myMarketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        assertEquals("AreaCode", myMarketArea.code());
        assertEquals("AreaEic", myMarketArea.eic());
        assertTrue(myMarketArea.isMcParticipant());
        assertFalse(myMarketArea.isAhc());

        MarketArea myOtherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, true);
        assertEquals("OtherAreaCode", myOtherMarketArea.code());
        assertEquals("OtherAreaEic", myOtherMarketArea.eic());
        assertFalse(myOtherMarketArea.isMcParticipant());
        assertTrue(myOtherMarketArea.isAhc());
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenCodeIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new MarketArea(null, "AreaEic", true, false),
            "Null code in MarketArea creation should throw but does not"
        );
        assertEquals("MarketArea creation does not allow null code", thrown.getMessage());
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenEicIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new MarketArea("AreaCode", null, true, false),
            "Null eic in MarketArea creation should throw but does not"
        );
        assertEquals("MarketArea creation does not allow null eic", thrown.getMessage());
    }
}
