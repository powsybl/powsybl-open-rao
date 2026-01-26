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
class MarketAreaTest {
    @Test
    void checkThatMarketAreaIsCorrectlyCreated() {
        MarketArea myMarketArea = new MarketArea("AreaCode", "AreaEic", true, false);
        Assertions.assertThat(myMarketArea.code()).isEqualTo("AreaCode");
        Assertions.assertThat(myMarketArea.eic()).isEqualTo("AreaEic");
        Assertions.assertThat(myMarketArea.isMcParticipant()).isTrue();
        Assertions.assertThat(myMarketArea.isAhc()).isFalse();

        MarketArea myOtherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, true);
        Assertions.assertThat(myOtherMarketArea.code()).isEqualTo("OtherAreaCode");
        Assertions.assertThat(myOtherMarketArea.eic()).isEqualTo("OtherAreaEic");
        Assertions.assertThat(myOtherMarketArea.isMcParticipant()).isFalse();
        Assertions.assertThat(myOtherMarketArea.isAhc()).isTrue();
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenCodeIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new MarketArea(null, "AreaEic", true, false))
            .withMessage("MarketArea creation does not allow null code");
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenEicIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new MarketArea("AreaCode", null, true, false))
            .withMessage("MarketArea creation does not allow null eic");
    }
}
