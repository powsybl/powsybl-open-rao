/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class InjectionRangeActionArrayDeserializer {
    private InjectionRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", INJECTION_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction();

            while (!jsonParser.nextToken().isStructEnd()) {
                if (StandardRangeActionDeserializer.addCommonElement(injectionRangeActionAdder, jsonParser, version)) {
                    continue;
                }
                if (jsonParser.getCurrentName().equals(NETWORK_ELEMENT_IDS_AND_KEYS)) {
                    jsonParser.nextToken();
                    deserializeInjectionDistributionKeys(jsonParser, injectionRangeActionAdder, networkElementsNamesPerId);
                } else {
                    throw new OpenRaoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                injectionRangeActionAdder.withInitialSetpoint(0);
            }
            injectionRangeActionAdder.add();
        }
    }

    private static void deserializeInjectionDistributionKeys(JsonParser jsonParser, InjectionRangeActionAdder adder, Map<String, String> networkElementsNamesPerId) throws IOException {

        while (!jsonParser.nextToken().isStructEnd()) {
            String networkElementId = jsonParser.getCurrentName();
            jsonParser.nextToken();
            double key = jsonParser.getDoubleValue();
            if (networkElementsNamesPerId.containsKey(networkElementId)) {
                adder.withNetworkElementAndKey(key, networkElementId, networkElementsNamesPerId.get(networkElementId));
            } else {
                adder.withNetworkElementAndKey(key, networkElementId);
            }
        }
    }
}
