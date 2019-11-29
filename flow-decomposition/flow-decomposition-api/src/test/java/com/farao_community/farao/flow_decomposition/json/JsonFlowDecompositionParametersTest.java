/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.extensions.AbstractExtension;
import com.farao_community.farao.flow_decomposition.FlowDecompositionParameters;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonFlowDecompositionParametersTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters();
        roundTripTest(parameters, JsonFlowDecompositionParameters::write, JsonFlowDecompositionParameters::read, "/FlowDecompositionParameters.json");
    }

    @Test
    public void writeExtension() throws IOException {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonFlowDecompositionParameters::write, AbstractConverterTest::compareTxt, "/FlowDecompositionParametersWithExtension.json");
    }

    @Test
    public void updateLoadFlowParameters() {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters();
        parameters.getLoadFlowParameters().setSpecificCompatibility(true);
        JsonFlowDecompositionParameters.update(parameters, getClass().getResourceAsStream("/FlowDecompositionParametersIncomplete.json"));

        assertEquals(true, parameters.getLoadFlowParameters().isSpecificCompatibility());
    }

    @Test
    public void readExtension() throws IOException {
        FlowDecompositionParameters parameters = JsonFlowDecompositionParameters.read(getClass().getResourceAsStream("/FlowDecompositionParametersWithExtension.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test
    public void readError() throws IOException {
        try {
            JsonFlowDecompositionParameters.read(getClass().getResourceAsStream("/FlowDecompositionParametersWithExtension.json"));
            Assert.fail();
        } catch (AssertionError ignored) {
        }
    }

    static class DummyExtension extends AbstractExtension<FlowDecompositionParameters> {

        DummyExtension() {
            super();
        }

        @Override
        public String getName() {
            return "dummy-extension";
        }
    }

    @AutoService(JsonFlowDecompositionParameters.ExtensionSerializer.class)
    public static class DummySerializer implements JsonFlowDecompositionParameters.ExtensionSerializer<DummyExtension> {

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
            return "flow-decomposition-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

}
