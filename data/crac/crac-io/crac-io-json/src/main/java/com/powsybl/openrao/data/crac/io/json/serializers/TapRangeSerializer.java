/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.range.TapRange;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TapRangeSerializer extends AbstractJsonSerializer<TapRange> {
    @Override
    public void serialize(TapRange value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getMinTap() > Integer.MIN_VALUE) {
            gen.writeNumberField(JsonSerializationConstants.MIN, value.getMinTap());
        }
        if (value.getMaxTap() < Integer.MAX_VALUE) {
            gen.writeNumberField(JsonSerializationConstants.MAX, value.getMaxTap());
        }
        gen.writeStringField(JsonSerializationConstants.RANGE_TYPE, JsonSerializationConstants.serializeRangeType(value.getRangeType()));
        gen.writeEndObject();
    }
}
