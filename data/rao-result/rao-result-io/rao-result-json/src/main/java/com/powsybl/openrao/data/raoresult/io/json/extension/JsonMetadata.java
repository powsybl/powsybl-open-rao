/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonMetadata implements RaoResultJsonUtils.ExtensionSerializer<Metadata> {
    private static final String COMPUTATION_START = "computation-start";
    private static final String COMPUTATION_END = "computation-end";
    private static final String COMPUTATION_DURATION = "computation-duration";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void serialize(Metadata metadata, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        Optional<OffsetDateTime> computationStart = metadata.getComputationStart();
        if (computationStart.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_START, computationStart.get().format(FORMATTER));
        }
        Optional<OffsetDateTime> computationEnd = metadata.getComputationEnd();
        if (computationEnd.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_END, computationEnd.get().format(FORMATTER));
        }
        Optional<Duration> computationDuration = metadata.getComputationDuration();
        if (computationDuration.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_DURATION, computationDuration.get().toString());
        }
        jsonGenerator.writeEndObject();
    }

    @Override
    public Metadata deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Metadata metadata = new Metadata();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case COMPUTATION_START:
                    metadata.setComputationStart(OffsetDateTime.parse(jsonParser.nextTextValue()));
                    break;
                case COMPUTATION_END:
                    metadata.setComputationEnd(OffsetDateTime.parse(jsonParser.nextTextValue()));
                    break;
                case COMPUTATION_DURATION:
                    jsonParser.nextToken(); // duration is computed from start and end timestamps
                    break;
                default:
                    throw new OpenRaoException("Unexpected token in metadata extension: " + jsonParser.currentToken());
            }
        }
        return metadata;
    }

    @Override
    public String getExtensionName() {
        return "metadata";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super Metadata> getExtensionClass() {
        return Metadata.class;
    }
}
