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
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.rao_result_impl.RangeActionResult;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class HvdcRangeActionResultArrayDeserializer {

    private HvdcRangeActionResultArrayDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {

        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            if (!jsonParser.nextFieldName().equals(HVDCRANGEACTION_ID)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", HVDCRANGEACTION_RESULTS, NETWORKACTION_ID));
            }

            String hvdcRangeActionId = jsonParser.nextTextValue();
            HvdcRangeAction hvdcRangeAction = crac.getHvdcRangeAction(hvdcRangeActionId);

            if (hvdcRangeAction == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: cannot deserialize RaoResult: hvdcRangeAction with id %s does not exist in the Crac", hvdcRangeActionId));
            }

            RangeActionResult hvdcRangeActionResult = raoResult.getAndCreateIfAbsentRangeActionResult(hvdcRangeAction);
            Double afterPraSetpoint = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {

                    case PST_NETWORKELEMENT_ID:
                        hvdcRangeActionResult.setNetworkElementId(jsonParser.nextTextValue());
                        break;

                    case INITIAL_SETPOINT:
                        jsonParser.nextToken();
                        hvdcRangeActionResult.setPreOptimSetPoint(jsonParser.getDoubleValue());
                        break;

                    case AFTER_PRA_SETPOINT:
                        jsonParser.nextToken();
                        afterPraSetpoint = jsonParser.getDoubleValue();
                        break;

                    case STATES_ACTIVATED:
                        jsonParser.nextToken();
                        deserializeResultsPerStates(jsonParser, hvdcRangeActionResult, crac);
                        break;

                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", PSTRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }
            // Do this at the end: for PSTs with afterPraTap and afterPraSetpoint, initial tap/setpoint should be set to afterPra values
            if (afterPraSetpoint != null) {
                hvdcRangeActionResult.setPreOptimSetPoint(afterPraSetpoint);
            }
        }
    }

    private static void deserializeResultsPerStates(JsonParser jsonParser, RangeActionResult hvdcRangeActionResult, Crac crac) throws IOException {

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
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", HVDCRANGEACTION_RESULTS, jsonParser.getCurrentName()));
                }
            }

            if (setpoint == null) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: setpoint are required in %s", HVDCRANGEACTION_RESULTS));
            }
            hvdcRangeActionResult.addActivationForState(StateDeserializer.getState(instant, contingencyId, crac, HVDCRANGEACTION_RESULTS), setpoint);

        }
    }
}
