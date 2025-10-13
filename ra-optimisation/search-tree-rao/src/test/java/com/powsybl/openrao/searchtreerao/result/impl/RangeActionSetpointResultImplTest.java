/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.impl;

import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionSetpointResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class RangeActionSetpointResultImplTest {
    private static final double DOUBLE_TOLERANCE = 1e-6;

    private RangeAction<?> rangeAction = Mockito.mock(RangeAction.class);
    private PstRangeAction pstRangeAction = Mockito.mock(PstRangeAction.class);

    @BeforeEach
    public void setUp() {
        rangeAction = Mockito.mock(RangeAction.class);
        pstRangeAction = Mockito.mock(PstRangeAction.class);
        when(pstRangeAction.convertAngleToTap(2.75)).thenReturn(4);
    }

    @Test
    void testInitWithMap() {
        RangeActionSetpointResultImpl rangeActionSetpointResult = new RangeActionSetpointResultImpl(
                Map.of(rangeAction, 200.,
                    pstRangeAction, 2.75));
        checkContents(rangeActionSetpointResult);
    }

    @Test
    void testInitWithNetwork() {
        Network network = Mockito.mock(Network.class);
        when(rangeAction.getCurrentSetpoint(network)).thenReturn(200.);
        when(pstRangeAction.getCurrentSetpoint(network)).thenReturn(2.75);

        RangeActionSetpointResult rangeActionSetpointResult = RangeActionSetpointResultImpl.buildWithSetpointsFromNetwork(network, Set.of(rangeAction, pstRangeAction));
        checkContents(rangeActionSetpointResult);
    }

    @Test
    void testInitWithRangeActionActivationResult() {
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
