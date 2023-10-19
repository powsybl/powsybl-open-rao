/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range.StandardRangeAdder;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
class StandardRangeAdderImplTest {
    private HvdcRangeActionAdder hvdcRangeActionAdder;

    @BeforeEach
    public void setUp() {
        Crac crac = new CracImplFactory().create("cracId");
        hvdcRangeActionAdder = crac.newHvdcRangeAction()
            .withId("hvdcRangeActionId")
            .withName("hvdcRangeActionName")
            .withOperator("operator")
            .withNetworkElement("networkElementId");
    }

    @Test
    void testOk() {
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
            .add();

        assertEquals(1, hvdcRangeAction.getRanges().size());
        assertEquals(-5, hvdcRangeAction.getRanges().get(0).getMin(), 1e-6);
        assertEquals(10, hvdcRangeAction.getRanges().get(0).getMax(), 1e-6);
        assertEquals(RangeType.ABSOLUTE, hvdcRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.MEGAWATT, hvdcRangeAction.getRanges().get(0).getUnit());
    }

    @Test
    void testNoMin() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMax(16);
        FaraoException exception = assertThrows(FaraoException.class, standardRangeAdder::add);
        assertEquals("StandardRange min value was not defined.", exception.getMessage());
    }

    @Test
    void testNoMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(16);
        FaraoException exception = assertThrows(FaraoException.class, standardRangeAdder::add);
        assertEquals("StandardRange max value was not defined.", exception.getMessage());
    }

    @Test
    void testMinGreaterThanMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(10).withMax(-5);
        FaraoException exception = assertThrows(FaraoException.class, standardRangeAdder::add);
        assertEquals("Max value of StandardRange must be equal or greater than min value.", exception.getMessage());
    }
}
