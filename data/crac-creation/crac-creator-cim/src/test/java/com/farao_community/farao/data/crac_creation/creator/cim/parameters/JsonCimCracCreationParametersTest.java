/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_creation.creator.cim.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class JsonCimCracCreationParametersTest {

    @Test
    public void roundTripTest() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        CimCracCreationParameters exportedCimParameters = new CimCracCreationParameters();
        exportedCimParameters.setRangeActionGroupsAsString(List.of("rangeAction3 + rangeAction4", "hvdc1 + hvdc2"));

        exportedParameters.addExtension(CimCracCreationParameters.class, exportedCimParameters);

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);
        assertEquals(2, cimCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cimCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cimCracCreationParameters.getRangeActionGroupsAsString().get(1));
    }

    @Test
    public void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-ok.json"));

        CimCracCreationParameters cimCracCreationParameters = importedParameters.getExtension(CimCracCreationParameters.class);
        assertNotNull(cimCracCreationParameters);

        assertEquals(2, cimCracCreationParameters.getRangeActionGroupsAsString().size());
        assertEquals("rangeAction3 + rangeAction4", cimCracCreationParameters.getRangeActionGroupsAsString().get(0));
        assertEquals("hvdc1 + hvdc2", cimCracCreationParameters.getRangeActionGroupsAsString().get(1));
    }

    @Test (expected = FaraoException.class)
    public void importNokTest() {
        JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/cim-crac-creation-parameters-nok.json"));
    }
}
