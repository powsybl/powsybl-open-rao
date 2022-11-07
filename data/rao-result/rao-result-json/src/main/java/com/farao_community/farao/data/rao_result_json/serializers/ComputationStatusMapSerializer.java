/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class ComputationStatusMapSerializer {

    private ComputationStatusMapSerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(COMPUTATION_STATUS_MAP);
        for (State state : crac.getStates()) {
            jsonGenerator.writeStringField(state.getId(), raoResult.getComputationStatus().toString());
        }
        jsonGenerator.writeEndObject();
    }
}
