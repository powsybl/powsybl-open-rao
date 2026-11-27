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

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class VoltageTest {
    private static final BigDecimal BIG_D_2 = new BigDecimal(2);
    private static final BigDecimal BIG_D_4 = new BigDecimal(4);

    @Test
    void checkThatVoltageIsCorrectlyCreated() {
        final Voltage voltage = new Voltage(BIG_D_4, BIG_D_2, 6);
        Assertions.assertThat(voltage.nominal()).isEqualTo(BIG_D_4);
        Assertions.assertThat(voltage.initial()).isEqualTo(BIG_D_2);
        Assertions.assertThat(voltage.slackSearchPriority()).isEqualTo(6);
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenNominalIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Voltage(null, BIG_D_2, 0))
            .withMessage("Voltage creation does not allow null attribute 'nominal'");
    }

    @Test
    void checkThatMarketAreaCreationThrowsWhenInitialIsNull() {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Voltage(BIG_D_4, null, 0))
            .withMessage("Voltage creation does not allow null attribute 'initial'");
    }
}
