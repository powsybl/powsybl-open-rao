/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_io_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.range_action.StandardRangeActionAdder;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

import static com.farao_community.farao.data.crac_io_json.JsonSerializationConstants.*;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
public final class StandardRangeActionDeserializerUtils {

    private StandardRangeActionDeserializerUtils() {
    }

    /**
     *
     * @return true if the element was found
     * @throws IOException
     */
    public static boolean addCommonElement(StandardRangeActionAdder<?> standardRangeActionAdder, JsonParser jsonParser, String version) throws IOException {
        switch (jsonParser.getCurrentName()) {
            case ID:
                standardRangeActionAdder.withId(jsonParser.nextTextValue());
                break;
            case NAME:
                standardRangeActionAdder.withName(jsonParser.nextTextValue());
                break;
            case OPERATOR:
                standardRangeActionAdder.withOperator(jsonParser.nextTextValue());
                break;
            case ON_INSTANT_USAGE_RULES:
                jsonParser.nextToken();
                OnInstantArrayDeserializer.deserialize(jsonParser, version, standardRangeActionAdder);
                break;
            case FREE_TO_USE_USAGE_RULES:
                if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                    throw new FaraoException("FreeToUse has been renamed to OnInstant since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnInstantArrayDeserializer.deserialize(jsonParser, version, standardRangeActionAdder);
                }
                break;
            case ON_CONTINGENCY_STATE_USAGE_RULES:
                jsonParser.nextToken();
                OnStateArrayDeserializer.deserialize(jsonParser, version, standardRangeActionAdder);
                break;
            case ON_STATE_USAGE_RULES:
                if (getPrimaryVersionNumber(version) > 1 || getSubVersionNumber(version) > 5) {
                    throw new FaraoException("OnState has been renamed to OnContingencyState since CRAC version 1.6");
                } else {
                    jsonParser.nextToken();
                    OnStateArrayDeserializer.deserialize(jsonParser, version, standardRangeActionAdder);
                }
                break;
            case ON_FLOW_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnFlowConstraintArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case ON_ANGLE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnAngleConstraintArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case ON_VOLTAGE_CONSTRAINT_USAGE_RULES:
                jsonParser.nextToken();
                OnVoltageConstraintArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case ON_FLOW_CONSTRAINT_IN_COUNTRY_USAGE_RULES:
                jsonParser.nextToken();
                OnFlowConstraintInCountryArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case GROUP_ID:
                standardRangeActionAdder.withGroupId(jsonParser.nextTextValue());
                break;
            case INITIAL_SETPOINT:
                jsonParser.nextToken();
                standardRangeActionAdder.withInitialSetpoint(jsonParser.getDoubleValue());
                break;
            case RANGES:
                jsonParser.nextToken();
                StandardRangeArrayDeserializer.deserialize(jsonParser, standardRangeActionAdder);
                break;
            case EXTENSIONS:
                throw new FaraoException("Extensions are deprecated since CRAC version 1.7");
            case SPEED:
                jsonParser.nextToken();
                standardRangeActionAdder.withSpeed(jsonParser.getIntValue());
                break;
            default:
                return false;
        }
        return true;
    }

}
