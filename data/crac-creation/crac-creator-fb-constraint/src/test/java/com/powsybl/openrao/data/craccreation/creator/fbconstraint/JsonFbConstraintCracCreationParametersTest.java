/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craccreation.creator.fbconstraint;

import com.powsybl.openrao.data.cracapi.parameters.CracCreationParameters;
import com.powsybl.openrao.data.cracapi.parameters.JsonCracCreationParameters;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class JsonFbConstraintCracCreationParametersTest {

    @Test
    void roundTripTestWithTimestamp() {
        CracCreationParameters exportedParameters = createCracCreationParametersWithFbConstraintExtension(OffsetDateTime.parse("2024-07-09T07:15:00Z"));
        CracCreationParameters importedParameters = roundTrip(exportedParameters);
        assertEquals(OffsetDateTime.parse("2024-07-09T07:15:00Z"), importedParameters.getExtension(FbConstraintCracCreationParameters.class).getOffsetDateTime());
    }

    @Test
    void roundTripTestWithNoTimestamp() {
        CracCreationParameters exportedParameters = createCracCreationParametersWithFbConstraintExtension(null);
        CracCreationParameters importedParameters = roundTrip(exportedParameters);
        assertNull(importedParameters.getExtension(FbConstraintCracCreationParameters.class).getOffsetDateTime());
    }

    private CracCreationParameters roundTrip(CracCreationParameters exportedParameters) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        JsonCracCreationParameters.write(exportedParameters, os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        return JsonCracCreationParameters.read(is);
    }

    private CracCreationParameters createCracCreationParametersWithFbConstraintExtension(OffsetDateTime offsetDateTime) {
        CracCreationParameters exportedParameters = new CracCreationParameters();
        FbConstraintCracCreationParameters exportedFbConstraintParameters = new FbConstraintCracCreationParameters();
        exportedFbConstraintParameters.setOffsetDateTime(offsetDateTime);
        exportedParameters.addExtension(FbConstraintCracCreationParameters.class, exportedFbConstraintParameters);
        return exportedParameters;
    }
}
