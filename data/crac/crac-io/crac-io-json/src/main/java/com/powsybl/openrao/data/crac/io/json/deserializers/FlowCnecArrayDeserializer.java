/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmCnecElementHelper;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.*;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class FlowCnecArrayDeserializer {

    private FlowCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.FLOW_CNECS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
            String networkElementId = null;
            List<Extension<FlowCnec>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.ID:
                        flowCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NAME:
                        flowCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NETWORK_ELEMENT_ID:
                        networkElementId = readNetworkElementId(jsonParser, networkElementsNamesPerId, flowCnecAdder);
                        break;
                    case JsonSerializationConstants.OPERATOR:
                        flowCnecAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.BORDER:
                        flowCnecAdder.withBorder(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.INSTANT:
                        flowCnecAdder.withInstant(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.CONTINGENCY_ID:
                        flowCnecAdder.withContingency(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.OPTIMIZED:
                        flowCnecAdder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case JsonSerializationConstants.MONITORED:
                        flowCnecAdder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case JsonSerializationConstants.FRM:
                        readFrm(jsonParser, version, flowCnecAdder);
                        break;
                    case JsonSerializationConstants.RELIABILITY_MARGIN:
                        readReliabilityMargin(jsonParser, version, flowCnecAdder);
                        break;
                    case JsonSerializationConstants.I_MAX:
                        jsonParser.nextToken();
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) == 1
                            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) <= 7) {
                            jsonParser.readValueAs(Double[].class);
                            BUSINESS_WARNS.warn("The iMax is now fetched in the network so the value in the CRAC will not be read.");
                            break;
                        }
                        throw new OpenRaoException("From version 2.8 onwards, iMax is deprecated and is read from the network.");
                    case JsonSerializationConstants.NOMINAL_VOLTAGE:
                        jsonParser.nextToken();
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) == 1
                            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) <= 7) {
                            jsonParser.readValueAs(Double[].class);
                            BUSINESS_WARNS.warn("The nominalV is now fetched in the network so the value in the CRAC will not be read.");
                            break;
                        }
                        throw new OpenRaoException("From version 2.8 onwards, nominalV is deprecated and is read from the network.");
                    case JsonSerializationConstants.THRESHOLDS:
                        jsonParser.nextToken();
                        IidmCnecElementHelper cnecElementHelper = new IidmCnecElementHelper(networkElementId, network);
                        BranchThresholdArrayDeserializer.deserialize(jsonParser, flowCnecAdder, cnecElementHelper, version);
                        break;
                    case JsonSerializationConstants.EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in FlowCnec: " + jsonParser.getCurrentName());
                }
            }
            FlowCnec cnec = flowCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void readReliabilityMargin(JsonParser jsonParser, String version, FlowCnecAdder flowCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 && JsonSerializationConstants.getSubVersionNumber(version) <= 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, JsonSerializationConstants.RELIABILITY_MARGIN));
        }
        jsonParser.nextToken();
        flowCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readFrm(JsonParser jsonParser, String version, FlowCnecAdder flowCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, JsonSerializationConstants.FRM));
        }
        jsonParser.nextToken();
        flowCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static String readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, FlowCnecAdder flowCnecAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            flowCnecAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            flowCnecAdder.withNetworkElement(networkElementId);
        }
        return networkElementId;
    }
}
