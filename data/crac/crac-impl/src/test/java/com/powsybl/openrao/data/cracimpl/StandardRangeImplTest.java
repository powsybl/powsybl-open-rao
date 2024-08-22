/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.data.cracapi.range.RangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class StandardRangeImplTest {

    private final double min = 1;
    private final double max = 32;

    private StandardRangeImpl fixedRange;

    @BeforeEach
    public void setUp() {
        fixedRange = new StandardRangeImpl(min, max, RangeType.ABSOLUTE);
    }

    @Test
    void getMinTest() {
        assertEquals(min, fixedRange.getMin(), 1e-6);
    }

    @Test
    void getMaxTest() {
        assertEquals(max, fixedRange.getMax(), 1e-6);
    }

    @Test
    void getRangeTypeTest() {
        assertEquals(RangeType.ABSOLUTE, fixedRange.getRangeType());
    }

    @Test
    void testEquals() {
        StandardRangeImpl range1 = new StandardRangeImpl(0, 10, RangeType.ABSOLUTE);
        StandardRangeImpl range2 = new StandardRangeImpl(0, 10, RangeType.ABSOLUTE);
        StandardRangeImpl range3 = new StandardRangeImpl(0, 11, RangeType.ABSOLUTE);

        assertEquals(range1, range2);
        assertNotEquals(range1, range3);
    }

    @Test
    void testHashCode() {
        StandardRangeImpl range1 = new StandardRangeImpl(0, 10, RangeType.ABSOLUTE);
        StandardRangeImpl range2 = new StandardRangeImpl(0, 10, RangeType.ABSOLUTE);
        StandardRangeImpl range3 = new StandardRangeImpl(0, 11, RangeType.ABSOLUTE);

        assertEquals(range1.hashCode(), range2.hashCode());
        assertNotEquals(range1.hashCode(), range3.hashCode());
    }
}
