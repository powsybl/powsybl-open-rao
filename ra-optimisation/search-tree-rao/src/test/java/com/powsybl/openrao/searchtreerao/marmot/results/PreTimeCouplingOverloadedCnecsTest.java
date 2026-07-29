/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.impl.utils.ExhaustiveRaoResultCreation;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonExporter;
import com.powsybl.openrao.searchtreerao.marmot.results.extensions.JsonPreTimeCouplingOverloadedCnecs;
import com.powsybl.openrao.searchtreerao.marmot.results.extensions.PreTimeCouplingOverloadedCnecs;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class PreTimeCouplingOverloadedCnecsTest {
    @Test
    void testSerialize() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        raoResult.addExtension(PreTimeCouplingOverloadedCnecs.class, new PreTimeCouplingOverloadedCnecs(Set.of("cnec1", "cnec2")));

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/raoResult/rao-result-marmot-extension.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testDeserialize() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        InputStream inputStream = getClass().getResourceAsStream("/raoResult/rao-result-marmot-extension.json");
        RaoResult raoResult = RaoResult.read(inputStream, crac);

        PreTimeCouplingOverloadedCnecs extension = raoResult.getExtension(PreTimeCouplingOverloadedCnecs.class);
        assertNotNull(extension);
        assertEquals(Set.of("cnec1", "cnec2"), extension.getCriticalCnecIds());
    }

    @Test
    void testRoundTrip() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);
        Set<String> ids = Set.of("cnec1", "cnec2", "cnec3");
        raoResult.addExtension(PreTimeCouplingOverloadedCnecs.class, new PreTimeCouplingOverloadedCnecs(ids));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult deserialized = RaoResult.read(inputStream, crac);

        PreTimeCouplingOverloadedCnecs extension = deserialized.getExtension(PreTimeCouplingOverloadedCnecs.class);
        assertNotNull(extension);
        assertEquals(ids, extension.getCriticalCnecIds());
    }

    @Test
    void testEmptyListRoundTrip() throws IOException {
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);
        raoResult.addExtension(PreTimeCouplingOverloadedCnecs.class, new PreTimeCouplingOverloadedCnecs(Set.of()));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);

        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        RaoResult deserialized = RaoResult.read(inputStream, crac);

        PreTimeCouplingOverloadedCnecs extension = deserialized.getExtension(PreTimeCouplingOverloadedCnecs.class);
        assertNotNull(extension);
        assertTrue(extension.getCriticalCnecIds().isEmpty());
    }

    @Test
    void testGetClass() {
        assertEquals(PreTimeCouplingOverloadedCnecs.class, new JsonPreTimeCouplingOverloadedCnecs().getExtensionClass());
    }
}
