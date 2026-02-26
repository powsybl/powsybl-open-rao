/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.network.parameters;

import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PstRangeActionsTest extends AbstractTest {

    private PstRangeActions parameters;

    @BeforeEach
    void setUp() {
        parameters = new NetworkCracCreationParameters(null, List.of("cur")).getPstRangeActions();
    }

    @Test
    void testTapRange() {
        assertTrue(parameters.getTapRange(prevInstant).isEmpty());
        assertTrue(parameters.getTapRange(outInstant).isEmpty());
        assertTrue(parameters.getTapRange(cur1Instant).isEmpty());

        parameters.setAvailableTapRangesAtInstants(Map.of("preventive", new PstRangeActions.TapRange(-1, 5, RangeType.RELATIVE_TO_INITIAL_NETWORK)));
        assertEquals(Optional.of(new PstRangeActions.TapRange(-1, 5, RangeType.RELATIVE_TO_INITIAL_NETWORK)), parameters.getTapRange(prevInstant));
        assertTrue(parameters.getTapRange(outInstant).isEmpty());
        assertTrue(parameters.getTapRange(cur1Instant).isEmpty());

        parameters.setAvailableTapRangesAtInstants(Map.of("cur1", new PstRangeActions.TapRange(-10, 50, RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)));
        assertEquals(Optional.of(new PstRangeActions.TapRange(-10, 50, RangeType.RELATIVE_TO_PREVIOUS_TIME_STEP)), parameters.getTapRange(cur1Instant));
        assertTrue(parameters.getTapRange(outInstant).isEmpty());
        assertTrue(parameters.getTapRange(prevInstant).isEmpty());
    }

    @Test
    void testRaPredicate() {
        TwoWindingsTransformer twt1 = Mockito.mock(TwoWindingsTransformer.class);
        TwoWindingsTransformer twt2 = Mockito.mock(TwoWindingsTransformer.class);
        State state1 = Mockito.mock(State.class);
        State state2 = Mockito.mock(State.class);

        assertTrue(parameters.isAvailable(twt1, state1));
        assertTrue(parameters.isAvailable(twt1, state2));
        assertTrue(parameters.isAvailable(twt2, state1));
        assertTrue(parameters.isAvailable(twt2, state2));

        parameters.setPstRaPredicate((twt, state) -> twt.equals(twt1) || state.equals(state2));
        assertTrue(parameters.isAvailable(twt1, state1));
        assertTrue(parameters.isAvailable(twt1, state2));
        assertFalse(parameters.isAvailable(twt2, state1));
        assertTrue(parameters.isAvailable(twt2, state2));
    }
}
