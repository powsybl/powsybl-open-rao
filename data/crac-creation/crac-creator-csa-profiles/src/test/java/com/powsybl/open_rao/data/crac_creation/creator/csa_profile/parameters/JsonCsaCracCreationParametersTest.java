package com.powsybl.open_rao.data.crac_creation.creator.csa_profile.parameters;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.powsybl.open_rao.data.crac_creation.creator.api.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCsaCracCreationParametersTest {
    private static final JsonCsaCracCreationParameters jsonSerDe = new JsonCsaCracCreationParameters();

    @Test
    void basicData() {
        assertEquals("CsaCracCreatorParameters", jsonSerDe.getExtensionName());
        assertEquals(CsaCracCreationParameters.class, jsonSerDe.getExtensionClass());
        assertEquals("crac-creation-parameters", jsonSerDe.getCategoryName());
    }

    @Test
    void deserializeParametersWithGeographicalFilterActivated() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-geographical-filter.json"));
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertTrue(csaCracCreationParameters.getUseCnecGeographicalFilter());
    }

    @Test
    void deserializeParametersWithGeographicalFilterDeactivated() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-without-geographical-filter.json"));
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertFalse(csaCracCreationParameters.getUseCnecGeographicalFilter());
    }

    @Test
    void deserializeParametersWithDuplicatedGeographicalFilterField() {
        OpenRaoException importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-duplicated-geographical-filter.json")));
        assertEquals("Duplicated field: use-cnec-geographical-filter", importException.getMessage());
    }

    @Test
    void deserializeParametersWithUnknownField() {
        OpenRaoException importException = assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/csa-crac-parameters-with-unknown-field.json")));
        assertEquals("Unexpected field: unknown-field", importException.getMessage());
    }

    @Test
    void serializeParametersWithGeographicalFilterActivated() {
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
        assertFalse(csaCracCreationParameters.getUseCnecGeographicalFilter());
    }

    @Test
    void serializeParametersWithGeographicalFilterDeactivated() {
        CracCreationParameters parameters = new CracCreationParameters();
        CsaCracCreationParameters csaParameters = new CsaCracCreationParameters();
        csaParameters.setUseCnecGeographicalFilter(true);
        parameters.addExtension(CsaCracCreationParameters.class, csaParameters);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(parameters, outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(inputStream);

        assertEquals(1, importedParameters.getExtensions().size());
        CsaCracCreationParameters csaCracCreationParameters = importedParameters.getExtension(CsaCracCreationParameters.class);
        assertNotNull(csaCracCreationParameters);
        assertTrue(csaCracCreationParameters.getUseCnecGeographicalFilter());
    }
}
