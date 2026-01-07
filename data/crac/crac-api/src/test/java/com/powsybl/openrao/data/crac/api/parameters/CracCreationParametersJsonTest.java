/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.api.parameters;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class CracCreationParametersJsonTest {

    @Test
    void testRoundTripJson() {
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
    void importOkTest() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-ok.json"));
        assertNotNull(importedParameters);
        assertEquals("anotherCracFactory", importedParameters.getCracFactoryName());
    }

    @Test
    void importFromFileWithRaLimits() {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(getClass().getResourceAsStream("/parameters/crac-creator-parameters-with-ra-limits.json"));
        Map<String, RaUsageLimits> raUsageLimitsFromFile = importedParameters.getRaUsageLimitsPerInstant();
        assertEquals(2, raUsageLimitsFromFile.size());
        RaUsageLimits raUsageLimitsPreventive = raUsageLimitsFromFile.get("preventive");
        RaUsageLimits raUsageLimitsCurative = raUsageLimitsFromFile.get("curative");
        RaUsageLimits expectedLimitsPreventive = new RaUsageLimits();
        expectedLimitsPreventive.setMaxRa(3);
        expectedLimitsPreventive.setMaxTso(5);
        expectedLimitsPreventive.setMaxRaPerTso(Map.of("FR", 4));
        expectedLimitsPreventive.setMaxTopoPerTso(Map.of("FR", 2));
        expectedLimitsPreventive.setMaxPstPerTso(Map.of("FR", 3));
        expectedLimitsPreventive.setMaxElementaryActionsPerTso(Map.of("FR", 10));
        RaUsageLimits expectedLimitsCurative = new RaUsageLimits();
        expectedLimitsCurative.setMaxRa(7);
        expectedLimitsCurative.setMaxTso(2);
        expectedLimitsCurative.setMaxRaPerTso(Map.of("FR", 7));
        expectedLimitsCurative.setMaxTopoPerTso(Map.of("FR", 1));
        expectedLimitsCurative.setMaxPstPerTso(Map.of("FR", 5));
        assertEquals(expectedLimitsPreventive, raUsageLimitsPreventive);
        assertEquals(expectedLimitsCurative, raUsageLimitsCurative);
    }

    @Test
    void importNokTest() {
        InputStream inputStream = getClass().getResourceAsStream("/parameters/crac-creator-parameters-nok.json");
        assertThrows(OpenRaoException.class, () -> JsonCracCreationParameters.read(inputStream));
    }

    @Test
    void importFromFile() throws URISyntaxException {
        CracCreationParameters importedParameters = JsonCracCreationParameters.read(Paths.get(getClass().getResource("/parameters/crac-creator-parameters-ok.json").toURI()));
        assertNotNull(importedParameters);
        assertEquals("anotherCracFactory", importedParameters.getCracFactoryName());
    }
}
