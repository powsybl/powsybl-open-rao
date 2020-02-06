/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.farao_community.farao.data.crac_api.RangeDefinition;
import com.farao_community.farao.data.crac_impl.AbstractRemedialActionTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class RangeTest extends AbstractRemedialActionTest {

    private final double relMin = -4;
    private final double relMax = 4;
    private final double absMin = 1;
    private final double absMax = 32;

    private Range relativeFixedRange;
    private Range absoluteFixedRange;

    @Before
    public void setUp() throws Exception {
        relativeFixedRange = new Range(relMin, relMax, RangeType.RELATIVE_FIXED, RangeDefinition.CENTERED_ON_ZERO);
        absoluteFixedRange = new Range(absMin, absMax, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);
    }

    @Test
    public void getMin() {
        assertEquals(relMin, relativeFixedRange.getMin(), 0.);
        assertEquals(absMin, absoluteFixedRange.getMin(), 0.);
    }

    @Test
    public void getMax() {
        assertEquals(relMax, relativeFixedRange.getMax(), 0.);
        assertEquals(absMax, absoluteFixedRange.getMax(), 0.);
    }

    @Test
    public void getRangeType() {
        assertEquals(RangeType.RELATIVE_FIXED, relativeFixedRange.getRangeType());
        assertEquals(RangeType.ABSOLUTE_FIXED, absoluteFixedRange.getRangeType());
    }

    @Test
    public void getRangeDefinition() {
        assertEquals(RangeDefinition.CENTERED_ON_ZERO, relativeFixedRange.getRangeDefinition());
        assertEquals(RangeDefinition.STARTS_AT_ONE, absoluteFixedRange.getRangeDefinition());
    }

    @Test
    public void testEquals() {
        Range range1 = new Range(0, 10, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);
        Range range2 = new Range(0, 10, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);

        assertEquals(range1, range2);
    }

    @Test
    public void testHashCode() {
        Range range1 = new Range(0, 10, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);
        Range range2 = new Range(0, 10, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE);

        assertEquals(range1.hashCode(), range2.hashCode());
    }
}
