/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.impl.utils.ExhaustiveRaoResultCreation;
import com.powsybl.openrao.data.raoresult.io.json.extension.JsonCriticalCnecsResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CriticalCnecsResultRoundTripTest {

    @Test
    void testRoundTrip() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);
        Set<String> ids = Set.of("cnec1", "cnec2", "cnec3");
        raoResult.addExtension(CriticalCnecsResult.class, new CriticalCnecsResult(ids));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult deserialized = RaoResult.read(inputStream, crac);

        CriticalCnecsResult extension = deserialized.getExtension(CriticalCnecsResult.class);
        assertNotNull(extension);
        assertEquals(ids, extension.getCriticalCnecIds());
    }

    @Test
    void testEmptyListRoundTrip() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);
        raoResult.addExtension(CriticalCnecsResult.class, new CriticalCnecsResult(Set.of()));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult deserialized = RaoResult.read(inputStream, crac);

        CriticalCnecsResult extension = deserialized.getExtension(CriticalCnecsResult.class);
        assertNotNull(extension);
        assertTrue(extension.getCriticalCnecIds().isEmpty());
    }

    @Test
    void testGetClass() {
        assertEquals(CriticalCnecsResult.class, new JsonCriticalCnecsResult().getExtensionClass());
    }

    @Test
    void testGetCategoryName() {
        assertEquals("rao-result", new JsonCriticalCnecsResult().getCategoryName());
    }

    @Test
    void testGetExtensionName() {
        assertEquals("critical-cnecs-result", new JsonCriticalCnecsResult().getExtensionName());
    }
}
