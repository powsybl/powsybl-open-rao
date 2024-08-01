/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.cracapi.usagerule.OnConstraint;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.CNEC_ID;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.INSTANT;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.USAGE_METHOD;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.serializeUsageMethod;

/**
 * @author Thomas Bouquet <thomas.bouquet at rte-france.com>
 */
public class OnConstraintSerializer extends AbstractJsonSerializer<OnConstraint> {
    @Override
    public void serialize(OnConstraint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(INSTANT, value.getInstant().getId());
        gen.writeStringField(CNEC_ID, value.getCnec().getId());
        gen.writeStringField(USAGE_METHOD, serializeUsageMethod(value.getUsageMethod()));
        gen.writeEndObject();
    }
}
