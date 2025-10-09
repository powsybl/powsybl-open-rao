/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.Map;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class NetworkActionArrayDeserializer {
    private NetworkActionArrayDeserializer() {
    }

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.NETWORK_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            NetworkActionAdder networkActionAdder = crac.newNetworkAction();
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case JsonSerializationConstants.ID:
                        networkActionAdder.withId(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.NAME:
                        networkActionAdder.withName(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.OPERATOR:
                        networkActionAdder.withOperator(jsonParser.nextTextValue());
                        break;
                    case JsonSerializationConstants.ON_INSTANT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnInstantArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case JsonSerializationConstants.FREE_TO_USE_USAGE_RULES:
                        deserializeFreeToUseUsageRules(jsonParser, version, networkActionAdder);
                        break;
                    case JsonSerializationConstants.ON_CONTINGENCY_STATE_USAGE_RULES:
                        jsonParser.nextToken();
                        OnStateArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case JsonSerializationConstants.ON_STATE_USAGE_RULES:
                        deserializeOnStateUsageRules(jsonParser, version, networkActionAdder);
                        break;
                    case JsonSerializationConstants.ON_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        OnConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_FLOW_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_ANGLE_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                        jsonParser.nextToken();
                        deserializeOlderOnConstraintUsageRules(jsonParser, JsonSerializationConstants.ON_VOLTAGE_CONSTRAINT_USAGE_RULES, version, networkActionAdder);
                        break;
                    case JsonSerializationConstants.ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                        jsonParser.nextToken();
                        OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
                        break;
                    case JsonSerializationConstants.TOPOLOGICAL_ACTIONS:
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 4) {
                            throw new OpenRaoException(String.format("%s is either %s or %s since CRAC version 2.5", JsonSerializationConstants.TOPOLOGICAL_ACTIONS, JsonSerializationConstants.TERMINALS_CONNECTION_ACTIONS, JsonSerializationConstants.SWITCH_ACTIONS));
                        } else {
                            jsonParser.nextToken();
                            TopologicalActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId, network);
                        }
                        break;
                    case JsonSerializationConstants.PST_SETPOINTS:
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 4) {
                            throw new OpenRaoException(String.format("%s is now %s since CRAC version 2.5", JsonSerializationConstants.PST_SETPOINTS, JsonSerializationConstants.PHASETAPCHANGER_TAPPOSITION_ACTIONS));
                        } else {
                            jsonParser.nextToken();
                            PstSetpointArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        }
                        break;
                    case JsonSerializationConstants.INJECTION_SETPOINTS:
                        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) > 4) {
                            throw new OpenRaoException(String.format("%s is either %s, or %s, or %s, or %s since CRAC version 2.5", JsonSerializationConstants.INJECTION_SETPOINTS, JsonSerializationConstants.GENERATOR_ACTIONS, JsonSerializationConstants.LOAD_ACTIONS, JsonSerializationConstants.DANGLINGLINE_ACTIONS, JsonSerializationConstants.SHUNTCOMPENSATOR_POSITION_ACTIONS));
                        } else {
                            jsonParser.nextToken();
                            InjectionSetpointArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId, network);
                        }
                        break;
                    case JsonSerializationConstants.TERMINALS_CONNECTION_ACTIONS:
                        jsonParser.nextToken();
                        TerminalsConnectionActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.SWITCH_ACTIONS:
                        jsonParser.nextToken();
                        SwitchActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.GENERATOR_ACTIONS:
                        jsonParser.nextToken();
                        GeneratorActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.LOAD_ACTIONS:
                        jsonParser.nextToken();
                        LoadActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.DANGLINGLINE_ACTIONS:
                        jsonParser.nextToken();
                        DanglingLineActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.SHUNTCOMPENSATOR_POSITION_ACTIONS:
                        jsonParser.nextToken();
                        ShuntCompensatorPositionActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.PHASETAPCHANGER_TAPPOSITION_ACTIONS:
                        jsonParser.nextToken();
                        PhaseTapChangerTapPositionActionArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.SWITCH_PAIRS:
                        jsonParser.nextToken();
                        SwitchPairArrayDeserializer.deserialize(jsonParser, networkActionAdder, networkElementsNamesPerId);
                        break;
                    case JsonSerializationConstants.EXTENSIONS:
                        throw new OpenRaoException("Extensions are deprecated since CRAC version 1.7");
                    case JsonSerializationConstants.SPEED:
                        jsonParser.nextToken();
                        networkActionAdder.withSpeed(jsonParser.getIntValue());
                        break;
                    case JsonSerializationConstants.ACTIVATION_COST:
                        jsonParser.nextToken();
                        networkActionAdder.withActivationCost(jsonParser.getDoubleValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in NetworkAction: " + jsonParser.getCurrentName());
                }
            }
            networkActionAdder.add();
        }
    }

    private static void deserializeOnStateUsageRules(JsonParser jsonParser, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnStateArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
        }
    }

    private static void deserializeFreeToUseUsageRules(JsonParser jsonParser, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) > 1 || JsonSerializationConstants.getSubVersionNumber(version) > 5) {
            throw new OpenRaoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
        } else {
            jsonParser.nextToken();
            OnInstantArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
        }
    }

    private static void deserializeOlderOnConstraintUsageRules(JsonParser jsonParser, String keyword, String version, NetworkActionAdder networkActionAdder) throws IOException {
        if (JsonSerializationConstants.getPrimaryVersionNumber(version) < 2 || JsonSerializationConstants.getPrimaryVersionNumber(version) == 2 && JsonSerializationConstants.getSubVersionNumber(version) < 4) {
            OnConstraintArrayDeserializer.deserialize(jsonParser, networkActionAdder, version);
        } else {
            throw new OpenRaoException("Unsupported field %s in CRAC version >= 2.4".formatted(keyword));
        }
    }
}
