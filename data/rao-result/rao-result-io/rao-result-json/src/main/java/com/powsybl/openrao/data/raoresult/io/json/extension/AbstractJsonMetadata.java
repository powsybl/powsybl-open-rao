/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.raoresult.api.extension.RaoMetadata;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public abstract class AbstractJsonMetadata<E extends RaoMetadata> implements RaoResultJsonUtils.ExtensionSerializer<E> {
    protected static final String EXECUTION_DETAILS = "executionDetails";
    protected static final String COMPUTATION_START = "computationStart";
    protected static final String COMPUTATION_END = "computationEnd";
    protected static final String COMPUTATION_DURATION = "computationDuration";
    protected static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public void serialize(E metadata, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(EXECUTION_DETAILS, metadata.getExecutionDetails());
        Optional<OffsetDateTime> computationStart = metadata.getComputationStart();
        if (computationStart.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_START, computationStart.get().format(FORMATTER));
        }
        Optional<OffsetDateTime> computationEnd = metadata.getComputationEnd();
        if (computationEnd.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_END, computationEnd.get().format(FORMATTER));
        }
        Optional<Duration> duration = metadata.getComputationDuration();
        if (duration.isPresent()) {
            jsonGenerator.writeStringField(COMPUTATION_DURATION, duration.get().toString());
        }
        serializeImplementationSpecificMetadata(metadata, jsonGenerator, serializerProvider);
        jsonGenerator.writeEndObject();
    }

    protected abstract void serializeImplementationSpecificMetadata(E metadata, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException;
}
