/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnecAdder;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class VoltageCnecArrayDeserializer {

    private VoltageCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", VOLTAGE_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            VoltageCnecAdder voltageCnecAdder = crac.newVoltageCnec();
            List<Extension<VoltageCnec>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        voltageCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        voltageCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case NETWORK_ELEMENT_ID:
                        readNetworkElementId(jsonParser, networkElementsNamesPerId, voltageCnecAdder);
                        break;
                    case OPERATOR:
                        voltageCnecAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case INSTANT:
                        voltageCnecAdder.withInstant(crac.getInstant(jsonParser.nextTextValue()));
                        break;
                    case CONTINGENCY_ID:
                        voltageCnecAdder.withContingency(jsonParser.nextTextValue());
                        break;
                    case OPTIMIZED:
                        voltageCnecAdder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case MONITORED:
                        voltageCnecAdder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case FRM:
                        readFrm(jsonParser, version, voltageCnecAdder);
                        break;
                    case RELIABILITY_MARGIN:
                        readReliabilityMargin(jsonParser, version, voltageCnecAdder);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        VoltageThresholdArrayDeserializer.deserialize(jsonParser, voltageCnecAdder);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in VoltageCnec: " + jsonParser.getCurrentName());
                }
            }
            VoltageCnec cnec = voltageCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void readReliabilityMargin(JsonParser jsonParser, String version, VoltageCnecAdder voltageCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) <= 3) {
            throw new FaraoException(String.format("Unexpected field for version %s : %s", version, RELIABILITY_MARGIN));
        }
        jsonParser.nextToken();
        voltageCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readFrm(JsonParser jsonParser, String version, VoltageCnecAdder voltageCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 3) {
            throw new FaraoException(String.format("Unexpected field for version %s : %s", version, FRM));
        }
        jsonParser.nextToken();
        voltageCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, VoltageCnecAdder voltageCnecAdder) throws IOException {
        String importingNetworkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(importingNetworkElementId)) {
            voltageCnecAdder.withNetworkElement(importingNetworkElementId, networkElementsNamesPerId.get(importingNetworkElementId));
        } else {
            voltageCnecAdder.withNetworkElement(importingNetworkElementId);
        }
    }
}
