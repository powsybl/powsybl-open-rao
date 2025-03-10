/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class PhaseTapChangerTapPositionActionSerializer extends AbstractJsonSerializer<PhaseTapChangerTapPositionAction> {
    @Override
    public void serialize(PhaseTapChangerTapPositionAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField(JsonSerializationConstants.NETWORK_ELEMENT_ID, value.getTransformerId());
        gen.writeNumberField(JsonSerializationConstants.TAP_POSITION, value.getTapPosition());
        gen.writeEndObject();
    }
}
