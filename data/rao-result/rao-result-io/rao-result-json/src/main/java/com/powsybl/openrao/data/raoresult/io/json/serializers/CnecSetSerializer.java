/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;
import com.powsybl.openrao.searchtreerao.result.impl.FastRaoResultImpl;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public final class CnecSetSerializer {
    private CnecSetSerializer() { }

    static void serialize(FastRaoResultImpl raoResult, JsonGenerator jsonGenerator) throws IOException {
        List<FlowCnec> sortedListofFlowCnecs = raoResult.getCriticalCnecs().stream()
            .sorted(Comparator.comparing(FlowCnec::getId))
            .toList();
        jsonGenerator.writeArrayFieldStart(RaoResultJsonConstants.CRITICAL_CNECS_SET);
        for (FlowCnec consideredCnec : sortedListofFlowCnecs) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(RaoResultJsonConstants.FLOWCNEC_ID, consideredCnec.getId());
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }
}
