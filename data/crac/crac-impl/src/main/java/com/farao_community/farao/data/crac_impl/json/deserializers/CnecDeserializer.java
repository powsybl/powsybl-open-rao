/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_api.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.BranchThresholdImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.data.crac_impl.json.JsonSerializationNames.*;
import static java.lang.String.format;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class CnecDeserializer {

    private CnecDeserializer() { }

    static void deserialize(JsonParser jsonParser, DeserializationContext deserializationContext, SimpleCrac simpleCrac) throws IOException {
        // cannot be done in a standard Cnec deserializer as it requires the simpleCrac to compare
        // the State id and NetworkElement id of the Cnec with what is in the Crac

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String id = null;
            String name = null;
            String networkElementId = null;
            String operator = null;
            String contingencyId = null;
            Instant instant = null;
            double frm = 0;
            boolean optimized = false;
            boolean monitored = false;
            Set<BranchThreshold> thresholds = new HashSet<>();
            List<Extension<BranchCnec>> extensions = new ArrayList<>();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(FLOW_CNEC_TYPE)) {
                            throw new FaraoException(format("SimpleCrac cannot deserialize other Cnecs types than %s", FLOW_CNEC_TYPE));
                        }
                        break;

                    case ID:
                        id = jsonParser.nextTextValue();
                        break;

                    case NAME:
                        name = jsonParser.nextTextValue();
                        break;

                    case NETWORK_ELEMENT:
                        networkElementId = jsonParser.nextTextValue();
                        break;

                    case OPERATOR:
                        operator = jsonParser.nextTextValue();
                        break;

                    case CONTINGENCY:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case INSTANT:
                        jsonParser.nextToken();
                        instant = jsonParser.readValueAs(Instant.class);
                        break;

                    case FRM:
                        jsonParser.nextToken();
                        frm = jsonParser.getDoubleValue();
                        break;

                    case OPTIMIZED:
                        jsonParser.nextToken();
                        optimized = jsonParser.getBooleanValue();
                        break;

                    case MONITORED:
                        jsonParser.nextToken();
                        monitored = jsonParser.getBooleanValue();
                        break;

                    case THRESHOLDS:
                        jsonParser.nextToken();
                        thresholds = jsonParser.readValueAs(new TypeReference<Set<BranchThresholdImpl>>() {
                        });
                        break;

                    case EXTENSIONS:
                        jsonParser.nextToken();
                        extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                        break;

                    default:
                        throw new FaraoException("Unexpected field: " + jsonParser.getCurrentName());
                }
            }

            if (contingencyId != null && instant != Instant.PREVENTIVE) {
                Contingency contingency = simpleCrac.getContingency(contingencyId);
                simpleCrac.addCnec(id, name, networkElementId, operator, thresholds, contingency, instant, frm, optimized, monitored);
            } else if (contingencyId == null && instant == Instant.PREVENTIVE) {
                simpleCrac.addPreventiveCnec(id, name, networkElementId, operator, thresholds, frm, optimized, monitored);
            } else {
                throw new FaraoException("Impossible to add CNEC in preventive after a contingency.");
            }

            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(simpleCrac.getBranchCnec(id), extensions);
            }
        }
    }
}
