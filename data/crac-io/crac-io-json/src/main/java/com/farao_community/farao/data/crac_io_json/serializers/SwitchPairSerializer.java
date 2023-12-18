/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_io_json.serializers;

import com.powsybl.open_rao.data.crac_api.network_action.SwitchPair;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.open_rao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class SwitchPairSerializer extends AbstractJsonSerializer<SwitchPair> {
    @Override
    public void serialize(SwitchPair value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(OPEN_ACTION, value.getSwitchToOpen().getId());
        gen.writeStringField(CLOSE_ACTION, value.getSwitchToClose().getId());
        gen.writeEndObject();
    }
}
