/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ra_optimisation.json;

import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.extensions.AbstractExtension;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class JsonRaoComputationResultTest extends AbstractConverterTest {

    @Test
    public void roundTrip() throws IOException {
        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        roundTripTest(result, JsonRaoComputationResult::write, JsonRaoComputationResult::read, "/RaoComputationResult.json");
    }

    @Test
    public void writeExtension() throws IOException {
        RaoComputationResult result = new RaoComputationResult(RaoComputationResult.Status.SUCCESS);
        result.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(result, JsonRaoComputationResult::write, AbstractConverterTest::compareTxt, "/RaoComputationResultWithExtension.json");
    }

    @Test
    public void readExtension() throws IOException {
        RaoComputationResult result = JsonRaoComputationResult.read(getClass().getResourceAsStream("/RaoComputationResultWithExtension.json"));
        assertEquals(1, result.getExtensions().size());
        assertNotNull(result.getExtension(DummyExtension.class));
        assertNotNull(result.getExtensionByName("dummy-extension"));
    }

    @Test
    public void readError() throws IOException {
        try {
            JsonRaoComputationParameters.read(getClass().getResourceAsStream("/RaoComputationResultWithExtension.json"));
            Assert.fail();
        } catch (AssertionError ignored) {
        }
    }

    static class DummyExtension extends AbstractExtension<RaoComputationResult> {

        DummyExtension() {
            super();
        }

        @Override
        public String getName() {
            return "dummy-extension";
        }
    }

    @AutoService(JsonRaoComputationResult.ExtensionSerializer.class)
    public static class DummySerializer implements JsonRaoComputationResult.ExtensionSerializer<DummyExtension> {

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
            return "rao-computation-result";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }

}
