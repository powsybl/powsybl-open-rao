/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.impl.RaoResultImpl;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class FastRaoExtensionDeserializer {
    private FastRaoExtensionDeserializer() {
    }

    static void deserialize(JsonParser jsonParser, RaoResultImpl raoResult, Crac crac) throws IOException {
        if (jsonParser.nextFieldName().equals(RaoResultJsonConstants.CRITICAL_CNECS_SET)) {
            Set<FlowCnec> criticalCnecsSet = new HashSet<>();
            jsonParser.nextToken();
            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                if (jsonParser.nextFieldName().equals(RaoResultJsonConstants.FLOWCNEC_ID)) {
                    criticalCnecsSet.add(crac.getFlowCnec(jsonParser.nextTextValue()));
                }
                jsonParser.nextToken();
            }
            raoResult.setCriticalCnecs(criticalCnecsSet);
        }
    }
}
