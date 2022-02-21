/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.rao_result_json.deserializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_impl.RangeActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;
import static com.farao_community.farao.data.rao_result_json.deserializers.DeprecatedRaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class StandardRangeActionResultArrayDeserializer {

    private StandardRangeActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac, String jsonFileVersion) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {

            String firstFieldName = jsonParser.nextFieldName();

            // in version <= 1.1, the id field was HVDCRANGEACION_ID, it is now RANGEACTION_ID
            if (!firstFieldName.equals(RANGEACTION_ID)
                && !firstFieldName.equals(HVDCRANGEACTION_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", STANDARDRANGEACTION_RESULTS, RANGEACTION_ID));
            }

            String rangeActionId = jsonParser.nextTextValue();
            RangeAction<?> rangeAction = crac.getRangeAction(rangeActionId);

            if (rangeAction == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: RangeAction with id %s does not exist in the Crac", rangeActionId));
            }

            RangeActionResult rangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(rangeAction);
            Double afterPraSetpoint = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case HVDC_NETWORKELEMENT_ID:
                        // only used in version <=1.1
                        // keep here for retrocompatibility, but information is not used anymore
                        if (getPrimaryVersionNumber(jsonFileVersion) > 1 && getSubVersionNumber(jsonFileVersion) > 1) {
                            throw new FaraoException(String.format("Cannot deserialize RaoResult: field %s in %s in not supported in file version %s", jsonParser.getCurrentName(), HVDCRANGEACTION_RESULTS, jsonFileVersion));
                        } else {
                            jsonParser.nextTextValue();
                        }
                        break;

                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        rangeActionResult.setPreOptimSetPoint(jsonParser.getDoubleValue());
                        break;

                    case AFTER_PRA_SETPOINT:
                        jsonParser.nextToken();
                        afterPraSetpoint = jsonParser.getDoubleValue();
                        break;

                    case STATES_ACTIVATED:
                        jsonParser.nextToken();
                        deserializeResultsPerStates(jsonParser, rangeActionResult, crac);
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", STANDARDRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
            // Do this at the end: for rangeAction with afterPraSetpoint, initial setpoint should be set to afterPra values
            if (afterPraSetpoint != null) {
                rangeActionResult.setPreOptimSetPoint(afterPraSetpoint);
            }
        }
    }

    private static void deserializeResultsPerStates(JsonParser jsonParser, RangeActionResult rangeActionResult, Crac crac) throws IOException {

        Instant instant = null;
        String contingencyId = null;
        Double setpoint = null;

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;

                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;

                    case SETPOINT:
                        jsonParser.nextToken();
                        setpoint = jsonParser.getDoubleValue();
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", STANDARDRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (setpoint == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: setpoint are required in %s", STANDARDRANGEACTION_RESULTS));
            }
            rangeActionResult.addActivationForState(StateDeserializer.getState(instant, contingencyId, crac, STANDARDRANGEACTION_RESULTS), setpoint);

        }
    }
}
