/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.serializers;

import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionSerializer extends AbstractJsonSerializer<CounterTradeRangeAction> {

    @Override
    public void serialize(CounterTradeRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializer.serializeCommon(value, gen);
        gen.writeStringField(JsonSerializationConstants.EXPORTING_COUNTRY, value.getExportingCountry().toString());
        gen.writeStringField(JsonSerializationConstants.IMPORTING_COUNTRY, value.getImportingCountry().toString());
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }
}
