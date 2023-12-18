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
class MarketAreaTest {
    @Test
    public void checkThatMarketAreaIsCorrectlyCreated() {
        MarketArea myMarketArea = new MarketArea("AreaCode", "AreaEic", true);
        assertEquals("AreaCode", myMarketArea.getCode());
        assertEquals("AreaEic", myMarketArea.getEic());
        assertTrue(myMarketArea.isMcParticipant());

        MarketArea myOtherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false);
        assertEquals("OtherAreaCode", myOtherMarketArea.getCode());
        assertEquals("OtherAreaEic", myOtherMarketArea.getEic());
        assertFalse(myOtherMarketArea.isMcParticipant());
    }

    @Test
    public void checkThatMarketAreaCreationThrowsWhenCodeIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new MarketArea(null, "AreaEic", true),
            "Null code in MarketArea creation should throw but does not"
        );
        assertEquals("MarketArea creation does not allow null code", thrown.getMessage());
    }

    @Test
    public void checkThatMarketAreaCreationThrowsWhenEicIsNull() {
        NullPointerException thrown = assertThrows(
            NullPointerException.class,
            () -> new MarketArea("AreaCode", null, true),
            "Null eic in MarketArea creation should throw but does not"
        );
        assertEquals("MarketArea creation does not allow null eic", thrown.getMessage());
    }
}
