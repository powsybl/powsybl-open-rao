/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.range.TapRange;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class TapRangeSerializer extends AbstractJsonSerializer<TapRange> {
    @Override
    public void serialize(TapRange value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (value.getMinTap() > Integer.MIN_VALUE) {
            gen.writeNumberField(MIN, value.getMinTap());
        }
        if (value.getMaxTap() < Integer.MAX_VALUE) {
            gen.writeNumberField(MAX, value.getMaxTap());
        }
        gen.writeStringField(RANGE_TYPE, serializeRangeType(value.getRangeType()));
        gen.writeEndObject();
    }
}
