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
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.extension.AngleExtension;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonAngleExtension implements RaoResultJsonUtils.ExtensionSerializer<AngleExtension> {
    @Override
    public void serialize(AngleExtension angleExtension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        angleExtension.serialize(jsonGenerator);
    }

    @Override
    public AngleExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        AngleExtension angleExtension = new AngleExtension();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleCnec angleCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "angleCnecId" -> {
                        jsonParser.nextToken();
                        angleCnec = crac.getAngleCnec(jsonParser.getValueAsString());
                    }
                    case "measurements" -> {
                        jsonParser.nextToken();
                        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                            if (jsonParser.currentName().equals(Unit.DEGREE.name().toLowerCase())) {
                                jsonParser.nextToken();
                                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                                    Instant instant = null;
                                    Double angle = null;
                                    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                                        switch (jsonParser.currentName()) {
                                            case "instant" -> {
                                                jsonParser.nextToken();
                                                String instantId = jsonParser.getValueAsString();
                                                instant = "initial".equals(instantId) ? null : crac.getInstant(instantId);
                                            }
                                            case "angle" -> {
                                                jsonParser.nextToken();
                                                angle = jsonParser.getDoubleValue();
                                            }
                                            case "margin" -> jsonParser.nextToken();
                                            default -> throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                                        }
                                    }
                                    if (angle != null) {
                                        angleExtension.addAngle(angle, instant, angleCnec, Unit.DEGREE);
                                    }
                                }
                            } else {
                                throw new OpenRaoException("Unsupported unit for angle values.");
                            }
                        }
                    }
                    default -> throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                }
            }
        }
        return angleExtension;
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
    public Class<? super AngleExtension> getExtensionClass() {
        return AngleExtension.class;
    }
}
