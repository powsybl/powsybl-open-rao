/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.networkaction.*;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.openrao.data.craciojson.JsonSerializationConstants.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public final class InjectionSetpointArrayDeserializer {
    private InjectionSetpointArrayDeserializer() {
    }

    private static void checkExpectedUnit(Unit expectedUnit, Unit unit, Identifiable<?> ne) {
        if (!Objects.isNull(unit) && unit != expectedUnit) {
            throw new OpenRaoException("Network element '" + ne.getId() + "' is a " + ne.getType() + ", its injection should be in " + expectedUnit.name() + " not in " + unit);
        }
    }

    public static void deserialize(JsonParser jsonParser, NetworkActionAdder ownerAdder, Map<String, String> networkElementsNamesPerId, Network network) throws IOException {
        if (networkElementsNamesPerId == null) {
            throw new OpenRaoException(String.format("Cannot deserialize %s before %s", INJECTION_SETPOINTS, NETWORK_ELEMENTS_NAME_PER_ID));
        }
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            String networkElementId = null;
            Double setpoint = null;
            Unit unit = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case NETWORK_ELEMENT_ID:
                        networkElementId = jsonParser.nextTextValue();
                        break;
                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;
                    case UNIT:
                        unit = deserializeUnit(jsonParser.nextTextValue());
                        break;
                    default:
                        throw new OpenRaoException("Unexpected field in InjectionSetpoint: " + jsonParser.getCurrentName());
                }
            }
            Identifiable<?> identifiable = network.getIdentifiable(networkElementId);
            if (Objects.isNull(identifiable)) {
                throw new OpenRaoException("Network element id " + networkElementId + " does not exist in network " + network.getId());
            }
            switch (identifiable.getType()) {
                case GENERATOR:
                    checkExpectedUnit(Unit.MEGAWATT, unit, identifiable);
                    GeneratorActionAdder generatorActionAdder = ownerAdder.newGeneratorAction();
                    if (networkElementsNamesPerId.containsKey(networkElementId)) {
                        generatorActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                    } else {
                        generatorActionAdder.withNetworkElement(networkElementId);
                    }
                    if (setpoint != null) {
                        generatorActionAdder.withActivePowerValue(setpoint);
                    }
                    generatorActionAdder.add();
                    break;
                case LOAD:
                    checkExpectedUnit(Unit.MEGAWATT, unit, identifiable);
                    LoadActionAdder loadActionAdder = ownerAdder.newLoadAction();
                    if (networkElementsNamesPerId.containsKey(networkElementId)) {
                        loadActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                    } else {
                        loadActionAdder.withNetworkElement(networkElementId);
                    }
                    if (setpoint != null) {
                        loadActionAdder.withActivePowerValue(setpoint);
                    }
                    loadActionAdder.add();
                    break;
                case DANGLING_LINE:
                    checkExpectedUnit(Unit.MEGAWATT, unit, identifiable);
                    DanglingLineActionAdder danglingLineActionAdder = ownerAdder.newDanglingLineAction();
                    if (networkElementsNamesPerId.containsKey(networkElementId)) {
                        danglingLineActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                    } else {
                        danglingLineActionAdder.withNetworkElement(networkElementId);
                    }
                    if (setpoint != null) {
                        danglingLineActionAdder.withActivePowerValue(setpoint);
                    }
                    danglingLineActionAdder.add();
                    break;
                case SHUNT_COMPENSATOR:
                    checkExpectedUnit(Unit.SECTION_COUNT, unit, identifiable);
                    ShuntCompensatorPositionActionAdder shuntCompensatorPositionActionAdder = ownerAdder.newShuntCompensatorPositionAction();
                    if (networkElementsNamesPerId.containsKey(networkElementId)) {
                        shuntCompensatorPositionActionAdder.withNetworkElement(networkElementId, networkElementsNamesPerId.get(networkElementId));
                    } else {
                        shuntCompensatorPositionActionAdder.withNetworkElement(networkElementId);
                    }
                    if (setpoint != null) {
                        shuntCompensatorPositionActionAdder.withSectionCount(setpoint.intValue());
                    }
                    shuntCompensatorPositionActionAdder.add();
                    break;
                default:
                    throw new OpenRaoException("InjectionSetpoint actions must be on network element of type generator, load, dangling line or shunt compensator, and here it is " + identifiable.getType());
            }
        }
    }
}
