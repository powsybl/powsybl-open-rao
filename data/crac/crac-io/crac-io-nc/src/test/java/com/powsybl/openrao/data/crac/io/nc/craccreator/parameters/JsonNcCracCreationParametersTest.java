/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.nc.craccreator.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.parameters.Border;
import com.powsybl.openrao.data.crac.io.nc.parameters.JsonNcCracCreationParameters;
import com.powsybl.openrao.data.crac.io.nc.parameters.NcCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben-rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
class JsonNcCracCreationParametersTest {
    private static final JsonNcCracCreationParameters JSON_NC_CRAC_CREATION_PARAMETERS = new JsonNcCracCreationParameters();

    @Test
    void basicData() {
        assertEquals("NcCracCreatorParameters", JSON_NC_CRAC_CREATION_PARAMETERS.getExtensionName());
        assertEquals(NcCracCreationParameters.class, JSON_NC_CRAC_CREATION_PARAMETERS.getExtensionClass());
        assertEquals("crac-creation-parameters", JSON_NC_CRAC_CREATION_PARAMETERS.getCategoryName());
    }

    @Test
    void deserializeDefaultSweParameters() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/nc-crac-parameters.json"));
        NcCracCreationParameters ncCracCreationParameters = importedParameters.getExtension(NcCracCreationParameters.class);
        assertNotNull(ncCracCreationParameters);
        assertEquals("10Y1001C--00095L", ncCracCreationParameters.getCapacityCalculationRegionEicCode());
        assertEquals(Set.of("REE"), ncCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState());
        assertEquals(Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200), ncCracCreationParameters.getCurativeInstants());
        assertEquals(Set.of(new Border("ES-FR", "10YDOM--ES-FR--D", "RTE"), new Border("ES-PT", "10YDOM--ES-PT--T", "REN")), ncCracCreationParameters.getBorders());
        assertEquals(Map.of("REE", Set.of("curative 1", "curative 2"), "REN", Set.of("curative 1")), ncCracCreationParameters.getRestrictedCurativeBatchesPerTso());
    }

    @Test
    void deserializeParametersWithExtraCurativeInstantField() {
        OpenRaoException importException;
        try (InputStream inputStream = getClass().getResourceAsStream("/parameters/nc-crac-parameters-curative-instants-nok-1.json")) {
            importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Unexpected field in curative-instants: unknown-curative-instant-field", importException.getMessage());
    }

    @Test
    void deserializeParametersWithMissingCurativeInstantField() {
        OpenRaoException importException;
        try (InputStream inputStream = getClass().getResourceAsStream("/parameters/nc-crac-parameters-curative-instants-nok-2.json")) {
            importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Incomplete data for curative instant; please provide both a name and an application-time", importException.getMessage());
    }

    @Test
    void deserializeParametersWithUnknownField() {
        OpenRaoException importException;
        try (InputStream inputStream = getClass().getResourceAsStream("/parameters/nc-crac-parameters-with-unknown-field.json")) {
            importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertEquals("Unexpected field: unknown-field", importException.getMessage());
    }

    @Test
    void serializeDefaultParameters() {
        CracCreationParameters parameters = new CracCreationParameters();
        NcCracCreationParameters csaParameters = new NcCracCreationParameters();
        parameters.addExtension(NcCracCreationParameters.class, csaParameters);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(parameters, outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(inputStream);

        assertEquals(1, importedParameters.getExtensions().size());
        NcCracCreationParameters ncCracCreationParameters = importedParameters.getExtension(NcCracCreationParameters.class);
        assertNotNull(ncCracCreationParameters);
        assertEquals("10Y1001C--00095L", ncCracCreationParameters.getCapacityCalculationRegionEicCode());
        assertTrue(ncCracCreationParameters.getTsosWhichDoNotUsePatlInFinalState().isEmpty());
        assertEquals(Map.of("curative 1", 300, "curative 2", 600, "curative 3", 1200), ncCracCreationParameters.getCurativeInstants());
        assertTrue(ncCracCreationParameters.getBorders().isEmpty());
        assertEquals(Map.of("REE", Set.of("curative 1")), ncCracCreationParameters.getRestrictedCurativeBatchesPerTso());
    }

}
