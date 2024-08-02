/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.usagerule.OnContingencyState;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class OnStateSerializer extends AbstractJsonSerializer<OnContingencyState> {
    @Override
    public void serialize(OnContingencyState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(INSTANT, value.getInstant().getId());
        if (!value.getInstant().isPreventive()) {
            gen.writeStringField(CONTINGENCY_ID, value.getContingency().getId());
        }
        gen.writeStringField(USAGE_METHOD, serializeUsageMethod(value.getUsageMethod()));
        gen.writeEndObject();
    }
}
