/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class FbConstraintCracCreationParametersTest {
    @Test
    void testOffsetDateTime() {
        FbConstraintCracCreationParameters parameters = new FbConstraintCracCreationParameters();
        assertEquals("FbConstraintCracCreationParameters", parameters.getName());
        assertNull(parameters.getOffsetDateTime());
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2024-07-09T07:15:00Z");
        parameters.setOffsetDateTime(offsetDateTime);
        assertEquals(offsetDateTime, parameters.getOffsetDateTime());
    }
}
