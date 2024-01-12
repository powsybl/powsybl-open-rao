/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.serializers;

import com.powsybl.openrao.data.cracapi.usagerule.OnFlowConstraint;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnFlowConstraintSerializer extends AbstractJsonSerializer<OnFlowConstraint> {
    @Override
    public void serialize(OnFlowConstraint value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(INSTANT, value.getInstant().getId());
        gen.writeStringField(FLOW_CNEC_ID, value.getFlowCnec().getId());
        gen.writeStringField(USAGE_METHOD, serializeUsageMethod(value.getUsageMethod()));
        gen.writeEndObject();
    }
}
