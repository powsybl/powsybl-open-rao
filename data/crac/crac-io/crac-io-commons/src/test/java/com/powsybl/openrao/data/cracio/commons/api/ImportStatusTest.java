/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.commons.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.powsybl.openrao.data.cracio.commons.api.ImportStatus.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ImportStatusTest {
    @Test
    void testDescription() {
        assertEquals("Import OK.", IMPORTED.getDescription());
        assertEquals("Not found in network.", ELEMENT_NOT_FOUND_IN_NETWORK.getDescription());
        assertEquals("Data incomplete", INCOMPLETE_DATA.getDescription());
        assertEquals("Data inconsistent", INCONSISTENCY_IN_DATA.getDescription());
        assertEquals("Functionality is not handled by Open RAO for the moment.", NOT_YET_HANDLED_BY_OPEN_RAO.getDescription());
        assertEquals("Not used in RAO", NOT_FOR_RAO.getDescription());
        assertEquals("Not for requested timestamp", NOT_FOR_REQUESTED_TIMESTAMP.getDescription());
        assertEquals("", OTHER.getDescription());
    }
}
