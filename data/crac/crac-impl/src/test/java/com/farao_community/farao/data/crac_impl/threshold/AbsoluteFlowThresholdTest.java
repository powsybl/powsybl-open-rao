/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.Unit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class AbsoluteFlowThresholdTest {

    private AbsoluteFlowThreshold absoluteFlowThreshold;

    @Before
    public void setUp() throws Exception {
        absoluteFlowThreshold = new AbsoluteFlowThreshold(
                Unit.AMPERE,
                Side.LEFT,
                Direction.BOTH,
                1000.0
        );
    }

    @Test
    public void setMargin() {
        try {
            absoluteFlowThreshold.setMargin(2.0, Unit.AMPERE);
            fail();
        } catch (FaraoException e) {
            // should throw because the frm can only be defined in Megawatt
        }
        assertEquals(0.0, absoluteFlowThreshold.frmInMW, 0.0);
        double targetFrmInMw = 2.0;
        absoluteFlowThreshold.setMargin(targetFrmInMw, Unit.MEGAWATT);
        assertEquals(targetFrmInMw, absoluteFlowThreshold.frmInMW, 0.0);
    }
}
