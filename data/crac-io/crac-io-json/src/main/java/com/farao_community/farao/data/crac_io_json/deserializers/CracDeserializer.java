/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_io_json.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracDeserializer extends JsonDeserializer<Crac> {

    private CracFactory cracFactory;

    private CracDeserializer() {
    }

    public CracDeserializer(CracFactory cracFactory) {
        this.cracFactory = cracFactory;
    }

    @Override
    public Crac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        jsonParser.nextToken();
        while (!jsonParser.getCurrentName().equals(ID)) {
            if (jsonParser.nextToken() == JsonToken.END_OBJECT) {
                throw new FaraoException(String.format("The JSON Crac must contain an %s field", ID));
            }
            jsonParser.nextToken();
        }
        String id = jsonParser.nextTextValue();
        jsonParser.nextToken();
        if (!jsonParser.getCurrentName().equals(NAME)) {
            throw new FaraoException(String.format("The JSON Crac must contain a %s field after the %s field", NAME, ID));
        }
        String name = jsonParser.nextTextValue();

        Crac crac = cracFactory.create(id, name);

        Map<String, String> deserializedNetworkElementsNamesPerId = null;
        // deserialize the following lines of the Crac
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case NETWORK_ELEMENTS_NAME_PER_ID:
                    jsonParser.nextToken();
                    deserializedNetworkElementsNamesPerId = jsonParser.readValueAs(HashMap.class);
                    break;

                case CONTINGENCIES:
                    jsonParser.nextToken();
                    ContingencyArrayDeserializer.deserialize(jsonParser, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case FLOW_CNECS:
                    jsonParser.nextToken();
                    FlowCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case PST_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    PstRangeActionArrayDeserializer.deserialize(jsonParser, deserializationContext, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case HVDC_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    HvdcRangeActionArrayDeserializer.deserialize(jsonParser, deserializationContext, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    NetworkActionArrayDeserializer.deserialize(jsonParser, deserializationContext, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case EXTENSIONS:
                    jsonParser.nextToken();
                    List<Extension<Crac>> extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                    ExtensionsHandler.getExtensionsSerializers().addExtensions(crac, extensions);
                    break;

                default:
                    throw new FaraoException("Unexpected field in Crac: " + jsonParser.getCurrentName());
            }
        }
        return crac;
    }
}
