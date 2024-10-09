/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.csaprofiles.craccreator.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.Border;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.CsaCracCreationParameters;
import com.powsybl.openrao.data.cracio.csaprofiles.parameters.JsonCsaCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
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
    void deserializeValidParameters() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-json-csa-crac-creation-parameters-test.json"));
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertEquals("10XFR-RTE------Q", csaCracCreationParameters.getCapacityCalculationRegionEicCode());
        assertEquals(60, csaCracCreationParameters.getSpsMaxTimeToImplementThresholdInSeconds());
        assertEquals(Map.of("REE", false, "REN", false, "RTE", true), csaCracCreationParameters.getUsePatlInFinalState());
        assertEquals(Map.of("curative 1", 0, "curative 2", 200, "curative 3", 500), csaCracCreationParameters.getCraApplicationWindow());
        assertEquals(Set.of(new Border("ES-FR", "10YDOM--ES-FR--D", "RTE"), new Border("ES-PT", "10YDOM--ES-PT--T", "REN")), csaCracCreationParameters.getBorders());
    }

    @Test
    void deserializeDefaultParameters() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters.json"));
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertEquals("10Y1001C--00095L", csaCracCreationParameters.getCapacityCalculationRegionEicCode());
        assertEquals(0, csaCracCreationParameters.getSpsMaxTimeToImplementThresholdInSeconds());
        assertEquals(Map.of("REE", false, "REN", true, "RTE", true), csaCracCreationParameters.getUsePatlInFinalState());
        assertEquals(Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200), csaCracCreationParameters.getCraApplicationWindow());
        assertEquals(Set.of(new Border("ES-FR", "10YDOM--ES-FR--D", "RTE"), new Border("ES-PT", "10YDOM--ES-PT--T", "REN")), csaCracCreationParameters.getBorders());
    }

    @Test
    void deserializeParametersWithUnknownField() {
        OpenRaoException importException;
        try (InputStream inputStream = getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-unknown-field.json")) {
            importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Unexpected field: unknown-field", importException.getMessage());
    }

    @Test
    void serializeValidParameters() {
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
        assertEquals("10Y1001C--00095L", csaCracCreationParameters.getCapacityCalculationRegionEicCode());
        assertEquals(0, csaCracCreationParameters.getSpsMaxTimeToImplementThresholdInSeconds());
        assertEquals(Map.of("REE", false, "REN", true, "RTE", true), csaCracCreationParameters.getUsePatlInFinalState());
        assertEquals(Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200), csaCracCreationParameters.getCraApplicationWindow());
        assertEquals(Set.of(new Border("ES-FR", "10YDOM--ES-FR--D", "RTE"), new Border("ES-PT", "10YDOM--ES-PT--T", "REN")), csaCracCreationParameters.getBorders());
    }

}
