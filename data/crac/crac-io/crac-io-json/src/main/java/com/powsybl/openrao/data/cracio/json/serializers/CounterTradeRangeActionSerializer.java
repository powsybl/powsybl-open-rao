/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.cracio.json.serializers;

import com.powsybl.openrao.data.cracapi.rangeaction.CounterTradeRangeAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.EXPORTING_COUNTRY;
import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.IMPORTING_COUNTRY;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public class CounterTradeRangeActionSerializer extends AbstractJsonSerializer<CounterTradeRangeAction> {

    @Override
    public void serialize(CounterTradeRangeAction value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        StandardRangeActionSerializer.serializeCommon(value, gen);
        gen.writeStringField(EXPORTING_COUNTRY, value.getExportingCountry().toString());
        gen.writeStringField(IMPORTING_COUNTRY, value.getImportingCountry().toString());
        serializeRemedialActionSpeed(value, gen);
        gen.writeEndObject();
    }
}
