/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.List;

class InternalHvdcTest {
    private static final String HVDC_EIC = "EIC_OF_HVDC";
    private static final String HVDC_CODE = "CODE_OF_HVDC";
    private static final String POLE_ID = "ID_OF_POLE";
    private static final String POLE_EIC = "EIC_OF_POLE";
    private static final MarketArea MARKET_AREA = new MarketArea("code", "eic", false, false, false, false, null, null, false, false);

    private static final List<ThrowableAssert.ThrowingCallable> FAILING_CONSTRUCTORS = List.of(
        () -> new InternalHvdc(null, HVDC_CODE, createPoles(), MARKET_AREA, false, false),
        () -> new InternalHvdc(HVDC_EIC, null, createPoles(), MARKET_AREA, false, false),
        () -> new InternalHvdc(HVDC_EIC, HVDC_CODE, null, MARKET_AREA, false, false)
    // () -> new InternalHvdc(HVDC_EIC, HVDC_CODE, createPoles(), null, false, false) // TODO Add this when marketArea becomes mandatory
    );

    @ParameterizedTest
    @FieldSource("FAILING_CONSTRUCTORS")
    void checkThatCreationThrowsWhenNullArg(final ThrowableAssert.ThrowingCallable callable) {
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(callable);
    }

    @Test
    void createWithEmptyPoles() {
        final InternalHvdc hvdc = new InternalHvdc(HVDC_EIC, HVDC_CODE, List.of(), MARKET_AREA, false, false);

        Assertions.assertThat(hvdc).isNotNull();
        Assertions.assertThat(hvdc.code()).isEqualTo(HVDC_CODE);
        Assertions.assertThat(hvdc.eic()).isEqualTo(HVDC_EIC);
        Assertions.assertThat(hvdc.poles()).isEmpty();
    }

    @Test
    void createInternalHvdc() {
        final InternalHvdc hvdc = new InternalHvdc(HVDC_EIC, HVDC_CODE, createPoles(), MARKET_AREA, false, false);

        Assertions.assertThat(hvdc).isNotNull();
        Assertions.assertThat(hvdc.code()).isEqualTo(HVDC_CODE);
        Assertions.assertThat(hvdc.eic()).isEqualTo(HVDC_EIC);
        Assertions.assertThat(hvdc.relatedMa()).isEqualTo(MARKET_AREA);
        Assertions.assertThat(hvdc.poles()).hasSize(1);
    }

    private static List<HvdcPole> createPoles() {
        final HvdcPole pole = new HvdcPole(
            POLE_ID,
            POLE_EIC,
            List.of(new HvdcConverter("node value", "station value")),
            List.of(new HvdcLine("id", "from node", "to node"))
        );

        return List.of(pole);
    }
}
