/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.cne.core;

import com.powsybl.openrao.data.raoresult.io.cne.core.xsd.ESMPDateTimeInterval;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CoreCneUtilTest {
    @Test
    void testInterval() {
        OffsetDateTime odt = OffsetDateTime.parse("2021-10-30T22:00:00Z");
        ESMPDateTimeInterval interval = CoreCneUtil.createEsmpDateTimeInterval(odt);
        assertEquals("2021-10-30T22:00Z", interval.getStart());
        assertEquals("2021-10-30T23:00Z", interval.getEnd());

        odt = OffsetDateTime.parse("2021-03-28T10:00:00Z");
        interval = CoreCneUtil.createEsmpDateTimeInterval(odt);
        assertEquals("2021-03-28T10:00Z", interval.getStart());
        assertEquals("2021-03-28T11:00Z", interval.getEnd());
    }

    @Test
    void testIntervalWholeDay() {
        ESMPDateTimeInterval interval = CoreCneUtil.createEsmpDateTimeIntervalForWholeDay("2021-10-30T22:00Z/2021-10-31T23:00Z");
        assertEquals("2021-10-30T22:00Z", interval.getStart());
        assertEquals("2021-10-31T23:00Z", interval.getEnd());
    }
}
