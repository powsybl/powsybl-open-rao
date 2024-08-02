/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.Instant;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class InstantSerializer extends AbstractJsonSerializer<Instant> {

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(ID, value.getId());

        gen.writeStringField(INSTANT_KIND, seralizeInstantKind(value.getKind()));

        gen.writeEndObject();
    }
}
