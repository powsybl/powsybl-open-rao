/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class RegionTest {
    @Test
    void checkThatRegionIsCorrectlyCreated() {
        final Region region = new Region("RegionCode", "RegionEic");
        Assertions.assertThat(region.code()).isEqualTo("RegionCode");
        Assertions.assertThat(region.eic()).isEqualTo("RegionEic");
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenCodeIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Region(null, "RegionEic"))
            .withMessage("Region creation does not allow null attribute 'code'");
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenEicIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Region("RegionCode", null))
            .withMessage("Region creation does not allow null attribute 'eic'");
    }
}
