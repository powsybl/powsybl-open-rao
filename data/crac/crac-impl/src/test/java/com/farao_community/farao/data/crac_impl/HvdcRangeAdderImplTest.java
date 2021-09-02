/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class HvdcRangeAdderImplTest {
    private HvdcRangeActionAdder hvdcRangeActionAdder;

    @Before
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId");
        hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("hvdcRangeActionId")
            .withName("hvdcRangeActionName")
            .withOperator("operator")
            .withNetworkElement("networkElementId");
    }

    @Test
    public void testOk() {
        HvdcRangeAction hvdcRangeAction = hvdcRangeActionAdder.newHvdcRange()
                .withMin(-5)
                .withMax(10)
                .add()
                .add();

        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(-5, hvdcRangeAction.getRanges().get(0).getMin(), 1e-6);
        assertEquals(10, hvdcRangeAction.getRanges().get(0).getMax(), 1e-6);
        assertEquals(RangeType.ABSOLUTE, hvdcRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.MEGAWATT, hvdcRangeAction.getRanges().get(0).getUnit());
    }

    @Test
    public void testNoMin() {
        HvdcRangeAction hvdcRangeAction = hvdcRangeActionAdder.newHvdcRange()
            .withMax(16)
            .add()
            .add();

        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(Double.MIN_VALUE, hvdcRangeAction.getRanges().get(0).getMin(), 1e-6);
        assertEquals(16, hvdcRangeAction.getRanges().get(0).getMax(), 1e-6);
        assertEquals(RangeType.ABSOLUTE, hvdcRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.MEGAWATT, hvdcRangeAction.getRanges().get(0).getUnit());
    }

    @Test
    public void testNoMax() {
        HvdcRangeAction hvdcRangeAction = hvdcRangeActionAdder.newHvdcRange()
                .withMin(16)
                .add()
                .add();

        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(Double.MAX_VALUE, hvdcRangeAction.getRanges().get(0).getMax(), 1e-6);
        assertEquals(16, hvdcRangeAction.getRanges().get(0).getMin(), 1e-6);
        assertEquals(RangeType.ABSOLUTE, hvdcRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.MEGAWATT, hvdcRangeAction.getRanges().get(0).getUnit());
    }

    @Test (expected = FaraoException.class)
    public void testMinGreaterThanMax() {
        hvdcRangeActionAdder.newHvdcRange()
            .withMin(5)
            .withMax(-10)
            .add();
    }
}
