/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.utils.ExhaustiveCracCreation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class RangeActionLimitationParametersTest {

    private State state0;
    private State state1;
    private State state2;

    @BeforeEach
    public void setUp() {
        Crac crac = ExhaustiveCracCreation.create();
        state0 = crac.getPreventiveState();
        state1 = crac.getState("contingency1Id", crac.getInstant(Instant.Kind.CURATIVE));
        state2 = crac.getState("contingency2Id", crac.getInstant(Instant.Kind.CURATIVE));
    }

    @Test
    void testGetterAndSetters() {

        RangeActionLimitationParameters ralp = new RangeActionLimitationParameters();

        ralp.setMaxRangeAction(state1, 2);
        ralp.setMaxRangeAction(state2, 4);
        ralp.setMaxTso(state2, 1);
        ralp.setMaxTsoExclusion(state2, Set.of("DE"));
        ralp.setMaxPstPerTso(state1, Map.of("BE", 1));
        ralp.setMaxRangeActionPerTso(state1, Map.of("FR", 3));

        assertNull(ralp.getMaxRangeActions(state0));
        assertEquals(Integer.valueOf(2), ralp.getMaxRangeActions(state1));
        assertEquals(Integer.valueOf(4), ralp.getMaxRangeActions(state2));

        assertNull(ralp.getMaxTso(state0));
        assertNull(ralp.getMaxTso(state1));
        assertEquals(Integer.valueOf(1), ralp.getMaxTso(state2));

        assertTrue(ralp.getMaxTsoExclusion(state0).isEmpty());
        assertTrue(ralp.getMaxTsoExclusion(state1).isEmpty());
        assertEquals(Set.of("DE"), ralp.getMaxTsoExclusion(state2));

        assertTrue(ralp.getMaxPstPerTso(state0).isEmpty());
        assertEquals(Map.of("BE", 1), ralp.getMaxPstPerTso(state1));
        assertTrue(ralp.getMaxPstPerTso(state2).isEmpty());

        assertTrue(ralp.getMaxRangeActionPerTso(state0).isEmpty());
        assertEquals(Map.of("FR", 3), ralp.getMaxRangeActionPerTso(state1));
        assertTrue(ralp.getMaxRangeActionPerTso(state2).isEmpty());

        assertFalse(ralp.areRangeActionLimitedForState(state0));
        assertTrue(ralp.areRangeActionLimitedForState(state1));
        assertTrue(ralp.areRangeActionLimitedForState(state2));
    }

}
