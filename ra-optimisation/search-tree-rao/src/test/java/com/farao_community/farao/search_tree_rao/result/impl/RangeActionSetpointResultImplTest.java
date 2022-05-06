/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.result.impl;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionActivationResult;
import com.farao_community.farao.search_tree_rao.result.api.RangeActionSetpointResult;
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
public class RangeActionSetpointResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
    private PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);

    @Before
    public void setUp() {
        rangeAction = Mockito.mock(RangeAction.class);
        pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(pstRangeAction.convertAngleToTap(2.75)).thenReturn(4);
    }

    @Test
    public void testInitWithMap() {
        RangeActionSetpointResultImpl rangeActionSetpointResult = new RangeActionSetpointResultImpl(
                Map.of(rangeAction, 200.,
                    pstRangeAction, 2.75));
        checkContents(rangeActionSetpointResult);
    }

    @Test
    public void testInitWithNetwork() {
        Network network = Mockito.mock(Network.class);
        when(rangeAction.getCurrentSetpoint(network)).thenReturn(200.);
        when(pstRangeAction.getCurrentSetpoint(network)).thenReturn(2.75);

        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, Set.of(rangeAction, pstRangeAction));
        checkContents(rangeActionSetpointResult);
    }

    @Test
    public void testInitWithRangeActionActivationResult() {
        RangeActionActivationResult raar = Mockito.mock(RangeActionActivationResult.class);
        State anyState = Mockito.mock(State.class);

        when(raar.getRangeActions()).thenReturn(Set.of(rangeAction, pstRangeAction));
        when(raar.getOptimizedSetpoint(rangeAction, anyState)).thenReturn(200.);
        when(raar.getOptimizedSetpoint(pstRangeAction, anyState)).thenReturn(2.75);

        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildFromActivationOfRangeActionAtState(raar, anyState);
        checkContents(rangeActionSetpointResult);
    }

    private void checkContents(RangeActionSetpointResult rangeActionResult) {
        assertEquals(200, rangeActionResult.getSetpoint(rangeAction), DOUBLE_TOLERANCE);
        assertEquals(2.75, rangeActionResult.getSetpoint(pstRangeAction), DOUBLE_TOLERANCE);
        assertEquals(4, rangeActionResult.getTap(pstRangeAction));
        assertEquals(Set.of(rangeAction, pstRangeAction), rangeActionResult.getRangeActions());
    }
}
