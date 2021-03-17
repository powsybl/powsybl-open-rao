/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class StateTreeTest {

    private SimpleCrac crac;
    private Network network;
    private StateTree stateTree;
    private State preventiveState;
    private State curativeState1;
    private State curativeState2;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    private void setUpCrac(boolean withCra) {
        if (withCra) {
            crac = CommonCracCreation.createWithCurativePstRange();
        } else {
            crac = CommonCracCreation.create();

        }
        preventiveState = crac.getPreventiveState();
        curativeState1 = crac.getState("Contingency FR1 FR2-curative");
        curativeState2 = crac.getState("Contingency FR1 FR3-curative");
        stateTree = new StateTree(crac, network, preventiveState);
    }

    @Test
    public void testNoCraStartFromPreventive() {
        setUpCrac(false);
        assertEquals(1, stateTree.getOptimizedStates().size());
        assertEquals(preventiveState, stateTree.getOptimizedStates().iterator().next());
        for (State state : crac.getStates()) {
            assertEquals(preventiveState, stateTree.getOptimizedState(state));
        }
        assertEquals(crac.getStates(), stateTree.getPerimeter(preventiveState));

        assertEquals(2, stateTree.getOperatorsNotSharingCras().size());
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator1"));
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator2"));
    }

    @Test
    public void testGetPerimeterOnStateNotOptimized() {
        setUpCrac(false);
        assertNull(stateTree.getPerimeter(curativeState1));
    }

    @Test(expected = NotImplementedException.class)
    public void testStartFromCurativeError() {
        crac = CommonCracCreation.create();
        stateTree = new StateTree(crac, network, crac.getState("Contingency FR1 FR3-curative"));
    }

    @Test
    public void testInitFromPreventive() {
        setUpCrac(true); // PST is operated by operator1, usable after CURATIVE_STATE_2

        assertEquals(2, stateTree.getOptimizedStates().size());
        assertTrue(stateTree.getOptimizedStates().contains(preventiveState));
        assertTrue(stateTree.getOptimizedStates().contains(curativeState2));

        assertEquals(preventiveState, stateTree.getOptimizedState(preventiveState));
        assertEquals(preventiveState, stateTree.getOptimizedState(curativeState1));
        assertEquals(curativeState2, stateTree.getOptimizedState(curativeState2));

        assertEquals(Set.of(preventiveState, curativeState1), stateTree.getPerimeter(preventiveState));
        assertEquals(Set.of(curativeState2), stateTree.getPerimeter(curativeState2));

        assertEquals(1, stateTree.getOperatorsNotSharingCras().size());
        assertEquals("operator2", stateTree.getOperatorsNotSharingCras().iterator().next());
    }
}
