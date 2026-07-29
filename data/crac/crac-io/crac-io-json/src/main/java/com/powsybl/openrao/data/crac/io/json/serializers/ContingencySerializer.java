/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class ContingencySerializer extends AbstractJsonSerializer<Contingency> {

    @Override
    public void serialize(Contingency value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeStringField(JsonSerializationConstants.ID, value.getId());

        Optional<String> name = value.getName();
        if (name.isPresent()) {
            gen.writeStringField(JsonSerializationConstants.NAME, name.get());
        }

        gen.writeArrayFieldStart(JsonSerializationConstants.NETWORK_ELEMENTS_IDS);
        for (ContingencyElement networkElement : value.getElements()) {
            gen.writeString(networkElement.getId());
        }
        gen.writeEndArray();

        gen.writeEndObject();
    }
}
