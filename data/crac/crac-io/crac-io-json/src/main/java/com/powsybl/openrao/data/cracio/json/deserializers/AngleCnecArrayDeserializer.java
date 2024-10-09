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
import com.powsybl.openrao.data.cracio.json.ExtensionsHandler;
import com.powsybl.openrao.data.cracio.json.JsonSerializationConstants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public final class AngleCnecArrayDeserializer {

    private AngleCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.ANGLE_CNECS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            AngleCnecAdder angleCnecAdder = crac.newAngleCnec();
            List<Extension<AngleCnec>> extensions = new ArrayList<>();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.ID:
                        angleCnecAdder.withId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NAME:
                        angleCnecAdder.withName(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.EXPORTING_NETWORK_ELEMENT_ID:
                        readExportingNetworkElementId(jsonParser, networkElementsNamesPerId, angleCnecAdder);
                        break;
                    case JsonSerializationConstants.IMPORTING_NETWORK_ELEMENT_ID:
                        readImportingNetworkElementId(jsonParser, networkElementsNamesPerId, angleCnecAdder);
                        break;
                    case JsonSerializationConstants.OPERATOR:
                        angleCnecAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.BORDER:
                        angleCnecAdder.withBorder(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.INSTANT:
                        angleCnecAdder.withInstant(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.CONTINGENCY_ID:
                        angleCnecAdder.withContingency(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.OPTIMIZED:
                        angleCnecAdder.withOptimized(jsonParser.nextBooleanValue());
                        break;
                    case JsonSerializationConstants.MONITORED:
                        angleCnecAdder.withMonitored(jsonParser.nextBooleanValue());
                        break;
                    case JsonSerializationConstants.FRM:
                        readFrm(jsonParser, version, angleCnecAdder);
                        break;
                    case JsonSerializationConstants.RELIABILITY_MARGIN:
                        readReliabilityMargin(jsonParser, version, angleCnecAdder);
                        break;
                    case JsonSerializationConstants.THRESHOLDS:
                        jsonParser.nextToken();
                        AngleThresholdArrayDeserializer.deserialize(jsonParser, angleCnecAdder);
                        break;
                    case JsonSerializationConstants.EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in AngleCnec: " + jsonParser.getCurrentName());
                }
            }
            AngleCnec cnec = angleCnecAdder.add();
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(cnec, extensions);
            }
        }
    }

    private static void readReliabilityMargin(JsonParser jsonParser, String version, AngleCnecAdder angleCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) <= 1 && JsonSerializationConstants.getSubVersionNumber(version) <= 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, JsonSerializationConstants.RELIABILITY_MARGIN));
        }
        jsonParser.nextToken();
        angleCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readFrm(JsonParser jsonParser, String version, AngleCnecAdder angleCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 3) {
            throw new OpenRaoException(String.format("Unexpected field for version %s : %s", version, JsonSerializationConstants.FRM));
        }
        jsonParser.nextToken();
        angleCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
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
