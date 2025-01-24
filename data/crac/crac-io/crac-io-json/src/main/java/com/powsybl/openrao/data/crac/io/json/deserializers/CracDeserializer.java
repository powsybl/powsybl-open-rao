/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.ExtensionsHandler;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CracDeserializer extends JsonDeserializer<Crac> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracDeserializer.class);

    private CracFactory cracFactory;

    private Network network;

    private final boolean headerCheckOnly;

    public CracDeserializer(boolean headerCheckOnly) {
        this.headerCheckOnly = headerCheckOnly;
    }

    public CracDeserializer(CracFactory cracFactory, Network network) {
        this.cracFactory = cracFactory;
        this.network = network;
        this.headerCheckOnly = false;
    }

    @Override
    public Crac deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        // check header
        String version = isValid(jsonParser);
        if (headerCheckOnly) {
            return null;
        }

        // get id, name and timestamp
        scrollJsonUntilField(jsonParser, JsonSerializationConstants.ID);
        String id = jsonParser.nextTextValue();
        jsonParser.nextToken();
        if (!jsonParser.getCurrentName().equals(JsonSerializationConstants.NAME)) {
            throw new OpenRaoException(String.format("The JSON Crac must contain a %s field after the %s field", JsonSerializationConstants.NAME, JsonSerializationConstants.ID));
        }
        String name = jsonParser.nextTextValue();
        JsonToken nextToken = jsonParser.nextToken();
        OffsetDateTime timestamp;
        if (jsonParser.getCurrentName().equals(JsonSerializationConstants.TIMESTAMP)) {
            timestamp = OffsetDateTime.parse(jsonParser.nextTextValue());
            nextToken = jsonParser.nextToken();
        } else {
            timestamp = null;
        }
        Crac crac = cracFactory.create(id, name, timestamp);
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) < 2) {
            crac.newInstant("preventive", InstantKind.PREVENTIVE)
                .newInstant("outage", InstantKind.OUTAGE)
                .newInstant("auto", InstantKind.AUTO)
                .newInstant("curative", InstantKind.CURATIVE);
        }

        Map<String, String> deserializedNetworkElementsNamesPerId = null;

        // deserialize the following lines of the Crac
        while (nextToken != JsonToken.END_OBJECT) {
            switch (jsonParser.getCurrentName()) {
                case JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID:
                    jsonParser.nextToken();
                    deserializedNetworkElementsNamesPerId = jsonParser.readValueAs(HashMap.class);
                    break;

                case JsonSerializationConstants.CONTINGENCIES:
                    jsonParser.nextToken();
                    ContingencyArrayDeserializer.deserialize(jsonParser, crac, network);
                    break;

                case JsonSerializationConstants.FLOW_CNECS:
                    jsonParser.nextToken();
                    FlowCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.ANGLE_CNECS:
                    jsonParser.nextToken();
                    AngleCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.VOLTAGE_CNECS:
                    jsonParser.nextToken();
                    VoltageCnecArrayDeserializer.deserialize(jsonParser, deserializationContext, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.PST_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    PstRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.HVDC_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    HvdcRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.INJECTION_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    InjectionRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.COUNTER_TRADE_RANGE_ACTIONS:
                    jsonParser.nextToken();
                    CounterTradeRangeActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId);
                    break;

                case JsonSerializationConstants.NETWORK_ACTIONS:
                    jsonParser.nextToken();
                    NetworkActionArrayDeserializer.deserialize(jsonParser, version, crac, deserializedNetworkElementsNamesPerId, network);
                    break;
                case JsonSerializationConstants.EXTENSIONS:
                    jsonParser.nextToken();
                    List<Extension<Crac>> extensions = JsonUtil.readExtensions(jsonParser, deserializationContext, ExtensionsHandler.getExtensionsSerializers());
                    ExtensionsHandler.getExtensionsSerializers().addExtensions(crac, extensions);
                    break;

                case JsonSerializationConstants.INSTANTS:
                    jsonParser.nextToken();
                    InstantArrayDeserializer.deserialize(jsonParser, crac);
                    break;

                case JsonSerializationConstants.RA_USAGE_LIMITS_PER_INSTANT:
                    jsonParser.nextToken();
                    RaUsageLimitsDeserializer.deserialize(jsonParser, crac);
                    break;

                default:
                    throw new OpenRaoException("Unexpected field in Crac: " + jsonParser.getCurrentName());
            }
            nextToken = jsonParser.nextToken();
        }
        return crac;
    }

    public static String isValid(JsonParser jsonParser) throws IOException {
        if (!jsonParser.nextFieldName().equals(JsonSerializationConstants.TYPE)) {
            throw new OpenRaoException(String.format("json CRAC must start with field %s", JsonSerializationConstants.TYPE));
        }
        if (!jsonParser.nextTextValue().equals(JsonSerializationConstants.CRAC_TYPE)) {
            throw new OpenRaoException(String.format("type of document must be %s", JsonSerializationConstants.CRAC_TYPE));
        }
        if (!jsonParser.nextFieldName().equals(JsonSerializationConstants.VERSION)) {
            throw new OpenRaoException(String.format("%s must contain a %s in its second field", JsonSerializationConstants.CRAC_TYPE, JsonSerializationConstants.VERSION));
        }
        String version = jsonParser.nextTextValue();
        checkVersion(version);
        jsonParser.nextToken();
        return version;
    }

    private void scrollJsonUntilField(JsonParser jsonParser, String field) throws IOException {
        while (!jsonParser.getCurrentName().equals(field)) {
            if (jsonParser.nextToken() == JsonToken.END_OBJECT) {
                throw new OpenRaoException(String.format("The JSON Crac must contain an %s field", field));
            }
            jsonParser.nextToken();
        }
    }

    private static void checkVersion(String cracVersion) {

        if (JsonSerializationConstants.getPrimaryVersionNumber(JsonSerializationConstants.CRAC_IO_VERSION) > JsonSerializationConstants.getPrimaryVersionNumber(cracVersion)) {
            LOGGER.warn("CRAC importer {} might not be longer compatible with json CRAC version {}, consider updating your json CRAC file", JsonSerializationConstants.CRAC_IO_VERSION, cracVersion);
        }
        if (JsonSerializationConstants.getPrimaryVersionNumber(JsonSerializationConstants.CRAC_IO_VERSION) < JsonSerializationConstants.getPrimaryVersionNumber(cracVersion)) {
            throw new OpenRaoException(String.format("CRAC importer %s cannot handle json CRAC version %s, consider upgrading open-rao version", JsonSerializationConstants.CRAC_IO_VERSION, cracVersion));
        }
        if (JsonSerializationConstants.getSubVersionNumber(JsonSerializationConstants.CRAC_IO_VERSION) < JsonSerializationConstants.getSubVersionNumber(cracVersion)) {
            LOGGER.warn("CRAC importer {} might not be compatible with json CRAC version {}, consider upgrading open-rao version", JsonSerializationConstants.CRAC_IO_VERSION, cracVersion);
        }

        // otherwise, all is good !
    }
}
