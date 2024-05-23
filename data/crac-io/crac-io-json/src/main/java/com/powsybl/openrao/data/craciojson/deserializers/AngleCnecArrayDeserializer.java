/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.*;
import com.powsybl.openrao.data.cracapi.threshold.AngleThresholdAdder;
import com.powsybl.openrao.data.cracapi.threshold.VoltageThresholdAdder;
import com.powsybl.openrao.data.craciojson.ExtensionsHandler;
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

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class AngleCnecArrayDeserializer {

    private AngleCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", ANGLE_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        Set<AngleThresholdArrayDeserializer.AngleThreshold> thresholds = new HashSet<>();
        double reliabilityMargin = 0;
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleCnecAdder angleCnecAdder = crac.newAngleCnec();
            List<Extension<AngleCnec>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case ID:
                        angleCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case NAME:
                        angleCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case EXPORTING_NETWORK_ELEMENT_ID:
                        readExportingNetworkElementId(jsonParser, networkElementsNamesPerId, angleCnecAdder);
                        break;
                    case IMPORTING_NETWORK_ELEMENT_ID:
                        readImportingNetworkElementId(jsonParser, networkElementsNamesPerId, angleCnecAdder);
                        break;
                    case OPERATOR:
                        angleCnecAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case BORDER:
                        angleCnecAdder.withBorder(jsonParser.nextTextValue());
                        break;
                    case INSTANT:
                        angleCnecAdder.withInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY_ID:
                        angleCnecAdder.withContingency(jsonParser.nextTextValue());
                        break;
                    case OPTIMIZED:
                        angleCnecAdder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case MONITORED:
                        angleCnecAdder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case FRM:
                        reliabilityMargin = readFrm(jsonParser, version, angleCnecAdder);
                        break;
                    case RELIABILITY_MARGIN:
                        reliabilityMargin = readReliabilityMargin(jsonParser, version, angleCnecAdder);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        thresholds = new HashSet<>(AngleThresholdArrayDeserializer.deserialize(jsonParser, angleCnecAdder));
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in AngleCnec: " + jsonParser.getCurrentName());
                }
            }
            if (reliabilityMargin != 0) {
                // Workaround to support frm/reliability margin from older versions
                overrideThresholdsWithReliabilityMargin(angleCnecAdder, thresholds, reliabilityMargin);
            }
            AngleCnec cnec = angleCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void overrideThresholdsWithReliabilityMargin(AngleCnecAdder angleCnecAdder, Set<AngleThresholdArrayDeserializer.AngleThreshold> thresholds, double reliabilityMargin) {
        thresholds.forEach(threshold -> {
            AngleThresholdAdder thresholdAdder = angleCnecAdder.newThreshold().withUnit(threshold.unit());
            if (threshold.min() != null) {
                thresholdAdder.withMin(threshold.min() + reliabilityMargin);
            }
            if (threshold.max() != null) {
                thresholdAdder.withMax(threshold.max() - reliabilityMargin);
            }
            thresholdAdder.add();
        });
    }

    private static double readReliabilityMargin(JsonParser jsonParser, String version, AngleCnecAdder angleCnecAdder) throws IOException {
        CnecDeserializerUtils.checkReliabilityMargin(version);
        jsonParser.nextToken();
        return jsonParser.getDoubleValue();
    }

    private static double readFrm(JsonParser jsonParser, String version, AngleCnecAdder angleCnecAdder) throws IOException {
        CnecDeserializerUtils.checkFrm(version);
        jsonParser.nextToken();
        return jsonParser.getDoubleValue();
    }

    private static void readImportingNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, AngleCnecAdder angleCnecAdder) throws IOException {
        String importingNetworkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(importingNetworkElementId)) {
            angleCnecAdder.withImportingNetworkElement(importingNetworkElementId, networkElementsNamesPerId.get(importingNetworkElementId));
        } else {
            angleCnecAdder.withImportingNetworkElement(importingNetworkElementId);
        }
    }

    private static void readExportingNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, AngleCnecAdder angleCnecAdder) throws IOException {
        String exportingNetworkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(exportingNetworkElementId)) {
            angleCnecAdder.withExportingNetworkElement(exportingNetworkElementId, networkElementsNamesPerId.get(exportingNetworkElementId));
        } else {
            angleCnecAdder.withExportingNetworkElement(exportingNetworkElementId);
        }
    }
}
