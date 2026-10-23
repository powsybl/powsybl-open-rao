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
import com.powsybl.contingency.Contingency;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Version;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.api.extension.Metadata;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonMetadata implements RaoResultJsonUtils.ExtensionSerializer<Metadata> {
    @Override
    public void serialize(Metadata metadata, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        metadata.serialize(jsonGenerator);
    }

    @Override
    public Metadata deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Version version = (Version) deserializationContext.getAttribute("version");
        if (version.major() == 1) {
            throw new OpenRaoException("Metadata extension is only available for JSON RAO Result versions >= 2.");
        }
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        Metadata metadata = new Metadata();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "computationStatus" -> jsonParser.nextToken();
                case "executionDetails" -> metadata.setExecutionDetails(jsonParser.nextTextValue());
                case "computationStatusMap" -> {
                    jsonParser.nextToken();
                    getComputationStatusMap(jsonParser, crac).forEach(metadata::setComputationStatus);
                }
                default ->
                    throw new OpenRaoException("Unknown metadata field: %s.".formatted(jsonParser.currentName()));
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

    private static ComputationStatus getComputationStatus(String computationStatus) {
        return switch (computationStatus) {
            case "default" -> ComputationStatus.DEFAULT;
            case "partial-failure" -> ComputationStatus.PARTIAL_FAILURE;
            case "failure" -> ComputationStatus.FAILURE;
            default -> throw new OpenRaoException("Unknown computation status: " + computationStatus);
        };
    }

    private static Map<State, ComputationStatus> getComputationStatusMap(JsonParser jsonParser, Crac crac) throws IOException {
        Map<State, ComputationStatus> map = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            ComputationStatus computationStatus = null;
            Contingency contingency = null;
            Instant instant = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "computationStatus" -> computationStatus = getComputationStatus(jsonParser.nextTextValue());
                    case "instant" -> instant = crac.getInstant(jsonParser.nextTextValue());
                    case "contingency" -> contingency = crac.getContingency(jsonParser.nextTextValue());
                    default -> throw new OpenRaoException("Unknown computationStatusMap field in metadata: %s.".formatted(jsonParser.currentName()));
                }
            }
            if (computationStatus != null && instant != null) {
                State state = getState(crac, contingency, instant);
                if (state != null) {
                    map.put(state, computationStatus);
                }
            }
        }
        return map;
    }

    private static State getState(Crac crac, Contingency contingency, Instant instant) {
        if (contingency == null) {
            State preventiveState = crac.getPreventiveState();
            if (!instant.isPreventive()) {
                throw new OpenRaoException("No state for instant %s can be defined without a contingency.".formatted(instant.getId()));
            }
            return preventiveState;
        }
        return crac.getState(contingency, instant);
    }
}
