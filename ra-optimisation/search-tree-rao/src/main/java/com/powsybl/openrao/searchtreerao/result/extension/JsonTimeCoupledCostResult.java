/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.result.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.openrao.data.raoresult.api.extension.CostResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonUtils;
import com.powsybl.openrao.data.raoresult.io.json.extension.JsonCastorCostResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@AutoService(RaoResultJsonUtils.ExtensionSerializer.class)
public class JsonTimeCoupledCostResult implements RaoResultJsonUtils.ExtensionSerializer<TimeCoupledCostResult> {
    @Override
    public void serialize(TimeCoupledCostResult extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        extension.serialize(jsonGenerator);
    }

    @Override
    public TimeCoupledCostResult deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        TimeCoupledCostResult timeCoupledCostResult = new TimeCoupledCostResult();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            OffsetDateTime timestamp = OffsetDateTime.parse(jsonParser.currentName(), DateTimeFormatter.ISO_DATE_TIME);
            jsonParser.nextToken();
            CostResult costResult = new JsonCastorCostResult().deserialize(jsonParser, deserializationContext);
            timeCoupledCostResult.add(costResult, timestamp);
        }
        return timeCoupledCostResult;
    }

    @Override
    public String getExtensionName() {
        return "time-coupled-cost-results";
    }

    @Override
    public String getCategoryName() {
        return "rao-result";
    }

    @Override
    public Class<? super TimeCoupledCostResult> getExtensionClass() {
        return TimeCoupledCostResult.class;
    }
}
