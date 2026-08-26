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
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.commons.Version;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.api.extension.FlowResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonFlowResult implements RaoResultJsonUtils.ExtensionSerializer<FlowResult> {
    @Override
    public void serialize(FlowResult flowResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        flowResult.serialize(jsonGenerator);
    }

    @Override
    public FlowResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Version version = (Version) deserializationContext.getAttribute("version");
        if (version.major() == 1) {
            throw new OpenRaoException("Angle results extension is only available for JSON RAO Result versions >= 2.");
        }
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        FlowResult flowResult = new FlowResult();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnec flowCnec = null;
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                switch (jsonParser.currentName()) {
                    case "flowCnecId" -> {
                        jsonParser.nextToken();
                        flowCnec = crac.getFlowCnec(jsonParser.getValueAsString());
                    }
                    case "initial" -> deserializeForInstant(jsonParser, flowResult, flowCnec, null, crac);
                    default -> deserializeForInstant(jsonParser, flowResult, flowCnec, jsonParser.currentName(), crac);
                }
            }
        }
        return flowResult;
    }

    private static void deserializeForInstant(JsonParser jsonParser, FlowResult flowResult, FlowCnec flowCnec, String instantId, Crac crac) throws IOException {
        Instant instant = instantId == null ? null : crac.getInstant(instantId);
        jsonParser.nextToken();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "ampere" -> {
                    jsonParser.nextToken();
                    deserializeForUnit(jsonParser, flowResult, flowCnec, instant, Unit.AMPERE, crac);
                }
                case "megawatt" -> {
                    jsonParser.nextToken();
                    deserializeForUnit(jsonParser, flowResult, flowCnec, instant, Unit.MEGAWATT, crac);
                }
                default -> throw new OpenRaoException("Unsupported unit for flow values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    private static void deserializeForUnit(JsonParser jsonParser, FlowResult flowResult, FlowCnec flowCnec, Instant instant, Unit unit, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "margin", "relativeMargin" -> jsonParser.nextToken();
                case "side1" -> {
                    jsonParser.nextToken();
                    deserializeForSide(jsonParser, flowResult, flowCnec, instant, unit, TwoSides.ONE, crac);
                }
                case "side2" -> {
                    jsonParser.nextToken();
                    deserializeForSide(jsonParser, flowResult, flowCnec, instant, unit, TwoSides.TWO, crac);
                }
                default -> throw new OpenRaoException("Unsupported side for flow values: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    private static void deserializeForSide(JsonParser jsonParser, FlowResult flowResult, FlowCnec flowCnec, Instant instant, Unit unit, TwoSides side, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.currentName()) {
                case "flow" -> {
                    jsonParser.nextToken();
                    flowResult.addFlowMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side, unit);
                }
                case "commercialFlow" -> {
                    jsonParser.nextToken();
                    flowResult.addCommercialFlowMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side, unit);
                }
                case "loopFlow" -> jsonParser.nextToken();
                case "zonalPtdfSum" -> {
                    if (unit != Unit.MEGAWATT) {
                        throw new OpenRaoException("Zonal PTDF sum only defined for measurements in MW.");
                    }
                    jsonParser.nextToken();
                    flowResult.addPtdfZonalSumMeasurement(jsonParser.getDoubleValue(), instant, flowCnec, side);
                }
                default -> throw new OpenRaoException("Unsupported flow value: %s.".formatted(jsonParser.currentName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "flow-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super FlowResult> getExtensionClass() {
        return FlowResult.class;
    }
}
