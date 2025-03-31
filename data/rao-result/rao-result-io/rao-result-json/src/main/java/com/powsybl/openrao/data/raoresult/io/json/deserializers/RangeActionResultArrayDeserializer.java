/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.raoresult.impl.RangeActionResult;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;
import static com.powsybl.openrao.data.raoresult.io.json.deserializers.Utils.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class RangeActionResultArrayDeserializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RangeActionResultArrayDeserializer.class);

    private RangeActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String firstFieldName = jsonParser.nextFieldName();
            switch (firstFieldName) {
                case RANGEACTION_ID:
                    break;
                case DeprecatedRaoResultJsonConstants.HVDCRANGEACTION_ID:
                    // in version <= 1.1, the id field was HVDCRANGEACION_ID, it is now RANGEACTION_ID
                    checkDeprecatedField(DeprecatedRaoResultJsonConstants.HVDCRANGEACTION_RESULTS, RAO_RESULT_TYPE, jsonFileVersion, "1.1");
                    break;
                case PSTRANGEACTION_ID:
                    // in version <= 1.2, the id field was PSTRANGEACTION_ID, it is now RANGEACTION_ID
                    checkDeprecatedField(PSTRANGEACTION_ID, RAO_RESULT_TYPE, jsonFileVersion, "1.2");
                    break;
                default:
                    throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", RANGEACTION_RESULTS, RANGEACTION_ID));
            }

            String rangeActionId = jsonParser.nextTextValue();
            RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);

            if (rangeAction == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: RangeAction with id %s does not exist in the Crac", rangeActionId));
            }

            RangeActionResult rangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(rangeAction);
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case DeprecatedRaoResultJsonConstants.HVDC_NETWORKELEMENT_ID:
                        checkDeprecatedField(DeprecatedRaoResultJsonConstants.HVDC_NETWORKELEMENT_ID, RANGEACTION_RESULTS, jsonFileVersion, "1.1");
                        jsonParser.nextTextValue();
                        break;

                    case DeprecatedRaoResultJsonConstants.PST_NETWORKELEMENT_ID:
                        checkDeprecatedField(DeprecatedRaoResultJsonConstants.PST_NETWORKELEMENT_ID, RANGEACTION_RESULTS, jsonFileVersion, "1.2");
                        jsonParser.nextTextValue();
                        break;

                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        rangeActionResult.setInitialSetpoint(jsonParser.getDoubleValue());
                        break;

                    case STATES_ACTIVATED:
                        jsonParser.nextToken();
                        deserializeResultsPerStates(jsonParser, rangeActionResult, crac);
                        break;

                    case AFTER_PRA_SETPOINT:
                        checkDeprecatedField(AFTER_PRA_SETPOINT, RANGEACTION_RESULTS, jsonFileVersion, "1.2");
                        jsonParser.nextTextValue();
                        break;

                    case AFTER_PRA_TAP:
                        checkDeprecatedField(AFTER_PRA_TAP, RANGEACTION_RESULTS, jsonFileVersion, "1.2");
                        jsonParser.nextTextValue();
                        break;

                    case INITIAL_TAP:
                        // skip this, we don't need to read tap because we have the setpoint
                        LOGGER.info("Field {} in {} is no longer used", INITIAL_TAP, RANGEACTION_RESULTS);
                        jsonParser.nextTextValue();
                        break;

                    default:
                        throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", RANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
        }
    }

    private static void deserializeResultsPerStates(JsonParser jsonParser, RangeActionResult rangeActionResult, Crac crac) throws IOException {
        String instantId = null;
        String contingencyId = null;
        Double setpoint = null;
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case INSTANT:
                        String stringValue = jsonParser.nextTextValue();
                        instantId = stringValue;
                        break;

                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;

                    case TAP:
                        // Skip, we already have setpoint
                        LOGGER.info("Field {} in {} is no longer used", TAP, RANGEACTION_RESULTS);
                        jsonParser.nextFieldName();
                        break;

                    default:
                        throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", RANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (setpoint == null) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: setpoint is required in %s", RANGEACTION_RESULTS));
            }
            rangeActionResult.addActivationForState(StateDeserializer.getState(instantId, contingencyId, crac, RANGEACTION_RESULTS), setpoint);
        }
    }
}
