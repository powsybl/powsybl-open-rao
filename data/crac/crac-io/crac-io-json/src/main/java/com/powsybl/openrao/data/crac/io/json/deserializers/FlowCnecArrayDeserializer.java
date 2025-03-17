/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.apache.commons.lang3.tuple.Pair;

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
            List<Extension<FlowCnec>> extensions = new ArrayList<>();
            Pair<Double, Double> nominalV = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.ID:
                        flowCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NAME:
                        flowCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NETWORK_ELEMENT_ID:
                        String networkElementId = readNetworkElementId(jsonParser, networkElementsNamesPerId, flowCnecAdder);
                        nominalV = readNominalVoltage(flowCnecAdder, network, networkElementId);
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
                        readImax(jsonParser, flowCnecAdder);
                        break;
                    case JsonSerializationConstants.NOMINAL_VOLTAGE:
                        readNominalVoltage(jsonParser, flowCnecAdder);
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 ||
                            JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) <= 6) {
                            BUSINESS_WARNS.warn("The nominal voltage is now read from the network so the value in the crac will not be read");
                        }
                        break;
                    case JsonSerializationConstants.THRESHOLDS:
                        jsonParser.nextToken();
                        BranchThresholdArrayDeserializer.deserialize(jsonParser, flowCnecAdder, nominalV, version);
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

    private static Pair<Double, Double> readNominalVoltage(FlowCnecAdder flowCnecAdder, Network network, String networkElementId) {
        Identifiable<?> networkElement = network.getIdentifiable(networkElementId);
        if (networkElement == null) {
            return null;
        }
        if (networkElement instanceof Branch<?> branch) {
            return getNominalVoltages(branch, flowCnecAdder);
        }
        if (networkElement instanceof DanglingLine danglingLine) {
            return getNominalVoltages(danglingLine, flowCnecAdder);
        }
        return null;
    }

    private static Pair<Double, Double> getNominalVoltages(Branch<?> branch, FlowCnecAdder flowCnecAdder) {
        double nominalVoltage1 = branch.getTerminal(TwoSides.ONE).getVoltageLevel().getNominalV();
        double nominalVoltage2 = branch.getTerminal(TwoSides.TWO).getVoltageLevel().getNominalV();
        flowCnecAdder.withNominalVoltage(nominalVoltage1, TwoSides.ONE);
        flowCnecAdder.withNominalVoltage(nominalVoltage2, TwoSides.TWO);
        return Pair.of(nominalVoltage1, nominalVoltage2);
    }

    private static Pair<Double, Double> getNominalVoltages(DanglingLine danglingLine, FlowCnecAdder flowCnecAdder) {
        double nominalVoltage = danglingLine.getTerminal().getVoltageLevel().getNominalV();
        flowCnecAdder.withNominalVoltage(nominalVoltage);
        return Pair.of(nominalVoltage, nominalVoltage);
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
