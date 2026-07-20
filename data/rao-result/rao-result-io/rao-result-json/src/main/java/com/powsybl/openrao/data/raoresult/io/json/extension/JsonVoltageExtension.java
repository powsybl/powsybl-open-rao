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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.extension.VoltageResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;
import com.powsybl.openrao.data.raoresult.io.json.Version;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonVoltageExtension implements RaoResultJsonUtils.ExtensionSerializer<VoltageResult> {
    @Override
    public void serialize(VoltageResult voltageResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        voltageResult.serialize(jsonGenerator);
    }

    @Override
    public VoltageResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Version version = (Version) deserializationContext.getAttribute("version");
        if (version.major() == 1) {
            throw new OpenRaoException("Voltage results extension is only available for JSON RAO Result versions >= 2.");
        }
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        VoltageResult voltageResult = new VoltageResult();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            VoltageCnec voltageCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "voltageCnecId" -> {
                        jsonParser.nextToken();
                        voltageCnec = crac.getVoltageCnec(jsonParser.getValueAsString());
                    }
                    case "measurements" -> {
                        jsonParser.nextToken();
                        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                            if (jsonParser.currentName().equals(Unit.KILOVOLT.name().toLowerCase())) {
                                jsonParser.nextToken();
                                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                                    Instant instant = null;
                                    Double minVoltage = null;
                                    Double maxVoltage = null;
                                    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                                        switch (jsonParser.currentName()) {
                                            case "instant" -> {
                                                jsonParser.nextToken();
                                                String instantId = jsonParser.getValueAsString();
                                                instant = "initial".equals(instantId) ? null : crac.getInstant(instantId);
                                            }
                                            case "minVoltage" -> {
                                                jsonParser.nextToken();
                                                minVoltage = jsonParser.getDoubleValue();
                                            }
                                            case "maxVoltage" -> {
                                                jsonParser.nextToken();
                                                maxVoltage = jsonParser.getDoubleValue();
                                            }
                                            case "margin" -> jsonParser.nextToken();
                                            default -> throw new OpenRaoException("Unexpected token: " + jsonParser.getCurrentToken());
                                        }
                                    }
                                    if (minVoltage != null && maxVoltage != null) {
                                        voltageResult.addMeasurement(minVoltage, maxVoltage, instant, voltageCnec, Unit.KILOVOLT);
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
        return voltageResult;
    }

    @Override
    public String getExtensionName() {
        return "voltage-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super VoltageResult> getExtensionClass() {
        return VoltageResult.class;
    }
}
