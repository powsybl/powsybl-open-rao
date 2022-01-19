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
import com.farao_community.farao.data.crac_api.range.RangeType;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

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
            .withOperator("operator")
            .withNetworkElement("networkElementId")
            .withInitialTap(0)
            .withTapToAngleConversionMap(Map.of(-2, -20., -1, -10., 0, 0., 1, 10., 2, 20.));
    }

    @Test
    public void testOk() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newTapRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(-5)
            .withMaxTap(10)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(-5, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(10, pstRangeAction.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.ABSOLUTE, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.TAP, pstRangeAction.getRanges().get(0).getUnit());
    }

    @Test
    public void testNoMin() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newTapRange()
            .withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK)
            .withMaxTap(16)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
        assertEquals(Integer.MIN_VALUE, pstRangeAction.getRanges().get(0).getMinTap());
        assertEquals(16, pstRangeAction.getRanges().get(0).getMaxTap());
        assertEquals(RangeType.RELATIVE_TO_INITIAL_NETWORK, pstRangeAction.getRanges().get(0).getRangeType());
        assertEquals(Unit.TAP, pstRangeAction.getRanges().get(0).getUnit());
    }

    @Test (expected = FaraoException.class)
    public void testNoRangeType() {
        pstRangeActionAdder.newTapRange()
            .withMinTap(-5)
            .withMaxTap(10)
            .add();
    }

    @Test
    public void testNoTapConventionInRelative() {
        PstRangeAction pstRangeAction = pstRangeActionAdder.newTapRange()
            .withRangeType(RangeType.RELATIVE_TO_INITIAL_NETWORK)
            .withMinTap(-5)
            .withMaxTap(10)
            .add()
            .add();

        assertEquals(1, pstRangeAction.getRanges().size());
    }

    @Test (expected = FaraoException.class)
    public void testMinGreaterThanMax() {
        pstRangeActionAdder.newTapRange()
            .withRangeType(RangeType.ABSOLUTE)
            .withMinTap(5)
            .withMaxTap(-10)
            .add();
    }
}
