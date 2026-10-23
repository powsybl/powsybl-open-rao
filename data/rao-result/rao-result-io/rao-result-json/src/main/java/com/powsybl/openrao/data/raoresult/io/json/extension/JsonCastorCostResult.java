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
import com.powsybl.openrao.commons.Version;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonCastorCostResult implements RaoResultJsonUtils.ExtensionSerializer<CostResult> {
    @Override
    public void serialize(CostResult costResult, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        costResult.serialize(jsonGenerator);
    }

    @Override
    public CostResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        Version version = (Version) deserializationContext.getAttribute("version");
        if (version.major() == 1) {
            throw new OpenRaoException("Cost results extension is only available for JSON RAO Result versions >= 2.");
        }
        Crac crac = (Crac) deserializationContext.getAttribute("crac");
        CostResult costResult = new CostResult();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            if ("initial".equals(jsonParser.currentName())) {
                deserializeElementaryCostResult(jsonParser, null, costResult);
            } else {
                Instant instant = crac.getInstant(jsonParser.currentName());
                deserializeElementaryCostResult(jsonParser, instant, costResult);
            }
        }
        return costResult;
    }

    private void deserializeElementaryCostResult(JsonParser jsonParser, Instant instant, CostResult costResult) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            jsonParser.nextToken();
            if ("functionalCost".equals(jsonParser.currentName())) {
                costResult.addFunctionalCostResult(instant, jsonParser.getDoubleValue());
            } else if ("virtualCost".equals(jsonParser.currentName())) {
                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    jsonParser.nextToken();
                    costResult.addVirtualCostResult(instant, jsonParser.currentName(), jsonParser.getDoubleValue());
                }
            } else {
                throw new OpenRaoException("Unexpected field in '%s': '%s'.".formatted(jsonParser.currentName(), getExtensionName()));
            }
        }
    }

    @Override
    public String getExtensionName() {
        return "cost-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super CostResult> getExtensionClass() {
        return CostResult.class;
    }
}
