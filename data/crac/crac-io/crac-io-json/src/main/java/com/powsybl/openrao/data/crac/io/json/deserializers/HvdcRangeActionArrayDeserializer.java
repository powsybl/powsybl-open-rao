/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmHvdcHelper;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class HvdcRangeActionArrayDeserializer {
    private HvdcRangeActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.HVDC_RANGE_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
            String networkElementId = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                if (StandardRangeActionDeserializer.addCommonElement(hvdcRangeActionAdder, jsonParser, version)) {
                    continue;
                }
                if (jsonParser.getCurrentName().equals(JsonSerializationConstants.NETWORK_ELEMENT_ID)) {
                    networkElementId = readNetworkElementId(jsonParser, networkElementsNamesPerId, hvdcRangeActionAdder);
                } else {
                    throw new OpenRaoException("Unexpected field in HvdcRangeAction: " + jsonParser.getCurrentName());
                }
            }
            double initialSetpoint = IidmHvdcHelper.getCurrentSetpoint(network, networkElementId);
            hvdcRangeActionAdder.withInitialSetpoint(initialSetpoint);
            hvdcRangeActionAdder.add();

            // TODO add usage rule to network action
            // TODO move the network creaion outside of range action deserialization
            //add associated network action, only if the hvdc line has an AngleDroopActivePowerControl extension
            HvdcAngleDroopActivePowerControl hvdcAngleDoopActivePowerControl = IidmHvdcHelper.getHvdcLine(network, networkElementId).getExtension(HvdcAngleDroopActivePowerControl.class);
            if (hvdcAngleDoopActivePowerControl != null && hvdcAngleDoopActivePowerControl.isEnabled() ) {
                crac.newNetworkAction()
                    .newAcEmulationSwitchAction()
                    .withNetworkElement(networkElementId)
                    .withActionType(ActionType.DEACTIVATE)
                    .add();
            }
        }
    }

    private static String readNetworkElementId(JsonParser jsonParser, Map<String, String> networkElementsNamesPerId, HvdcRangeActionAdder hvdcRangeActionAdder) throws IOException {
        String networkElementId = jsonParser.nextTextValue();
        if (networkElementsNamesPerId.containsKey(networkElementId)) {
            hvdcRangeActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
        } else {
            hvdcRangeActionAdder.withNetworkElement(networkElementId);
        }
        return networkElementId;
    }
}
