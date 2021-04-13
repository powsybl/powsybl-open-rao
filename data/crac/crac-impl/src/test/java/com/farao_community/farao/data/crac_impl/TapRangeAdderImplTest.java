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
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.range_action.RangeType;
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
        Crac crac = new CracImplFactory().create("cracId");
        pstRangeActionAdder = crac.newPstRangeAction()
            .withId("pstRangeActionId")
            .withName("pstRangeActionName")
            .withOperator("operator");
    }

    @Test
    public void testOk() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newPstRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withTapConvention(TapConvention.CENTERED_ON_ZERO)
            .withMinTap(-5)
            .withMaxTap(10)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(-5, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.TAP, pstRangeAction.getRanges().get(0).getUnit());
        assertEquals(TapConvention.CENTERED_ON_ZERO, (pstRangeAction.getRanges().get(0)).getTapConvention());
    }

    @Test
    public void testNoMin() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newPstRange()
            .withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK)
            .withTapConvention(TapConvention.STARTS_AT_ONE)
            .withMaxTap(16)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(Integer.MIN_VALUE, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(16, pstRangeAction.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.TAP, pstRangeAction.getRanges().get(0).getUnit());
        assertEquals(TapConvention.STARTS_AT_ONE, (pstRangeAction.getRanges().get(0)).getTapConvention());
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeType() {
        pstRangeActionAdder.newPstRange()
            .withTapConvention(TapConvention.CENTERED_ON_ZERO)
            .withMinTap(-5)
            .withMaxTap(10)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testNoTypeConvention() {
        pstRangeActionAdder.newPstRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(-5)
            .withMaxTap(10)
            .add();
    }

    @Test (expected = FaraoException.class)
    public void testMinGreaterThanMax() {
        pstRangeActionAdder.newPstRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withTapConvention(TapConvention.CENTERED_ON_ZERO)
            .withMinTap(5)
            .withMaxTap(-10)
            .add();
    }
}
