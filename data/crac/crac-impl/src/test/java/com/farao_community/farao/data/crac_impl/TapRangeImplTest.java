/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_impl.TapRangeImpl;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class TapRangeImplTest {

    private final int relMin = -4;
    private final int relMax = 4;
    private final int absMin = 1;
    private final int absMax = 32;

    private TapRangeImpl relativeFixedRange;
    private TapRangeImpl absoluteFixedRange;

    @Before
    public void setUp() {
        relativeFixedRange = new TapRangeImpl(relMin, relMax, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.CENTERED_ON_ZERO);
        absoluteFixedRange = new TapRangeImpl(absMin, absMax, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
    }

    @Test
    public void getMinTest() {
        assertEquals(relMin, relativeFixedRange.getMinTap(), 1e-6);
        assertEquals(absMin, absoluteFixedRange.getMinTap(), 1e-6);
    }

    @Test
    public void getMaxTest() {
        assertEquals(relMax, relativeFixedRange.getMaxTap(), 1e-6);
        assertEquals(absMax, absoluteFixedRange.getMaxTap(), 1e-6);
    }

    @Test
    public void getRangeTypeTest() {
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, relativeFixedRange.getRangeType());
        assertEquals(RangeType.ABSOLUTE, absoluteFixedRange.getRangeType());
    }

    @Test
    public void getTapConventionTest() {
        assertEquals(TapConvention.CENTERED_ON_ZERO, relativeFixedRange.getTapConvention());
        assertEquals(TapConvention.STARTS_AT_ONE, absoluteFixedRange.getTapConvention());
    }

    @Test
    public void testEquals() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range3 = new TapRangeImpl(0, 11, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range4 = new TapRangeImpl(0, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range5 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.CENTERED_ON_ZERO);

        assertEquals(range1, range2);
        assertNotEquals(range1, range3);
        assertNotEquals(range1, range4);
        assertNotEquals(range1, range5);

    }

    @Test
    public void testHashCode() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range3 = new TapRangeImpl(0, 11, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range4 = new TapRangeImpl(0, 10, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range5 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.CENTERED_ON_ZERO);

        assertEquals(range1.hashCode(), range2.hashCode());
        assertNotEquals(range1.hashCode(), range3.hashCode());
        assertNotEquals(range1.hashCode(), range4.hashCode());
        assertNotEquals(range1.hashCode(), range5.hashCode());
    }
}
