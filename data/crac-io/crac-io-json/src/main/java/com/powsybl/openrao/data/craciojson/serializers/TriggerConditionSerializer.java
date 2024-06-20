/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.cracapi.triggercondition.TriggerCondition;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CNEC_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.CONTINGENCY_ID;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.COUNTRY;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.USAGE_METHOD;
import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.serializeUsageMethod;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TriggerConditionSerializer extends AbstractJsonSerializer<TriggerCondition> {
    @Override
    public void serialize(TriggerCondition triggerCondition, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(INSTANT, triggerCondition.getInstant().getId());
        if (triggerCondition.getContingency().isPresent()) {
            jsonGenerator.writeStringField(CONTINGENCY_ID, triggerCondition.getContingency().get().getId());
        }
        if (triggerCondition.getCnec().isPresent()) {
            jsonGenerator.writeStringField(CNEC_ID, triggerCondition.getCnec().get().getId());
        }
        if (triggerCondition.getCountry().isPresent()) {
            jsonGenerator.writeStringField(COUNTRY, triggerCondition.getCountry().get().toString());
        }
        jsonGenerator.writeStringField(USAGE_METHOD, serializeUsageMethod(triggerCondition.getUsageMethod()));
        jsonGenerator.writeEndObject();
    }
}
