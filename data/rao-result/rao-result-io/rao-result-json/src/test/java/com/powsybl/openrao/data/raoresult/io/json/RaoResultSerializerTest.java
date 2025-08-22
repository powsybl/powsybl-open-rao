/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresult.io.json;

import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.utils.ExhaustiveCracCreation;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.api.extension.CriticalCnecsResult;
import com.powsybl.openrao.data.raoresult.impl.utils.ExhaustiveRaoResultCreation;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
class RaoResultSerializerTest {

    @Test
    void testSerialize() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

    @Test
    void testSerializeWithFastRaoExtension() throws IOException {
        // get exhaustive CRAC and RaoResult
        Crac crac = ExhaustiveCracCreation.create();
        RaoResult raoResult = ExhaustiveRaoResultCreation.create(crac);

        raoResult.addExtension(CriticalCnecsResult.class, new CriticalCnecsResult(crac.getFlowCnecs().stream().map(FlowCnec::getId).collect(Collectors.toSet())));

        // export RaoResult
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Properties properties = new Properties();
        properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
        properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
        new RaoResultJsonExporter().exportData(raoResult, crac, properties, outputStream);
        String outputString = outputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/rao-result-fastrao-extension.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(inputString, outputString);
    }

}
