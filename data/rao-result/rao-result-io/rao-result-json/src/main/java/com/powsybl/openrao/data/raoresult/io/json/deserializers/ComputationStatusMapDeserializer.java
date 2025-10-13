/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.ComputationStatus;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class ComputationStatusMapDeserializer {

    private ComputationStatusMapDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {
        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
            // COMPUTATION STATUS
            if (!jsonParser.nextFieldName().equals(RaoResultJsonConstants.COMPUTATION_STATUS)) {
                throw new OpenRaoException(String.format("Cannot deserialize RaoResult: each %s must start with an %s field", RaoResultJsonConstants.COMPUTATION_STATUS_MAP, RaoResultJsonConstants.COMPUTATION_STATUS));
            }
            String computationStatus = jsonParser.nextTextValue();
            // STATE
            String instantId = null;
            String contingencyId = null;
            while (!jsonParser.nextToken().isStructEnd()) {
                switch (jsonParser.getCurrentName()) {
                    case RaoResultJsonConstants.INSTANT:
                        String stringValue = jsonParser.nextTextValue();
                        instantId = stringValue;
                        break;
                    case RaoResultJsonConstants.CONTINGENCY_ID:
                        contingencyId = jsonParser.nextTextValue();
                        break;
                    default:
                        throw new OpenRaoException(String.format("Cannot deserialize RaoResult: unexpected field in %s (%s)", RaoResultJsonConstants.COMPUTATION_STATUS_MAP, jsonParser.getCurrentName()));
                }
            }
            raoResult.setComputationStatus(StateDeserializer.getState(instantId, contingencyId, crac, RaoResultJsonConstants.COMPUTATION_STATUS_MAP), ComputationStatus.valueOf(computationStatus));
        }
    }
}
