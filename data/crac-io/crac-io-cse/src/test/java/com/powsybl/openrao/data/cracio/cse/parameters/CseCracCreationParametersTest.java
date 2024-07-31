/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.cse.parameters;

import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CseCracCreationParametersTest {

    @Test
    void testDefaultParameters() {
        CseCracCreationParameters parameters = new CseCracCreationParameters();

        assertEquals(0, parameters.getRangeActionGroupsAsString().size());
        assertEquals(0, parameters.getRangeActionGroups().size());
    }

    @Test
    void testParallelRaConf() {

        CseCracCreationParameters parameters = new CseCracCreationParameters();
        List<String> parallelRaAsConcatenatedString = new ArrayList<>();
        parallelRaAsConcatenatedString.add("rangeAction1 + rangeAction3 + rangeAction7");
        parallelRaAsConcatenatedString.add("errorInThisOne");

        parameters.setRangeActionGroupsAsString(parallelRaAsConcatenatedString);

        assertEquals(1, parameters.getRangeActionGroupsAsString().size());
        assertEquals(1, parameters.getRangeActionGroups().size());
        assertEquals("rangeAction1 + rangeAction3 + rangeAction7", parameters.getRangeActionGroups().get(0).toString());
    }

    @Test
    void testParametersWithinExtendable() {
        CracCreationParameters parameters = new CracCreationParameters();
        assertNull(parameters.getExtension(CseCracCreationParameters.class));

        parameters.addExtension(CseCracCreationParameters.class, new CseCracCreationParameters());
        assertNotNull(parameters.getExtension(CseCracCreationParameters.class));
    }

}
