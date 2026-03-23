/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRangeAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
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
        HvdcRangeAction hvdcRangeAction = hvdcRangeActionAdder.newRange().withMin(-5).withMax(10).add()
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
        assertThrows(OpenRaoException.class, standardRangeAdder::add);
    }

    @Test
    void testNoMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(16);
        assertThrows(OpenRaoException.class, standardRangeAdder::add);
    }

    @Test
    void testMinGreaterThanMax() {
        StandardRangeAdder<HvdcRangeActionAdder> standardRangeAdder = hvdcRangeActionAdder.newRange().withMin(10).withMax(-5);
        assertThrows(OpenRaoException.class, standardRangeAdder::add);
    }
}
