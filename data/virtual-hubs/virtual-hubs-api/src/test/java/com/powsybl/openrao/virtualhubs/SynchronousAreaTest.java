/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class SynchronousAreaTest {
    @Test
    void checkThatSynchronousAreaIsCorrectlyCreated() {
        final Map<String, String> localLOT3Parameters = new HashMap<>();
        localLOT3Parameters.put("key", "value");
        final Voltage initialVoltage = new Voltage(new BigDecimal(4), new BigDecimal(2), 0);
        List<Voltage> initialVoltages = List.of(initialVoltage);

        final SynchronousArea synchronousArea1 = new SynchronousArea("code", "eic", true, false, null, initialVoltages);
        Assertions.assertThat(synchronousArea1.code()).isEqualTo("code");
        Assertions.assertThat(synchronousArea1.eic()).isEqualTo("eic");
        Assertions.assertThat(synchronousArea1.isLive()).isTrue();
        Assertions.assertThat(synchronousArea1.isMainArea()).isFalse();
        Assertions.assertThat(synchronousArea1.localLOT3Parameters()).isNull();
        Assertions.assertThat(synchronousArea1.initialVoltages()).isEqualTo(initialVoltages);

        final SynchronousArea synchronousArea2 = new SynchronousArea("code", "eic", false, true, localLOT3Parameters, null);
        Assertions.assertThat(synchronousArea2.code()).isEqualTo("code");
        Assertions.assertThat(synchronousArea2.eic()).isEqualTo("eic");
        Assertions.assertThat(synchronousArea2.isLive()).isFalse();
        Assertions.assertThat(synchronousArea2.isMainArea()).isTrue();
        Assertions.assertThat(synchronousArea2.localLOT3Parameters()).isEqualTo(localLOT3Parameters);
        Assertions.assertThat(synchronousArea2.initialVoltages()).isNull();
    }

    @Test
    void checkThatSynchronousAreaCreationThrowsWhenCodeIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new SynchronousArea(null, "eic", false, false, null, null))
            .withMessage("SynchronousArea creation does not allow null attribute 'code'");
    }

    @Test
    void checkThatSynchronousAreaCreationThrowsWhenEicIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new SynchronousArea("code", null, false, false, null, null))
            .withMessage("SynchronousArea creation does not allow null attribute 'eic'");
    }
}
