/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.GeneratorAction;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
public class GeneratorActionSerializer extends AbstractJsonSerializer<GeneratorAction> {
    @Override
    public void serialize(GeneratorAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.NETWORK_ELEMENT_ID, value.getGeneratorId());
        gen.writeNumberField(JsonSerializationConstants.ACTIVE_POWER_VALUE, value.getActivePowerValue().getAsDouble());
        gen.writeEndObject();
    }
}
