/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.api.parameters;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracCreationParametersJsonTest {

    @Test
    public void testRoundTripJson() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        exportedParameters.setCracFactoryName("coucouFactory");

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        assertEquals("coucouFactory", importedParameters.getCracFactoryName());
    }

    @Test
    public void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-ok.json"));
        assertNotNull(importedParameters);
        assertEquals("anotherCracFactory", importedParameters.getCracFactoryName());
    }

    @Test (expected = FaraoException.class)
    public void importNokTest() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-nok.json"));
    }

    @Test
    public void importFromFile() throws URISyntaxException {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(Paths.get(getClass().getResource("/parameters/crac-creator-parameters-ok.json").toURI()));
        assertNotNull(importedParameters);
        assertEquals("anotherCracFactory", importedParameters.getCracFactoryName());
    }
}
