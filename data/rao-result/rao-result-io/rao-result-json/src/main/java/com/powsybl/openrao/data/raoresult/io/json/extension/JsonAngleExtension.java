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
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.Version;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonAngleExtension implements RaoResultJsonUtils.ExtensionSerializer<AngleResult> {
    @Override
    public void serialize(AngleResult angleResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        angleResult.serialize(jsonGenerator);
    }

    @Override
    public AngleResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Version version = (Version) deserializationContext.getAttribute("version");
        if (version.major() == 1) {
            throw new OpenRaoException("Angle results extension is only available for JSON RAO Result versions >= 2.");
        }
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        AngleResult angleResult = new AngleResult();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleCnec angleCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "angleCnecId" -> {
                        jsonParser.nextToken();
                        angleCnec = crac.getAngleCnec(jsonParser.getValueAsString());
                    }
                    case "initial" -> deserializeForInstant(jsonParser, angleResult, angleCnec, null, crac);
                    default -> deserializeForInstant(jsonParser, angleResult, angleCnec, jsonParser.currentName(), crac);
                }
            }
        }
        return angleResult;
    }

    private static void deserializeForInstant(JsonParser jsonParser, AngleResult angleResult, AngleCnec angleCnec, String instantId, Crac crac) throws IOException {
        Instant instant = instantId == null ? null : crac.getInstant(instantId);
        jsonParser.nextToken();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            if (jsonParser.currentName().equals(Unit.DEGREE.name().toLowerCase())) {
                jsonParser.nextToken();
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    switch (jsonParser.currentName()) {
                        case "angle" -> {
                            jsonParser.nextToken();
                            angleResult.addMeasurement(jsonParser.getDoubleValue(), instant, angleCnec, Unit.DEGREE);
                        }
                        case "margin" -> jsonParser.nextToken();
                        default -> throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                    }
                }
            } else {
                throw new OpenRaoException("Unsupported unit for angle values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "angle-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super AngleResult> getExtensionClass() {
        return AngleResult.class;
    }
}
