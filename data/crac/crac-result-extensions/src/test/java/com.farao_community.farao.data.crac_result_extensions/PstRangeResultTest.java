/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResultTest {
    private static final double EPSILON = 0.1;
    private State state;
    private PstRangeResult pstRangeResult;

    @Before
    public void setUp() {
        state = new SimpleState(Optional.empty(), new Instant("initial", 0));
        pstRangeResult = new PstRangeResult(Collections.singleton(state));
    }

    @Test
    public void setSetPoint() {
        pstRangeResult.setSetPoint(state, 3.2);
        pstRangeResult.setTap(state, 5);

        assertEquals(3.2, pstRangeResult.getSetPoint(state), EPSILON);
        assertEquals(5, pstRangeResult.getTap(state));
    }
}
