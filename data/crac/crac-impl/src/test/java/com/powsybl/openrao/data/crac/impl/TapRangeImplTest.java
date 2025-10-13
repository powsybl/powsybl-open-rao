/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.data.crac.api.range.RangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class TapRangeImplTest {

    private final int relMin = -4;
    private final int relMax = 4;
    private final int absMin = 1;
    private final int absMax = 32;

    private TapRangeImpl relativeFixedRange;
    private TapRangeImpl absoluteFixedRange;

    @BeforeEach
    public void setUp() {
        relativeFixedRange = new TapRangeImpl(relMin, relMax, RangeType.RELATIVE_TO_INITIAL_NETWORK);
        absoluteFixedRange = new TapRangeImpl(absMin, absMax, RangeType.ABSOLUTE);
    }

    @Test
    void getMinTest() {
        assertEquals(relMin, relativeFixedRange.getMinTap(), 1e-6);
        assertEquals(absMin, absoluteFixedRange.getMinTap(), 1e-6);
    }

    @Test
    void getMaxTest() {
        assertEquals(relMax, relativeFixedRange.getMaxTap(), 1e-6);
        assertEquals(absMax, absoluteFixedRange.getMaxTap(), 1e-6);
    }

    @Test
    void getRangeTypeTest() {
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, relativeFixedRange.getRangeType());
        assertEquals(RangeType.ABSOLUTE, absoluteFixedRange.getRangeType());
    }

    @Test
    void testEquals() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE);
        TapRangeImpl range3 = new TapRangeImpl(0, 11, RangeType.ABSOLUTE);
        TapRangeImpl range4 = new TapRangeImpl(0, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK);

        assertEquals(range1, range2);
        assertNotEquals(range1, range3);
        assertNotEquals(range1, range4);
    }

    @Test
    void testHashCode() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE);
        TapRangeImpl range3 = new TapRangeImpl(0, 11, RangeType.ABSOLUTE);
        TapRangeImpl range4 = new TapRangeImpl(0, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK);

        assertEquals(range1.hashCode(), range2.hashCode());
        assertNotEquals(range1.hashCode(), range3.hashCode());
        assertNotEquals(range1.hashCode(), range4.hashCode());
    }
}
