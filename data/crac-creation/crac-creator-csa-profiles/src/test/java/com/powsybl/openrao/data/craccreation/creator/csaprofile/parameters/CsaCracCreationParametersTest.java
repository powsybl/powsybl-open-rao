/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class CsaCracCreationParametersTest {
    @Test
    void setAttributes() {
        CsaCracCreationParameters parameters = new CsaCracCreationParameters();
        assertEquals("CsaCracCreatorParameters", parameters.getName());
        assertFalse(parameters.getUseCnecGeographicalFilter());
        parameters.setUseCnecGeographicalFilter(true);
        assertTrue(parameters.getUseCnecGeographicalFilter());
    }
}
