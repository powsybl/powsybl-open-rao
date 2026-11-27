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
    private static final String POLE_ID = "ID_OF_POLE";
    private static final String POLE_EIC = "EIC_OF_POLE";

    @Test
    void checkThatPoleCreationThrowsWhenIdIsNull() {
        final List<HvdcConverter> converters = List.of();
        final List<HvdcLine> lines = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new HvdcPole(null, POLE_EIC, converters, lines))
                .withMessage("HvdcPole configuration does not allow adding null attribute 'id'");
    }

    @Test
    void checkThatPoleCreationThrowsWhenEicIsNull() {
        final List<HvdcConverter> converters = List.of();
        final List<HvdcLine> lines = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new HvdcPole(POLE_ID, null, converters, lines))
                .withMessage("HvdcPole configuration does not allow adding null attribute 'eic'");
    }

    @Test
    void checkThatPoleCreationThrowsWhenConvertersIsNull() {
        final List<HvdcLine> lines = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new HvdcPole(POLE_ID, POLE_EIC, null, lines))
                .withMessage("HvdcPole configuration does not allow adding null attribute 'converters'");
    }

    @Test
    void checkThatPoleCreationThrowsWhenLinesIsNull() {
        final List<HvdcConverter> converters = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new HvdcPole(POLE_ID, POLE_EIC, converters, null))
                .withMessage("HvdcPole configuration does not allow adding null attribute 'lines'");
    }

    @Test
    void createPoleWithEmptyLists() {
        final HvdcPole pole = new HvdcPole(POLE_ID, POLE_EIC, List.of(), List.of());
        Assertions.assertThat(pole).isNotNull();
        Assertions.assertThat(pole.converters()).isEmpty();
        Assertions.assertThat(pole.lines()).isEmpty();
    }

    @Test
    void createPole() {
        final HvdcConverter hvdcConverter = new HvdcConverter("node value", "station value");
        final List<HvdcConverter> converters = List.of(hvdcConverter);
        final HvdcLine hvdcLine = new HvdcLine("id", "from node", "to node");
        final List<HvdcLine> lines = List.of(hvdcLine);
        final HvdcPole pole = new HvdcPole(POLE_ID, POLE_EIC, converters, lines);
        Assertions.assertThat(pole).isNotNull();
        Assertions.assertThat(pole.id()).isEqualTo(POLE_ID);
        Assertions.assertThat(pole.eic()).isEqualTo(POLE_EIC);
        Assertions.assertThat(pole.converters()).containsExactly(hvdcConverter);
        Assertions.assertThat(pole.lines()).containsExactly(hvdcLine);
    }
}
