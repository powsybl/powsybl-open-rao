/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.usagerule.OnConstraint;

import java.io.IOException;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class OnConstraintSerializer extends AbstractJsonSerializer<OnConstraint> {
    @Override
    public void serialize(OnConstraint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.INSTANT, value.getInstant().getId());
        gen.writeStringField(JsonSerializationConstants.CNEC_ID, value.getCnec().getId());
        gen.writeStringField(JsonSerializationConstants.USAGE_METHOD, JsonSerializationConstants.serializeUsageMethod(value.getUsageMethod()));
        gen.writeEndObject();
    }
}
