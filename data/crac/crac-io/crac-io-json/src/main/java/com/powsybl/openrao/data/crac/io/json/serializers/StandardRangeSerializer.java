/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class StandardRangeSerializer extends AbstractJsonSerializer<StandardRange> {
    @Override
    public void serialize(StandardRange value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getMin() > Integer.MIN_VALUE) {
            gen.writeNumberField(JsonSerializationConstants.MIN, value.getMin());
        }
        if (value.getMax() < Integer.MAX_VALUE) {
            gen.writeNumberField(JsonSerializationConstants.MAX, value.getMax());
        }
        gen.writeEndObject();
    }
}
