/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.virtualhubs;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class HvdcPoleTest {
    private static final String POLE_ID = "EIC_OF_POLE";

    @Test
    void checkThatPoleCreationThrowsWhenConvertersIsNull() {
        final List<HvdcLine> lines = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new HvdcPole(POLE_ID, null, lines))
            .withMessage("Virtual hubs configuration does not allow adding null hvdc converters");
    }

    @Test
    void checkThatPoleCreationThrowsWhenLinesIsNull() {
        final List<HvdcConverter> converters = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new HvdcPole(POLE_ID, converters, null))
            .withMessage("Virtual hubs configuration does not allow adding null hvdc lines");
    }

    @Test
    void createPoleWithEmptyLists() {
        final HvdcPole pole = new HvdcPole(POLE_ID, List.of(), List.of());
        Assertions.assertThat(pole).isNotNull();
        Assertions.assertThat(pole.converters()).isEmpty();
        Assertions.assertThat(pole.lines()).isEmpty();
    }

    @Test
    void createPole() {
        final HvdcConverter hvdcConverter = new HvdcConverter("node value", "station value");
        final List<HvdcConverter> converters = List.of(hvdcConverter);
        final HvdcLine hvdcLine = new HvdcLine("from node", "to node");
        final List<HvdcLine> lines = List.of(hvdcLine);
        final HvdcPole pole = new HvdcPole(POLE_ID, converters, lines);
        Assertions.assertThat(pole).isNotNull();
        Assertions.assertThat(pole.converters()).containsExactly(hvdcConverter);
        Assertions.assertThat(pole.lines()).containsExactly(hvdcLine);
    }
}
