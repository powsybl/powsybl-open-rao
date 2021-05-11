/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons.result;

import com.farao_community.farao.data.crac_api.PstRangeAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void testBasicReturns() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);
        RangeActionResultImpl rangeActionResultImpl = new RangeActionResultImpl(
                Map.of(
                        rangeAction, 200.,
                        pstRangeAction, 2.75
                )
        );

        when(pstRangeAction.computeTapPosition(2.75)).thenReturn(4);

        assertEquals(200, rangeActionResultImpl.getOptimizedSetPoint(rangeAction), DOUBLE_TOLERANCE);
        assertEquals(2.75, rangeActionResultImpl.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(4, rangeActionResultImpl.getOptimizedTap(pstRangeAction));
        assertEquals(1, rangeActionResultImpl.getOptimizedTaps().size());
        assertTrue(rangeActionResultImpl.getOptimizedTaps().containsKey(pstRangeAction));
        assertEquals(2, rangeActionResultImpl.getOptimizedSetPoints().size());
    }
}
