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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnecAdder;
import com.powsybl.openrao.data.crac.api.threshold.VoltageThresholdAdder;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class VoltageThresholdArrayDeserializer {

    private VoltageThresholdArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, VoltageCnecAdder ownerAdder, String version) throws IOException {
        boolean hasMin = false;
        boolean hasMax = false;
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            VoltageThresholdAdder voltageThresholdAdder = ownerAdder.newThreshold();
            boolean currentHasMin = false;
            boolean currentHasMax = false;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.currentName()) {
                    case JsonSerializationConstants.UNIT:
                        voltageThresholdAdder.withUnit(JsonSerializationConstants.deserializeUnit(jsonParser.nextTextValue()));
                        break;
                    case JsonSerializationConstants.MIN:
                        jsonParser.nextToken();
                        voltageThresholdAdder.withMin(jsonParser.getDoubleValue());
                        currentHasMin = true;
                        break;
                    case JsonSerializationConstants.MAX:
                        jsonParser.nextToken();
                        voltageThresholdAdder.withMax(jsonParser.getDoubleValue());
                        currentHasMax = true;
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in Threshold: " + jsonParser.currentName());
                }
            }

            if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2
                || JsonSerializationConstants.getSubVersionNumber(version) > 12) {
                checkForDuplicateThresholds(hasMin, hasMax, currentHasMin, currentHasMax);
                hasMin = hasMin || currentHasMin;
                hasMax = hasMax || currentHasMax;
            }

            voltageThresholdAdder.add();
        }
    }

    static void checkForDuplicateThresholds(boolean hasMin,
                                            boolean hasMax,
                                            boolean currentHasMin,
                                            boolean currentHasMax) {

        if (hasMin && currentHasMin) {
            throw new OpenRaoException("Min threshold can only be defined once since CRAC v2.13");
        }

        if (hasMax && currentHasMax) {
            throw new OpenRaoException("Max threshold can only be defined once since CRAC v2.13");
        }

    }
}
