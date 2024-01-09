/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.open_rao.data.crac_io_json.deserializers;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.cnec.VoltageCnecAdder;
import com.powsybl.open_rao.data.crac_api.threshold.VoltageThresholdAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.powsybl.open_rao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class VoltageThresholdArrayDeserializer {

    private VoltageThresholdArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, VoltageCnecAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            VoltageThresholdAdder voltageThresholdAdder = ownerAdder.newThreshold();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case UNIT:
                        voltageThresholdAdder.withUnit(deserializeUnit(jsonParser.nextTextValue()));
                        break;
                    case MIN:
                        jsonParser.nextToken();
                        voltageThresholdAdder.withMin(jsonParser.getDoubleValue());
                        break;
                    case MAX:
                        jsonParser.nextToken();
                        voltageThresholdAdder.withMax(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Threshold: " + jsonParser.getCurrentName());
                }
            }
            voltageThresholdAdder.add();
        }
    }
}
