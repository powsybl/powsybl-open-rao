/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.io.fbconstraint.parameters;

import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
class JsonFbConstraintCracCreationParametersTest {

    @Test
    void roundTripTest() {
        // prepare parameters to export
        CracCreationParameters exportedParameters = new CracCreationParameters();
        FbConstraintCracCreationParameters exportedFbConstraintParameters = new FbConstraintCracCreationParameters();
        exportedFbConstraintParameters.setTimestamp(OffsetDateTime.parse("2025-01-10T05:00:00Z"));
        exportedParameters.addExtension(FbConstraintCracCreationParameters.class, exportedFbConstraintParameters);

        // roundTrip
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(is);

        // test re-imported parameters
        FbConstraintCracCreationParameters fbConstraintCracCreationParameters = importedParameters.getExtension(FbConstraintCracCreationParameters.class);
        assertNotNull(fbConstraintCracCreationParameters);
        assertEquals(OffsetDateTime.parse("2025-01-10T05:00:00Z"), fbConstraintCracCreationParameters.getTimestamp());
    }

    @Test
    void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/fbconstraint-crac-creation-parameters_ok.json"));

        FbConstraintCracCreationParameters fbConstraintCracCreationParameters = importedParameters.getExtension(FbConstraintCracCreationParameters.class);
        assertNotNull(fbConstraintCracCreationParameters);
        assertEquals(OffsetDateTime.parse("2025-01-10T05:00:00Z"), fbConstraintCracCreationParameters.getTimestamp());
    }

    @Test
    void importNokTest() {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/fbconstraint-crac-creation-parameters_nok.json");
        assertThrows(DateTimeParseException.class, () -> JsonCracCreationParameters.read(inputStream));
    }
}
