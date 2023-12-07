/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class HvdcRangeActionArrayDeserializer {
    private HvdcRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", HVDC_RANGE_ACTIONS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                if (StandardRangeActionDeserializer.addCommonElement(hvdcRangeActionAdder, jsonParser, version)) {
                    continue;
                }
                if (jsonParser.getCurrentName().equals(NETWORK_ELEMENT_ID)) {
                    readNetworkElementId(jsonParser, networkElementsNamesPerId, hvdcRangeActionAdder);
                } else {
                    throw new FaraoException("Unexpected field in HvdcRangeAction: " + jsonParser.getCurrentName());
                }
            }
            if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) < 3) {
                // initial setpoint was not exported then, set default value to 0 to avoid errors
                hvdcRangeActionAdder.withInitialSetpoint(0);
            }
            hvdcRangeActionAdder.add();
        }
    }

    private static void readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, HvdcRangeActionAdder hvdcRangeActionAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            hvdcRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            hvdcRangeActionAdder.withNetworkElement(networkElementId);
        }
    }
}
