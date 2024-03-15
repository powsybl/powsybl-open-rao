/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.parameters.JsonCsaCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonCsaCracCreationParametersTest {
    private static final JsonCsaCracCreationParameters JSON_CSA_CRAC_CREATION_PARAMETERS = new JsonCsaCracCreationParameters();

    @Test
    void basicData() {
        assertEquals("CsaCracCreatorParameters", JSON_CSA_CRAC_CREATION_PARAMETERS.getExtensionName());
        assertEquals(CsaCracCreationParameters.class, JSON_CSA_CRAC_CREATION_PARAMETERS.getExtensionClass());
        assertEquals("crac-creation-parameters", JSON_CSA_CRAC_CREATION_PARAMETERS.getCategoryName());
    }

    @Test
    void deserializeParametersWithRegionEicCode() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-region-eic-code.json"));
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertEquals("10Y1001C–00095L", csaCracCreationParameters.getCapacityCalculationRegionEicCode());
    }

    @Test
    void deserializeParametersWithUnknownField() {
        OpenRaoException importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-unknown-field.json")));
        assertEquals("Unexpected field: unknown-field", importException.getMessage());
    }

    @Test
    void serializeParametersWithRegionEicCode() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(parameters, outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(inputStream);

        assertEquals(1, importedParameters.getExtensions().size());
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertEquals("10Y1001C–00095L", csaCracCreationParameters.getCapacityCalculationRegionEicCode());
    }

}
