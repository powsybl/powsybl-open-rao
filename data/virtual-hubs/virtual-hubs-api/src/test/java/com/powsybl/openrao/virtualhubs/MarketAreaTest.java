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
        final SynchronousArea synchronousArea = new SynchronousArea("saCode", "saEic", false, false, null, null);
        final ControlArea controlArea = new ControlArea("caCode", "caEic", false, false, false);

        final MarketArea myMarketArea = new MarketArea("AreaCode", "AreaEic", true, false, true, false, synchronousArea, null, false, true);
        Assertions.assertThat(myMarketArea.code()).isEqualTo("AreaCode");
        Assertions.assertThat(myMarketArea.eic()).isEqualTo("AreaEic");
        Assertions.assertThat(myMarketArea.isMcParticipant()).isTrue();
        Assertions.assertThat(myMarketArea.isAhc()).isFalse();
        Assertions.assertThat(myMarketArea.isTechCounterparty()).isTrue();
        Assertions.assertThat(myMarketArea.isSdac()).isFalse();
        Assertions.assertThat(myMarketArea.synchronousArea()).isEqualTo(synchronousArea);
        Assertions.assertThat(myMarketArea.relatedCa()).isNull();
        Assertions.assertThat(myMarketArea.isItcp()).isFalse();
        Assertions.assertThat(myMarketArea.isIgnoreECs()).isTrue();

        final MarketArea myOtherMarketArea = new MarketArea("OtherAreaCode", "OtherAreaEic", false, true, false, true, null, controlArea, true, false);
        Assertions.assertThat(myOtherMarketArea.code()).isEqualTo("OtherAreaCode");
        Assertions.assertThat(myOtherMarketArea.eic()).isEqualTo("OtherAreaEic");
        Assertions.assertThat(myOtherMarketArea.isMcParticipant()).isFalse();
        Assertions.assertThat(myOtherMarketArea.isAhc()).isTrue();
        Assertions.assertThat(myOtherMarketArea.isTechCounterparty()).isFalse();
        Assertions.assertThat(myOtherMarketArea.isSdac()).isTrue();
        Assertions.assertThat(myOtherMarketArea.synchronousArea()).isNull();
        Assertions.assertThat(myOtherMarketArea.relatedCa()).isEqualTo(controlArea);
        Assertions.assertThat(myOtherMarketArea.isItcp()).isTrue();
        Assertions.assertThat(myOtherMarketArea.isIgnoreECs()).isFalse();
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenCodeIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new MarketArea(null, "AreaEic", true, false, false, false, null, null, false, false))
            .withMessage("MarketArea creation does not allow null attribute 'code'");
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenEicIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new MarketArea("AreaCode", null, true, false, false, false, null, null, false, false))
            .withMessage("MarketArea creation does not allow null attribute 'eic'");
    }
}
