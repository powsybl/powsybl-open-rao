/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.farao_community.farao.data.crac_api.TapConvention;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
import com.farao_community.farao.data.crac_impl.TapRangeImpl;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RangeImplTest extends AbstractRemedialActionTest {

    private final double relMin = -4;
    private final double relMax = 4;
    private final double absMin = 1;
    private final double absMax = 32;

    //todo : is it a test of RangeImpl or TapRangeImpl ?
    private TapRangeImpl relativeFixedRange;
    private TapRangeImpl absoluteFixedRange;

    @Before
    public void setUp() throws Exception {
        relativeFixedRange = new TapRangeImpl(relMin, relMax, RangeType.RELATIVE_TO_INITIAL_NETWORK, TapConvention.CENTERED_ON_ZERO);
        absoluteFixedRange = new TapRangeImpl(absMin, absMax, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
    }

    @Test
    public void getMin() {
        assertEquals(relMin, relativeFixedRange.getMin(), 1e-6);
        assertEquals(absMin, absoluteFixedRange.getMin(), 1e-6);
    }

    @Test
    public void getMax() {
        assertEquals(relMax, relativeFixedRange.getMax(), 1e-6);
        assertEquals(absMax, absoluteFixedRange.getMax(), 1e-6);
    }

    @Test
    public void getRangeType() {
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, relativeFixedRange.getRangeType());
        assertEquals(RangeType.ABSOLUTE, absoluteFixedRange.getRangeType());
    }

    @Test
    public void getRangeDefinition() {
        assertEquals(TapConvention.CENTERED_ON_ZERO, relativeFixedRange.getTapConvention());
        assertEquals(TapConvention.STARTS_AT_ONE, absoluteFixedRange.getTapConvention());
    }

    @Test
    public void testEquals() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);

        assertEquals(range1, range2);
    }

    @Test
    public void testHashCode() {
        TapRangeImpl range1 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);
        TapRangeImpl range2 = new TapRangeImpl(0, 10, RangeType.ABSOLUTE, TapConvention.STARTS_AT_ONE);

        assertEquals(range1.hashCode(), range2.hashCode());
    }
}
