/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.raoresult.api.extension.AbstractMetadata;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;
import com.powsybl.openrao.data.raoresult.io.json.extension.AbstractJsonMetadata;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class MarmotMetadata extends AbstractMetadata {
    private static final String EXTENSION_NAME = "marmot-metadata";
    private static final String GLOBAL_LINEAR_OPTIMIZATION_START = "globalLinearOptimizationStart";
    private static final String GLOBAL_LINEAR_OPTIMIZATION_COMPUTATION_TIME = "globalLinearOptimizationComputationTime";

    private OffsetDateTime globalLinearOptimizationStart;

    public MarmotMetadata() {
        super();
    }

    public Optional<OffsetDateTime> getGlobalLinearOptimizationStart() {
        return Optional.ofNullable(globalLinearOptimizationStart);
    }

    public void setGlobalLinearOptimizationStart(OffsetDateTime globalLinearOptimizationStart) {
        this.globalLinearOptimizationStart = globalLinearOptimizationStart;
    }

    public Optional<Duration> getGlobalLinearOptimizationComputationDuration() {
        if (globalLinearOptimizationStart != null && computationEnd != null) {
            return Optional.of(Duration.between(globalLinearOptimizationStart, computationEnd));
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
    public static class JsonSerializer extends AbstractJsonMetadata<MarmotMetadata> {

        @Override
        protected void serializeImplementationSpecificMetadata(MarmotMetadata metadata, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            Optional<OffsetDateTime> globalLinearOptimizationStart = metadata.getGlobalLinearOptimizationStart();
            if (globalLinearOptimizationStart.isPresent()) {
                jsonGenerator.writeStringField(GLOBAL_LINEAR_OPTIMIZATION_START, globalLinearOptimizationStart.get().format(FORMATTER));
            }
            Optional<Duration> globalLinearOptimizationComputationDuration = metadata.getGlobalLinearOptimizationComputationDuration();
            if (globalLinearOptimizationComputationDuration.isPresent()) {
                jsonGenerator.writeStringField(GLOBAL_LINEAR_OPTIMIZATION_COMPUTATION_TIME, globalLinearOptimizationComputationDuration.get().toString());
            }
        }

        @Override
        public MarmotMetadata deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            MarmotMetadata metadata = new MarmotMetadata();
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case EXECUTION_DETAILS:
                        metadata.setExecutionDetails(jsonParser.nextTextValue());
                        break;
                    case COMPUTATION_START:
                        metadata.setComputationStart(OffsetDateTime.parse(jsonParser.nextTextValue()));
                        break;
                    case COMPUTATION_END:
                        metadata.setComputationEnd(OffsetDateTime.parse(jsonParser.nextTextValue()));
                        break;
                    case COMPUTATION_DURATION, GLOBAL_LINEAR_OPTIMIZATION_COMPUTATION_TIME:
                        jsonParser.nextToken(); // duration is computed from start and end timestamps
                        break;
                    case GLOBAL_LINEAR_OPTIMIZATION_START:
                        metadata.setGlobalLinearOptimizationStart(OffsetDateTime.parse(jsonParser.nextTextValue()));
                        break;
                    default:
                        throw new OpenRaoException("Unexpected token in metadata extension: " + jsonParser.currentToken());
                }
            }
            return metadata;
        }

        @Override
        public String getExtensionName() {
            return EXTENSION_NAME;
        }

        @Override
        public String getCategoryName() {
            return "rao-result";
        }

        @Override
        public Class<? super MarmotMetadata> getExtensionClass() {
            return MarmotMetadata.class;
        }
    }
}
