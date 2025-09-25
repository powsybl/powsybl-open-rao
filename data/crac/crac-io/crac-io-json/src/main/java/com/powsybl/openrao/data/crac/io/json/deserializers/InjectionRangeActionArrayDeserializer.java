/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.json.deserializers;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.io.json.JsonSerializationConstants;
import com.powsybl.openrao.data.crac.io.commons.iidm.IidmInjectionHelper;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.openrao.data.crac.io.json.deserializers.CracDeserializer.LOGGER;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public final class InjectionRangeActionArrayDeserializer {
    private InjectionRangeActionArrayDeserializer() {
    }

    static Set<String> networkElementsUsedList;

    public static void deserialize(JsonParser jsonParser, String version, Crac crac, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", JsonSerializationConstants.INJECTION_RANGE_ACTIONS, JsonSerializationConstants.NETWORK_ELEMENTS_NAME_PER_ID));
        }
        networkElementsUsedList = new HashSet<>();
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction();
            String injectionRangeActionId = null;
            Map<String, Double> injectionDistributionKeys = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                if (jsonParser.getCurrentName() == JsonSerializationConstants.ID) {
                    injectionRangeActionId = jsonParser.nextTextValue();
                    injectionRangeActionAdder.withId(injectionRangeActionId);
                    continue;
                }
                else if (StandardRangeActionDeserializer.addCommonElement(injectionRangeActionAdder, jsonParser, version)) {
                    continue;
                }
                if (jsonParser.getCurrentName().equals(JsonSerializationConstants.NETWORK_ELEMENT_IDS_AND_KEYS)) {
                    jsonParser.nextToken();
                    injectionDistributionKeys = deserializeInjectionDistributionKeys(jsonParser, injectionRangeActionAdder, networkElementsNamesPerId);
                } else {
                    throw new OpenRaoException("Unexpected field in InjectionRangeAction: " + jsonParser.getCurrentName());
                }
            }

            double initialSetpoint = IidmInjectionHelper.getCurrentSetpoint(network, injectionDistributionKeys);
            injectionRangeActionAdder.withInitialSetpoint(initialSetpoint);
            // add only if all the generators are connected
            Set<String> disconnectedGeneratorsSet = injectionDistributionKeys.keySet().stream()
                .map(generatorId -> network.getGenerator(generatorId))
                .filter(generator -> !generator.getTerminal().isConnected())
                .map(generator -> generator.getId()).collect(Collectors.toSet());

            if (!disconnectedGeneratorsSet.isEmpty()) {
                String disconnectedGenerators = String.join(",", disconnectedGeneratorsSet);
                OpenRaoLoggerProvider.BUSINESS_WARNS.warn(
                    String.format(
                        "The injection range action %s will be ignored in the optimization because it uses disconnected generator(s): %s.",
                        injectionRangeActionId,
                        disconnectedGenerators
                    )
                );
            } else {
                injectionRangeActionAdder.add();
            }


        }
    }

    private static Map<String, Double> deserializeInjectionDistributionKeys(JsonParser jsonParser, InjectionRangeActionAdder adder, Map<String, String> networkElementsNamesPerId) throws IOException {
        Map<String, Double> injectionDistributionKeys = new HashMap<>();
        while (!jsonParser.nextToken().isStructEnd()) {
            String networkElementId = jsonParser.getCurrentName();
            // check if an another injection action was already defined on the same network element.
            if (networkElementsUsedList.contains(networkElementId)) {
                LOGGER.warn("If the injection range action is used to represent a redispatching remedial action : " +
                    "two different injection actions in the crac can not be defined on the same network element : " + networkElementId);
            }
            networkElementsUsedList.add(networkElementId);
            jsonParser.nextToken();
            double key = jsonParser.getDoubleValue();
            if (networkElementsNamesPerId.containsKey(networkElementId)) {
                adder.withNetworkElementAndKey(key, networkElementId, networkElementsNamesPerId.get(networkElementId));
            } else {
                adder.withNetworkElementAndKey(key, networkElementId);
            }
            injectionDistributionKeys.put(networkElementId, key);
        }
        return injectionDistributionKeys;
    }
}
