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
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
final class ComputationStatusMapDeserializer {

    private ComputationStatusMapDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            // COMPUTATION STATUS
            if (!jsonParser.nextFieldName().equals(COMPUTATION_STATUS)) {
                throw new FaraoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", COMPUTATION_STATUS_MAP, COMPUTATION_STATUS));
            }
            String computationStatus = jsonParser.nextTextValue();
            // STATE
            Instant instant = null;
            String contingencyId = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case INSTANT:
                        instant = deserializeInstant(jsonParser.nextTextValue());
                        break;
                    case CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new FaraoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", COMPUTATION_STATUS_MAP, jsonParser.getCurrentName()));
                }
            }
            raoResult.setComputationStatus(StateDeserializer.getState(instant, contingencyId, crac, COMPUTATION_STATUS_MAP), ComputationStatus.valueOf(computationStatus));
        }
    }
}
