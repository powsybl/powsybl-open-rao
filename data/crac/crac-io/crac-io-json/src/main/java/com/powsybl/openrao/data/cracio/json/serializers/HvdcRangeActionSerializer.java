/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.NETWORK_ELEMENT_ID;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class HvdcRangeActionSerializer extends AbstractJsonSerializer<HvdcRangeAction> {
    @Override
    public void serialize(HvdcRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializer.serializeCommon(value, gen);
        gen.writeStringField(NETWORK_ELEMENT_ID, value.getNetworkElement().getId());
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }
}
