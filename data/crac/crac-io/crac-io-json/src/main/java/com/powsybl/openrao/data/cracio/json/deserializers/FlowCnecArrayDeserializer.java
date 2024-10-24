/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class FlowCnecArrayDeserializer {

    private FlowCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", FLOW_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
            List<Extension<FlowCnec>> extensions = new ArrayList<>();
            Pair<Double, Double> nominalV = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        flowCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        flowCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case NETWORK_ELEMENT_ID:
                        readNetworkElementId(jsonParser, networkElementsNamesPerId, flowCnecAdder);
                        break;
                    case OPERATOR:
                        flowCnecAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case BORDER:
                        flowCnecAdder.withBorder(jsonParser.nextTextValue());
                        break;
                    case INSTANT:
                        flowCnecAdder.withInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY_ID:
                        flowCnecAdder.withContingency(jsonParser.nextTextValue());
                        break;
                    case OPTIMIZED:
                        flowCnecAdder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case MONITORED:
                        flowCnecAdder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case FRM:
                        readFrm(jsonParser, version, flowCnecAdder);
                        break;
                    case RELIABILITY_MARGIN:
                        readReliabilityMargin(jsonParser, version, flowCnecAdder);
                        break;
                    case I_MAX:
                        readImax(jsonParser, flowCnecAdder);
                        break;
                    case NOMINAL_VOLTAGE:
                        nominalV = readNominalVoltage(jsonParser, flowCnecAdder);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        BranchThresholdArrayDeserializer.deserialize(jsonParser, flowCnecAdder, nominalV, version);
                        break;
                    case EXTENSIONS:
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

    private static Pair<Double, Double> readNominalVoltage(JsonParser jsonParser, FlowCnecAdder flowCnecAdder) throws IOException {
        jsonParser.nextToken();
        Double[] nominalV = jsonParser.readValueAs(Double[].class);
        if (nominalV.length == 1) {
            flowCnecAdder.withNominalVoltage(nominalV[0]);
            return Pair.of(nominalV[0], nominalV[0]);
        } else if (nominalV.length == 2) {
            flowCnecAdder.withNominalVoltage(nominalV[0], TwoSides.ONE);
            flowCnecAdder.withNominalVoltage(nominalV[1], TwoSides.TWO);
            return Pair.of(nominalV[0], nominalV[1]);
        } else if (nominalV.length > 2) {
            throw new OpenRaoException("nominalVoltage array of a flowCnec cannot contain more than 2 values");
        }
        return null;
    }

    private static void readImax(JsonParser jsonParser, FlowCnecAdder flowCnecAdder) throws IOException {
        jsonParser.nextToken();
        Double[] iMax = jsonParser.readValueAs(Double[].class);
        if (iMax.length == 1) {
            flowCnecAdder.withIMax(iMax[0]);
        } else if (iMax.length == 2) {
            flowCnecAdder.withIMax(iMax[0], TwoSides.ONE);
            flowCnecAdder.withIMax(iMax[1], TwoSides.TWO);
        } else if (iMax.length > 2) {
            throw new OpenRaoException("iMax array of a flowCnec cannot contain more than 2 values");
        }
    }

    private static void readReliabilityMargin(JsonParser jsonParser, String version, FlowCnecAdder flowCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) <= 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, RELIABILITY_MARGIN));
        }
        jsonParser.nextToken();
        flowCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readFrm(JsonParser jsonParser, String version, FlowCnecAdder flowCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, FRM));
        }
        jsonParser.nextToken();
        flowCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, FlowCnecAdder flowCnecAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            flowCnecAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            flowCnecAdder.withNetworkElement(networkElementId);
        }
    }
}
