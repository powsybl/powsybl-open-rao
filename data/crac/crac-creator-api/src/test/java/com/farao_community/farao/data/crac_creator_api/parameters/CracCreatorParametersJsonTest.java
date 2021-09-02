/*
 * Copyright (c) 2021, All partners of the iTesla project (http://www.itesla-project.eu/consortium)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creator_api.parameters;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreatorParametersJsonTest {

    @Test
    public void testRoundTripJson() {
        // prepare parameters to export
        CracCreatorParameters exportedParameters = new CracCreatorParameters();
        exportedParameters.setCracFactoryName("coucouFactory");

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreatorParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreatorParameters importedParameters = JsonCracCreatorParameters.read(is);

        // test re-imported parameters
        assertEquals("coucouFactory", importedParameters.getCracFactoryName());
    }

    @Test
    public void importOkTest() {
        CracCreatorParameters importedParameters = JsonCracCreatorParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-ok.json"));
        assertNotNull(importedParameters);
        assertEquals("anotherCracFactory", importedParameters.getCracFactoryName());
    }

    @Test (expected = FaraoException.class)
    public void importNokTest() {
        JsonCracCreatorParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-nok.json"));
    }
}
