/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
import com.farao_community.farao.data.crac_api.cnec.AngleCnecAdder;
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
public final class AngleCnecArrayDeserializer {

    private AngleCnecArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, String version, Crac crac, Map<String, String> networkElementsNamesPerId) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new FaraoException(String.format("Cannot deserialize %s before %s", ANGLE_CNECS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
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
                        readFrm(jsonParser, version, angleCnecAdder);
                        break;
                    case RELIABILITY_MARGIN:
                        readReliabilityMargin(jsonParser, version, angleCnecAdder);
                        break;
                    case THRESHOLDS:
                        jsonParser.nextToken();
                        AngleThresholdArrayDeserializer.deserialize(jsonParser, angleCnecAdder);
                        break;
                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;
                    default:
                        throw new FaraoException("Unexpected field in AngleCnec: " + jsonParser.getCurrentName());
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
        if (getPrimaryVersionNumber(version) <= 1 && getSubVersionNumber(version) <= 3) {
            throw new FaraoException(String.format("Unexpected field for version %s : %s", version, RELIABILITY_MARGIN));
        }
        jsonParser.nextToken();
        angleCnecAdder.withReliabilityMargin(jsonParser.getDoubleValue());
    }

    private static void readFrm(JsonParser jsonParser, String version, AngleCnecAdder angleCnecAdder) throws IOException {
        //"frm" renamed to "reliabilityMargin" in 1.4
        if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 3) {
            throw new FaraoException(String.format("Unexpected field for version %s : %s", version, FRM));
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
