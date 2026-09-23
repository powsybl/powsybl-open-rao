/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.range.ConnectedArea;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
public class ConnectedAreaSerializer extends AbstractJsonSerializer<ConnectedArea> {

    @Override
    public void serialize(ConnectedArea value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.AREA, value.getArea());
        gen.writeArrayFieldStart(JsonSerializationConstants.BORDER_RANGES);
        for (StandardRange borderRange : value.getBorderRanges()) {
            gen.writeObject(borderRange);
        }
        gen.writeEndArray();
        gen.writeEndObject();
    }
}