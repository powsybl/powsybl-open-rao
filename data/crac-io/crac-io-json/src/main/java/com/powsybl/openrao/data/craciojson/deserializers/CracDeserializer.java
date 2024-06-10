/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.craciojson.ExtensionsHandler;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracDeserializer extends JsonDeserializer<Crac> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracDeserializer.class);

    private CracFactory cracFactory;

    private Network network;

    private CracDeserializer() {
    }

    public CracDeserializer(CracFactory cracFactory, Network network) {
        this.cracFactory = cracFactory;
        this.network = network;
    }

    @Override
    public Crac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // check header
        if (!jsonParser.nextFieldName().equals(TYPE)) {
            throw new OpenRaoException(String.format("json CRAC must start with field %s", TYPE));
        }
        if (!jsonParser.nextTextValue().equals(CRAC_TYPE)) {
            throw new OpenRaoException(String.format("type of document must be %s", CRAC_TYPE));
        }
        if (!jsonParser.nextFieldName().equals(VERSION)) {
            throw new OpenRaoException(String.format("%s must contain a %s in its second field", CRAC_TYPE, VERSION));
        }
        String version = jsonParser.nextTextValue();
        checkVersion(version);
        jsonParser.nextToken();

        // get id and name
        scrollJsonUntilField(jsonParser, ID);
        String id = jsonParser.nextTextValue();
        jsonParser.nextToken();
        if (!jsonParser.getCurrentName().equals(NAME)) {
            throw new OpenRaoException(String.format("The JSON Crac must contain a %s field after the %s field", NAME, ID));
        }
        String name = jsonParser.nextTextValue();
        Crac crac = cracFactory.create(id, name);
        if (getPrimaryVersionNumber(version) < 2) {
            crac.newInstant("preventive", InstantKind.PREVENTIVE)
                .newInstant("outage", InstantKind.OUTAGE)
                .newInstant("auto", InstantKind.AUTO)
                .newInstant("curative", InstantKind.CURATIVE);
        }

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
                    ContingencyArrayDeserializer.deserialize(jsonParser, crac, network);
                    break;

                case FLOW_CNECS:
                    jsonParser.nextToken();
                    FlowCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case ANGLE_CNECS:
                    jsonParser.nextToken();
                    AngleCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case VOLTAGE_CNECS:
                    jsonParser.nextToken();
                    VoltageCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case PST_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    PstRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case HVDC_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    HvdcRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case INJECTION_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    InjectionRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case COUNTER_TRADE_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    CounterTradeRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    NetworkActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId, network);
                    break;
                case EXTENSIONS:
                    jsonParser.nextToken();
                    List<Extension<Crac>> extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                    ExtensionsHandler.getExtensionsSerializers().addExtensions(crac, extensions);
                    break;

                case INSTANTS:
                    jsonParser.nextToken();
                    InstantArrayDeserializer.deserialize(jsonParser, crac);
                    break;

                case RA_USAGE_LIMITS_PER_INSTANT:
                    jsonParser.nextToken();
                    RaUsageLimitsDeserializer.deserialize(jsonParser, crac);
                    break;

                default:
                    throw new OpenRaoException("Unexpected field in Crac: " + jsonParser.getCurrentName());
            }
        }
        return crac;
    }

    private void scrollJsonUntilField(JsonParser jsonParser, String field) throws IOException {
        while (!jsonParser.getCurrentName().equals(field)) {
            if (jsonParser.nextToken() == JsonToken.END_OBJECT) {
                throw new OpenRaoException(String.format("The JSON Crac must contain an %s field", field));
            }
            jsonParser.nextToken();
        }
    }

    private void checkVersion(String cracVersion) {

        if (getPrimaryVersionNumber(CRAC_IO_VERSION) > getPrimaryVersionNumber(cracVersion)) {
            LOGGER.warn("CRAC importer {} might not be longer compatible with json CRAC version {}, consider updating your json CRAC file", CRAC_IO_VERSION, cracVersion);
        }
        if (getPrimaryVersionNumber(CRAC_IO_VERSION) < getPrimaryVersionNumber(cracVersion)) {
            throw new OpenRaoException(String.format("CRAC importer %s cannot handle json CRAC version %s, consider upgrading open-rao version", CRAC_IO_VERSION, cracVersion));
        }
        if (getSubVersionNumber(CRAC_IO_VERSION) < getSubVersionNumber(cracVersion)) {
            LOGGER.warn("CRAC importer {} might not be compatible with json CRAC version {}, consider upgrading open-rao version", CRAC_IO_VERSION, cracVersion);
        }

        // otherwise, all is good !
    }
}
