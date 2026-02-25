/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.AngleThresholdAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class AngleThresholdArrayDeserializer {

    private AngleThresholdArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, AngleCnecAdder ownerAdder) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleThresholdAdder angleThresholdAdder = ownerAdder.newThreshold();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.UNIT:
                        angleThresholdAdder.withUnit(JsonSerializationConstants.deserializeUnit(jsonParser.nextTextValue()));
                        break;
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        angleThresholdAdder.withMin(jsonParser.getDoubleValue());
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        angleThresholdAdder.withMax(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Threshold: " + jsonParser.getCurrentName());
                }
            }
            angleThresholdAdder.add();
        }
    }
}
