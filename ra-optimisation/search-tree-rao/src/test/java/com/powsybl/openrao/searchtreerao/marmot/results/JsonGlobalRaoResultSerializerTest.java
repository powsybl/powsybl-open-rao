/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.TemporalDataImpl;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonGlobalRaoResultSerializerTest {
    private GlobalRaoResult globalRaoResult;

    @BeforeEach
    public void setUp() throws IOException {
        Network network1 = Network.read("/network/3Nodes.uct", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network2 = Network.read("/network/3Nodes.uct", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Network network3 = Network.read("/network/3Nodes.uct", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/network/3Nodes.uct"));
        Crac crac1 = Crac.read("/crac/crac-redispatching-202502141040.json", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/crac/crac-redispatching-202502141040.json"), network1);
        Crac crac2 = Crac.read("/crac/crac-redispatching-202502141140.json", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/crac/crac-redispatching-202502141140.json"), network2);
        Crac crac3 = Crac.read("/crac/crac-redispatching-202502141240.json", JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/crac/crac-redispatching-202502141240.json"), network3);
        RaoResult raoResult1 = RaoResult.read(JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/raoResult/raoResult1.json"), crac1);
        RaoResult raoResult2 = RaoResult.read(JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/raoResult/raoResult2.json"), crac2);
        RaoResult raoResult3 = RaoResult.read(JsonGlobalRaoResultSerializerTest.class.getResourceAsStream("/raoResult/raoResult3.json"), crac3);
        OffsetDateTime timestamp1 = OffsetDateTime.of(2025, 2, 14, 10, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp2 = OffsetDateTime.of(2025, 2, 14, 11, 40, 0, 0, ZoneOffset.UTC);
        OffsetDateTime timestamp3 = OffsetDateTime.of(2025, 2, 14, 12, 40, 0, 0, ZoneOffset.UTC);

        globalRaoResult = new GlobalRaoResultImpl(null, new TemporalDataImpl<>(Map.of(timestamp1, raoResult1, timestamp2, raoResult2, timestamp3, raoResult3)));

    }

    @Test
    public void testSerialize() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper();
            SimpleModule module = new JsonGlobalRaoResultSerializerModule();
            objectMapper.registerModule(module);
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(byteArrayOutputStream, globalRaoResult);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String outputStreamString = byteArrayOutputStream.toString();

        // import expected json to compare
        InputStream inputStream = getClass().getResourceAsStream("/raoResult/globalRaoResult_summary.json");
        String inputString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(inputString, outputStreamString);
    }
}
