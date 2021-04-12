/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_result_extensions;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PstRangeResultTest {
    private static final double EPSILON = 0.1;
    private String stateId;
    private PstRangeResult pstRangeResult;

    @Before
    public void setUp() {
        stateId = "state-id";
        pstRangeResult = new PstRangeResult(Collections.singleton(stateId));
    }

    @Test
    public void setSetPoint() {
        pstRangeResult.setSetPoint(stateId, 3.2);
        pstRangeResult.setTap(stateId, 5);

        assertEquals(3.2, pstRangeResult.getSetPoint(stateId), EPSILON);
        assertEquals(Integer.valueOf(5), pstRangeResult.getTap(stateId));
    }
}
