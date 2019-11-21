/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */

public class NetworkElementTest {

    @Test
    public void testConstructorElementTest() {
        NetworkElement networkElement = new NetworkElement("basicElemId", "basicElemName");
        assertEquals("basicElemId", networkElement.getId());
        assertEquals("basicElemName", networkElement.getName());
        assertEquals("basicElemId", networkElement.toString());
    }

}
