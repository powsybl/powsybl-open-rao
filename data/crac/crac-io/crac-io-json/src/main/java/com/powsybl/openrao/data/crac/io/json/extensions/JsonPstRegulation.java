/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.extensions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.extensions.PstRegulation;
import com.powsybl.openrao.data.crac.api.extensions.PstRegulationInput;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(ExtensionsHandler.ExtensionSerializer.class)
public class JsonPstRegulation implements ExtensionsHandler.ExtensionSerializer<Crac, PstRegulation> {
    @Override
    public void serialize(PstRegulation pstRegulation, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray();
        for (PstRegulationInput pstRegulationInput : pstRegulation.getRegulationInputs()) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("pstId", pstRegulationInput.pstId());
            jsonGenerator.writeStringField("monitoredBranchId", pstRegulationInput.monitoredBranchId());
            jsonGenerator.writeNumberField("threshold", pstRegulationInput.threshold());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public PstRegulation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        PstRegulation pstRegulation = new PstRegulation();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String pstId = null;
            String monitoredBranchId = null;
            Double threshold = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "pstId" -> pstId = jsonParser.nextTextValue();
                    case "monitoredBranchId" -> monitoredBranchId = jsonParser.nextTextValue();
                    case "threshold" -> {
                        jsonParser.nextToken();
                        threshold = jsonParser.getDoubleValue();
                    }
                    default ->
                        throw new OpenRaoException("Unexpected field name '%s' in %s.".formatted(jsonParser.nextFieldName(), getExtensionName()));
                }
            }
            if (pstId != null && monitoredBranchId != null && threshold != null) {
                pstRegulation.addPstRegulationInput(new PstRegulationInput(pstId, monitoredBranchId, threshold));
            }
        }
        return pstRegulation;
    }

    @Override
    public String getExtensionName() {
        return "pst-regulation";
    }

    @Override
    public String getCategoryName() {
        return "crac";
    }

    @Override
    public Class<? super PstRegulation> getExtensionClass() {
        return PstRegulation.class;
    }
}
