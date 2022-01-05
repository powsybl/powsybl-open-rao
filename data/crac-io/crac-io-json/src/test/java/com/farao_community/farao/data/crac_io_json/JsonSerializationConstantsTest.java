/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.getPrimaryVersionNumber;
import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.getSubVersionNumber;
import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonSerializationConstantsTest {

    @Test
    public void versionNumberOkTest() {
        assertEquals(1, getPrimaryVersionNumber("1.2"));
        assertEquals(2, getSubVersionNumber("1.2"));
        assertEquals(2, getPrimaryVersionNumber("2.51"));
        assertEquals(51, getSubVersionNumber("2.51"));
    }

    @Test (expected = FaraoException.class)
    public void versionNumberNok1Test() {
        getPrimaryVersionNumber("v1.2");
    }

    @Test (expected = FaraoException.class)
    public void versionNumberNok2Test() {
        getPrimaryVersionNumber("1.3.1");
    }

    @Test (expected = FaraoException.class)
    public void versionNumberNok3Test() {
        getPrimaryVersionNumber("1.2b");
    }
}
