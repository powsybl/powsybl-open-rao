/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionSerializer extends AbstractJsonSerializer<CounterTradeRangeAction> {

    @Override
    public void serialize(CounterTradeRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializer.serializeCommon(value, gen);
        gen.writeStringField(JsonSerializationConstants.AREA, value.getArea());
        if (value.getInitialNetPosition() != null) {
            gen.writeNumberField(JsonSerializationConstants.INITIAL_NET_POSITION, value.getInitialNetPosition());
        }
        serializeConnectedAreas(value, gen);
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }

    private static void serializeConnectedAreas(CounterTradeRangeAction value, JsonGenerator gen) throws IOException {
        gen.writeArrayFieldStart(JsonSerializationConstants.CONNECTED_AREAS);
        for (ConnectedArea connectedArea : value.getConnectedAreas()) {
            gen.writeStartObject();
            gen.writeStringField(JsonSerializationConstants.AREA, connectedArea.getArea());
            gen.writeArrayFieldStart(JsonSerializationConstants.BORDER_RANGES);
            for (StandardRange borderRange : connectedArea.getBorderRanges()) {
                gen.writeObject(borderRange);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
