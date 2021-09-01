/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.state_tree;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.search_tree_rao.state_tree.StateTree;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class StateTreeTest {

    private Crac crac;
    private StateTree stateTree;
    private State preventiveState;
    private State curativeState1;
    private State curativeState2;

    private void setUpCrac(boolean withCra) {
        if (withCra) {
            crac = CommonCracCreation.createWithCurativePstRange();
        } else {
            crac = CommonCracCreation.create();

        }
        preventiveState = crac.getPreventiveState();
        curativeState1 = crac.getState("Contingency FR1 FR2", Instant.CURATIVE);
        curativeState2 = crac.getState("Contingency FR1 FR3", Instant.CURATIVE);
        stateTree = new StateTree(crac);
    }

    @Test
    public void testNoCraStartFromPreventive() {
        setUpCrac(false);
        assertEquals(0, stateTree.getContingencyScenarios().size());
        assertEquals(crac.getStates(), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(2, stateTree.getOperatorsNotSharingCras().size());
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator1"));
        assertTrue(stateTree.getOperatorsNotSharingCras().contains("operator2"));
    }

    @Test
    public void testInitFromPreventive() {
        setUpCrac(true); // PST is operated by operator1, usable after CURATIVE_STATE_2

        assertEquals(Set.of(preventiveState, curativeState1), stateTree.getBasecaseScenario().getAllStates());

        assertEquals(1, stateTree.getContingencyScenarios().size());
        ContingencyScenario contingencyScenario = stateTree.getContingencyScenarios().iterator().next();
        assertEquals(crac.getContingency("Contingency FR1 FR3"), contingencyScenario.getContingency());
        assertTrue(contingencyScenario.getAutomatonState().isEmpty());
        assertEquals(curativeState2, contingencyScenario.getCurativeState());

        assertEquals(1, stateTree.getOperatorsNotSharingCras().size());
        assertEquals("operator2", stateTree.getOperatorsNotSharingCras().iterator().next());
    }
}
