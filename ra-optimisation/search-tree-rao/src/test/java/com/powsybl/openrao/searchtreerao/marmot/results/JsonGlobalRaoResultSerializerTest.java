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
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.GlobalRaoResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class JsonGlobalRaoResultSerializerTest {
    @Test
    public void testSerialize() throws IOException {
        GlobalRaoResult globalRaoResult = Mockito.mock(GlobalRaoResult.class);
        Mockito.when(globalRaoResult.getComputationStatus()).thenReturn(ComputationStatus.DEFAULT);
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
