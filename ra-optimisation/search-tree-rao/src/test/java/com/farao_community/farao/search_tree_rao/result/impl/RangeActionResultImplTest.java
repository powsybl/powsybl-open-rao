/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RangeActionResultImplTest {
    private static final double DOUBLE_TOLERANCE = 0.01;

    private RangeAction rangeAction = Mockito.mock(RangeAction.class);
    private PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);

    @Before
    public void setUp() {
        rangeAction = Mockito.mock(RangeAction.class);
        pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(pstRangeAction.convertAngleToTap(2.75)).thenReturn(4);
    }

    @Test
    public void testInitWithMap() {
        RangeActionResultImpl rangeActionResultImpl = new RangeActionResultImpl(
                Map.of(
                        rangeAction, 200.,
                        pstRangeAction, 2.75
                )
        );
        checkContents(rangeActionResultImpl);
    }

    @Test
    public void testInitWithNetwork() {
        Network network = Mockito.mock(Network.class);
        when(rangeAction.getCurrentSetpoint(network)).thenReturn(200.);
        when(pstRangeAction.getCurrentSetpoint(network)).thenReturn(2.75);

        RangeActionResultImpl rangeActionResultImpl = new RangeActionResultImpl(network, Set.of(rangeAction, pstRangeAction));
        checkContents(rangeActionResultImpl);
    }

    private void checkContents(RangeActionResultImpl rangeActionResultImpl) {
        assertEquals(200, rangeActionResultImpl.getOptimizedSetPoint(rangeAction), DOUBLE_TOLERANCE);
        assertEquals(2.75, rangeActionResultImpl.getOptimizedSetPoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(4, rangeActionResultImpl.getOptimizedTap(pstRangeAction));
        assertEquals(1, rangeActionResultImpl.getOptimizedTaps().size());
        assertTrue(rangeActionResultImpl.getOptimizedTaps().containsKey(pstRangeAction));
        assertEquals(2, rangeActionResultImpl.getOptimizedSetPoints().size());
        assertEquals(Set.of(rangeAction, pstRangeAction), rangeActionResultImpl.getRangeActions());
        assertThrows(FaraoException.class, () -> rangeActionResultImpl.getOptimizedTap(Mockito.mock(PstRangeAction.class)));
        assertThrows(FaraoException.class, () -> rangeActionResultImpl.getOptimizedSetPoint(Mockito.mock(RangeAction.class)));
    }
}
