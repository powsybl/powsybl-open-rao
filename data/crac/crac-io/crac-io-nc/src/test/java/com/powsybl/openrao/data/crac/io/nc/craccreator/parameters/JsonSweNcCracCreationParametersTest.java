/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.parameters;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.parameters.JsonSweNcCracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.parameters.SweNcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonSweNcCracCreationParametersTest {
    private static final JsonSweNcCracCreationParameters JSON_SWE_NC_CRAC_CREATION_PARAMETERS = new JsonSweNcCracCreationParameters();

    @Test
    void basicData() {
        assertEquals("SweNcCracCreatorParameters", JSON_SWE_NC_CRAC_CREATION_PARAMETERS.getExtensionName());
        assertEquals(SweNcCracCreationParameters.class, JSON_SWE_NC_CRAC_CREATION_PARAMETERS.getExtensionClass());
        assertEquals("crac-creation-parameters", JSON_SWE_NC_CRAC_CREATION_PARAMETERS.getCategoryName());
    }

    @Test
    void deserializeDefaultSweParameters() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/nc-crac-parameters.json"));
        SweNcCracCreationParameters sweNcCracCreationParameters = importedParameters.getExtension(SweNcCracCreationParameters.class);
        assertNotNull(sweNcCracCreationParameters);
        assertEquals(Set.of("REE"), sweNcCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState());
    }

    @Test
    void serializeDefaultParameters() {
        CracCreationParameters parameters = new CracCreationParameters();
        SweNcCracCreationParameters sweNcParameters = new SweNcCracCreationParameters();
        sweNcParameters.setTsosWhichDoNotUsePatlInFinalState(Set.of("REE", "REN"));
        parameters.addExtension(SweNcCracCreationParameters.class, sweNcParameters);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(parameters, outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(inputStream);

        assertEquals(1, importedParameters.getExtensions().size());
        SweNcCracCreationParameters sweNcCracCreationParameters = importedParameters.getExtension(SweNcCracCreationParameters.class);
        assertNotNull(sweNcCracCreationParameters);
        assertEquals(Set.of("REE", "REN"), sweNcCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState());
    }

}
