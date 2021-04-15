/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_io_json.serializers;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Objects;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationNames.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class NetworkElementSerializer extends AbstractJsonSerializer<NetworkElement> {

    @Override
    public void serialize(NetworkElement value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStringField(ID, value.getId());
        if (!Objects.isNull(value.getName()) && !value.getName().equals(value.getId())) {
            gen.writeStringField(NAME, value.getName());
        }
    }
}
