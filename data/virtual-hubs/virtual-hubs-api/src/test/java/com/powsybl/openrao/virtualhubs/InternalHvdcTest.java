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
class InternalHvdcTest {
    @Test
    void checkThatInternalHvdcCreationThrowsWhenConvertersIsNull() {
        final List<HvdcLine> lines = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new InternalHvdc(null, lines))
            .withMessage("Virtual hubs configuration does not allow adding null hvdc converters");
    }

    @Test
    void checkThatInternalHvdcCreationThrowsWhenLinesIsNull() {
        final List<HvdcConverter> converters = List.of();
        Assertions.assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new InternalHvdc(converters, null))
            .withMessage("Virtual hubs configuration does not allow adding null hvdc lines");
    }

    @Test
    void createInternalHvdcWithEmptyLists() {
        final InternalHvdc internalHvdc = new InternalHvdc(List.of(), List.of());
        Assertions.assertThat(internalHvdc).isNotNull();
        Assertions.assertThat(internalHvdc.converters()).isEmpty();
        Assertions.assertThat(internalHvdc.lines()).isEmpty();
    }

    @Test
    void createInternalHvdc() {
        final HvdcConverter hvdcConverter = new HvdcConverter("node value", "station value");
        final List<HvdcConverter> converters = List.of(hvdcConverter);
        final HvdcLine hvdcLine = new HvdcLine("from node", "to node");
        final List<HvdcLine> lines = List.of(hvdcLine);
        final InternalHvdc internalHvdc = new InternalHvdc(converters, lines);
        Assertions.assertThat(internalHvdc).isNotNull();
        Assertions.assertThat(internalHvdc.converters()).containsExactly(hvdcConverter);
        Assertions.assertThat(internalHvdc.lines()).containsExactly(hvdcLine);
    }
}
