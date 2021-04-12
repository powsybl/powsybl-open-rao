/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class TapRangeAdderImplTest {
    private PstRangeActionAdder pstRangeActionAdder;

    @Before
    public void setUp() {
        Crac crac = new SimpleCracFactory().create("cracId");
        pstRangeActionAdder = crac.newPstRangeAction()
            .withId("pstRangeActionId")
            .withName("pstRangeActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
        assertTrue(pstRangeAction.getRanges().get(0) instanceof TapRange);
        assertEquals(-5., pstRangeAction.getRanges().get(0).getMin(), 1e-3);
        assertEquals(10., pstRangeAction.getRanges().get(0).getMax(), 1e-3);
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.TAP, pstRangeAction.getRanges().get(0).getUnit());
        assertEquals(TapConvention.CENTERED_ON_ZERO, ((TapRange) pstRangeAction.getRanges().get(0)).getTapConvention());
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeType() {
        pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeDefinition() {
        pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoUnit() {
        pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testMinGreaterThanMax() {
        pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNonIntegerTaps() {
        pstRangeActionAdder.newPstRange()
            .withUnit(Unit.TAP)
            .withRangeType(RangeType.ABSOLUTE)
            .withRangeDefinition(TapConvention.CENTERED_ON_ZERO)
            .withMin(-5.)
            .withMax(10.)
            .add();
    }
}
