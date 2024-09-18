/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracio.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.cracapi.threshold.VoltageThresholdAdder;
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.data.cracio.json.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class VoltageCnecArrayDeserializer {

    private VoltageCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", VOLTAGE_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        Set<VoltageThresholdArrayDeserializer.VoltageThreshold> thresholds = new HashSet<>();
        double reliabilityMargin = 0;
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
                    case BORDER:
                        voltageCnecAdder.withBorder(jsonParser.nextTextValue());
                        break;
                    case INSTANT:
                        voltageCnecAdder.withInstant(jsonParser.nextTextValue());
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
                        reliabilityMargin = readFrm(jsonParser, version);
                        break;
                    case RELIABILITY_MARGIN:
                        reliabilityMargin = readReliabilityMargin(jsonParser, version);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        thresholds = new HashSet<>(VoltageThresholdArrayDeserializer.deserialize(jsonParser, voltageCnecAdder));
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in VoltageCnec: " + jsonParser.getCurrentName());
                }
            }
            if (reliabilityMargin != 0) {
                // Workaround to support frm/reliability margin from older versions
                overrideThresholdsWithReliabilityMargin(voltageCnecAdder, thresholds, reliabilityMargin);
            }
            VoltageCnec cnec = voltageCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void overrideThresholdsWithReliabilityMargin(VoltageCnecAdder voltageCnecAdder, Set<VoltageThresholdArrayDeserializer.VoltageThreshold> thresholds, double reliabilityMargin) {
        thresholds.forEach(threshold -> {
            VoltageThresholdAdder thresholdAdder = voltageCnecAdder.newThreshold().withUnit(threshold.unit());
            if (threshold.min() != null) {
                thresholdAdder.withMin(threshold.min() + reliabilityMargin);
            }
            if (threshold.max() != null) {
                thresholdAdder.withMax(threshold.max() - reliabilityMargin);
            }
            thresholdAdder.add();
        });
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

    private static void readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, VoltageCnecAdder voltageCnecAdder) throws IOException {
        String importingNetworkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(importingNetworkElementId)) {
            voltageCnecAdder.withNetworkElement(importingNetworkElementId, networkElementsNamesPerId.get(importingNetworkElementId));
        } else {
            voltageCnecAdder.withNetworkElement(importingNetworkElementId);
        }
    }
}
