/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.extensions.AbstractExtension;
import com.farao_community.farao.ra_optimisation.RaoComputationParameters;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Mohamed Zelmat {@literal <mohamed.zelmat at rte-france.com>}
 */
public class JsonRaoComputationParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoComputationParameters parameters = new RaoComputationParameters();
        roundTripTest(parameters, JsonRaoComputationParameters::write, JsonRaoComputationParameters::read, "/RaoComputationParameters.json");
    }

    @Test
    public void writeExtension() throws IOException {
        RaoComputationParameters parameters = new RaoComputationParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonRaoComputationParameters::write, AbstractConverterTest::compareTxt, "/RaoComputationParametersWithExtension.json");
    }

    @Test
    public void updateLoadFlowParameters() {
        RaoComputationParameters parameters = new RaoComputationParameters();
        parameters.getLoadFlowParameters().setSpecificCompatibility(true);
        JsonRaoComputationParameters.update(parameters, getClass().getResourceAsStream("/RaoComputationParametersIncomplete.json"));

        assertEquals(true, parameters.getLoadFlowParameters().isSpecificCompatibility());
    }

    @Test
    public void readExtension() throws IOException {
        RaoComputationParameters parameters = JsonRaoComputationParameters.read(getClass().getResourceAsStream("/RaoComputationParametersWithExtension.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test
    public void readError() throws IOException {
        try {
            JsonRaoComputationParameters.read(getClass().getResourceAsStream("/RaoComputationParametersError.json"));
            fail();
        } catch (AssertionError e) {
            // should throw
            assertTrue(e.getMessage().contains("Unexpected field"));
        }
    }

    static class DummyExtension extends AbstractExtension<RaoComputationParameters> {

        DummyExtension() {
            super();
        }

        @Override
        public String getName() {
            return "dummy-extension";
        }
    }

    @AutoService(JsonRaoComputationParameters.ExtensionSerializer.class)
    public static class DummySerializer implements JsonRaoComputationParameters.ExtensionSerializer<DummyExtension> {

        @Override
        public void serialize(DummyExtension extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }

        @Override
        public DummyExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummy-extension";
        }

        @Override
        public String getCategoryName() {
            return "rao-computation-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

}
