/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.threshold.BranchThresholdAdder;
import com.powsybl.openrao.data.craciojson.ExtensionsHandler;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

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
        Set<BranchThresholdArrayDeserializer.BranchThreshold> thresholds = new HashSet<>();
        double reliabilityMargin = 0;
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
            List<Extension<FlowCnec>> extensions = new ArrayList<>();
            Pair<Double, Double> nominalV = null;
            Pair<Double, Double> iMax = null;
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
                        reliabilityMargin = readFrm(jsonParser, version);
                        break;
                    case RELIABILITY_MARGIN:
                        reliabilityMargin = readReliabilityMargin(jsonParser, version);
                        break;
                    case I_MAX:
                        iMax = readImax(jsonParser, flowCnecAdder);
                        break;
                    case NOMINAL_VOLTAGE:
                        nominalV = readNominalVoltage(jsonParser, flowCnecAdder);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        thresholds = new HashSet<>(BranchThresholdArrayDeserializer.deserialize(jsonParser, flowCnecAdder, nominalV, version));
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in FlowCnec: " + jsonParser.getCurrentName());
                }
            }
            if (reliabilityMargin != 0) {
                // Workaround to support frm/reliability margin from older versions
                overrideThresholdsWithReliabilityMargin(flowCnecAdder, thresholds, reliabilityMargin, nominalV, iMax);
            }
            FlowCnec cnec = flowCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void overrideThresholdsWithReliabilityMargin(FlowCnecAdder flowCnecAdder, Set<BranchThresholdArrayDeserializer.BranchThreshold> thresholds, double reliabilityMargin, Pair<Double, Double> nominalV, Pair<Double, Double> iMax) {
        Pair<Double, Double> actualNominalV = nominalV == null ? Pair.of(null, null) : nominalV;
        Pair<Double, Double> actualIMax = iMax == null ? Pair.of(null, null) : iMax;
        thresholds.forEach(threshold -> {
            double reliabilityMarginInTargetUnit = convertReliabilityMarginInTargetUnit(reliabilityMargin, threshold.unit(), Side.LEFT.equals(threshold.side()) ? actualNominalV.getLeft() : actualNominalV.getRight(), Side.LEFT.equals(threshold.side()) ? actualIMax.getLeft() : actualIMax.getRight());
            BranchThresholdAdder thresholdAdder = flowCnecAdder.newThreshold().withUnit(threshold.unit()).withSide(threshold.side());
            if (threshold.min() != null) {
                thresholdAdder.withMin(threshold.min() + reliabilityMarginInTargetUnit);
            }
            if (threshold.max() != null) {
                thresholdAdder.withMax(threshold.max() - reliabilityMarginInTargetUnit);
            }
            thresholdAdder.add();
        });
    }

    private static double convertReliabilityMarginInTargetUnit(double reliabilityMargin, Unit targetUnit, Double nominalVoltage, Double iMax) {
        // Reliability margin is always in MW
        if (Unit.MEGAWATT.equals(targetUnit)) {
            return reliabilityMargin;
        } else {
            if (nominalVoltage == null) {
                throw new OpenRaoException("Undefined nominal voltage");
            }
            double reliabilityMarginInAmpere = reliabilityMargin * 1000 / (nominalVoltage * Math.sqrt(3));
            if (Unit.AMPERE.equals(targetUnit)) {
                return reliabilityMarginInAmpere;
            } else if (Unit.PERCENT_IMAX.equals(targetUnit)) {
                if (iMax == null) {
                    throw new OpenRaoException("Undefined iMax");
                }
                return reliabilityMarginInAmpere / iMax;
            } else {
                throw new OpenRaoException("Unsupported branch threshold unit %s".formatted(targetUnit));
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
            flowCnecAdder.withNominalVoltage(nominalV[0], Side.LEFT);
            flowCnecAdder.withNominalVoltage(nominalV[1], Side.RIGHT);
            return Pair.of(nominalV[0], nominalV[1]);
        } else if (nominalV.length > 2) {
            throw new OpenRaoException("nominalVoltage array of a flowCnec cannot contain more than 2 values");
        }
        return null;
    }

    private static Pair<Double, Double> readImax(JsonParser jsonParser, FlowCnecAdder flowCnecAdder) throws IOException {
        jsonParser.nextToken();
        Double[] iMax = jsonParser.readValueAs(Double[].class);
        if (iMax.length == 1) {
            flowCnecAdder.withIMax(iMax[0]);
            return Pair.of(iMax[0], iMax[0]);
        } else if (iMax.length == 2) {
            flowCnecAdder.withIMax(iMax[0], Side.LEFT);
            flowCnecAdder.withIMax(iMax[1], Side.RIGHT);
            return Pair.of(iMax[0], iMax[1]);
        } else if (iMax.length > 2) {
            throw new OpenRaoException("iMax array of a flowCnec cannot contain more than 2 values");
        }
        return null;
    }

    private static double readReliabilityMargin(JsonParser jsonParser, String version) throws IOException {
        CnecDeserializerUtils.checkReliabilityMargin(version);
        jsonParser.nextToken();
        return jsonParser.getDoubleValue();
    }

    private static double readFrm(JsonParser jsonParser, String version) throws IOException {
        CnecDeserializerUtils.checkFrm(version);
        jsonParser.nextToken();
        return jsonParser.getDoubleValue();
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
