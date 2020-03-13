/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.json.ExtensionsHandler;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
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

import static com.farao_community.farao.data.crac_io_json.deserializers.DeserializerNames.*;

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
            String stateId = null;
            Set<AbstractThreshold> thresholds = new HashSet<>();
            List<Extension<Cnec>> extensions = new ArrayList<>();

            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case TYPE:
                        if (!jsonParser.nextTextValue().equals(SIMPLE_CNEC_TYPE)) {
                            throw new FaraoException(String.format("SimpleCrac cannot deserialize other Cnecs types than %s", SIMPLE_CNEC_TYPE));
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

                    case STATE:
                        stateId = jsonParser.nextTextValue();
                        break;

                    case THRESHOLDS:
                        jsonParser.nextToken();
                        thresholds = jsonParser.readValueAs(new TypeReference<Set<AbstractThreshold>>() {
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

            //add SimpleCnec in Crac
            simpleCrac.addCnec(id, name, networkElementId, thresholds, stateId);
            if (!extensions.isEmpty()) {
                ExtensionsHandler.getExtensionsSerializers().addExtensions(simpleCrac.getCnec(id), extensions);
            }
        }
    }
}
