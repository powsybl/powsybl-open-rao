/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class InstantTest {

    private Instant instant;

    @Before
    public void setUp() {
        instant = new Instant("instant", 15);
    }

    @Test
    public void getDuration() {
        assertEquals(15, instant.getDuration(), 0.1);
    }

    @Test
    public void setDuration() {
        instant.setDuration(5);
        assertEquals(5, instant.getDuration(), 0.1);
    }
}
