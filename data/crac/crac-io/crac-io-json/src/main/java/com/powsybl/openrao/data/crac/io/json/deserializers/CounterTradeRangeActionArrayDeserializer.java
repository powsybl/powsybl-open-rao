/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmCounterTradeHelper;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;

import java.io.IOException;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class CounterTradeRangeActionArrayDeserializer {
    private CounterTradeRangeActionArrayDeserializer() {
    }

    /**
     * Fields read in the JSON that are needed to add the connected areas from the network before the adder is called
     */
    private static final class AreaFields {
        private String areaId;
        private boolean connectedAreasDefined;
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Network network) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction();
            AreaFields areaFields = new AreaFields();
            while (!jsonParser.nextToken().isStructEnd()) {
                addElement(counterTradeRangeActionAdder, areaFields, jsonParser, version);
            }
            // if no connected area is defined, all the areas sharing a border with the area in the network are used, without border ranges
            // (a missing area is reported by the adder)
            if (!areaFields.connectedAreasDefined && areaFields.areaId != null) {
                new IidmCounterTradeHelper(areaFields.areaId, network).getNeighbors()
                    .forEach(neighbor -> counterTradeRangeActionAdder.newConnectedArea().withAreaId(neighbor).add());
            }
            // TODO: compute the initial set-point, i.e. the initial net position of the area, from the network
            //  with a loadflow, as done for the initial set-point of other range actions
            counterTradeRangeActionAdder.withInitialSetpoint(0.0);
            CounterTradeRangeAction counterTradeRangeAction = counterTradeRangeActionAdder.add();
            // connected areas defined in the JSON must share a border with the area in the network
            if (areaFields.connectedAreasDefined) {
                new IidmCounterTradeHelper(counterTradeRangeAction.getAreaId(), network).validateConnectedAreas(counterTradeRangeAction);
            }
        }
    }

    private static boolean isBeforeV2Point12(String version) {
        return JsonSerializationConstants.getPrimaryVersionNumber(version) < 2
            || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) < 12;
    }

    private static void checkFieldRemovedInV2Point12(JsonParser jsonParser, String version) throws IOException {
        if (!isBeforeV2Point12(version)) {
            throw new OpenRaoException("%s field is no longer used since CRAC version 2.12, it is replaced by area and connectedAreas".formatted(jsonParser.currentName()));
        }
    }

    private static void addElement(CounterTradeRangeActionAdder counterTradeRangeActionAdder, AreaFields areaFields, JsonParser jsonParser, String version) throws IOException {
        if (StandardRangeActionDeserializer.addCommonElement(counterTradeRangeActionAdder, jsonParser, version)) {
            return;
        }
        switch (jsonParser.currentName()) {
            case JsonSerializationConstants.AREA:
                areaFields.areaId = jsonParser.nextTextValue();
                counterTradeRangeActionAdder.withAreaId(areaFields.areaId);
                break;
            case JsonSerializationConstants.EXPORTING_AREA, JsonSerializationConstants.EXPORTING_COUNTRY:
                checkFieldRemovedInV2Point12(jsonParser, version);
                areaFields.areaId = jsonParser.nextTextValue();
                counterTradeRangeActionAdder.withAreaId(areaFields.areaId);
                break;
            case JsonSerializationConstants.IMPORTING_AREA, JsonSerializationConstants.IMPORTING_COUNTRY:
                checkFieldRemovedInV2Point12(jsonParser, version);
                counterTradeRangeActionAdder.newConnectedArea().withAreaId(jsonParser.nextTextValue()).add();
                areaFields.connectedAreasDefined = true;
                break;
            case JsonSerializationConstants.CONNECTED_AREAS:
                jsonParser.nextToken();
                ConnectedAreaArrayDeserializer.deserialize(jsonParser, counterTradeRangeActionAdder);
                areaFields.connectedAreasDefined = true;
                break;
            default:
                throw new OpenRaoException("Unexpected field in InjectionRangeAction: " + jsonParser.currentName());
        }
    }
}
