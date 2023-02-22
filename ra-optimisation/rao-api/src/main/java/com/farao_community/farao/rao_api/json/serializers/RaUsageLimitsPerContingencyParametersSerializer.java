/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.serializers;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class RaUsageLimitsPerContingencyParametersSerializer {

    private RaUsageLimitsPerContingencyParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(RA_USAGE_LIMITS_PER_CONTINGENCY);
        jsonGenerator.writeNumberField(MAX_CURATIVE_RA, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRa());
        jsonGenerator.writeNumberField(MAX_CURATIVE_TSO, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTso());
        jsonGenerator.writeObjectField(MAX_CURATIVE_TOPO_PER_TSO, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeTopoPerTso());
        jsonGenerator.writeObjectField(MAX_CURATIVE_PST_PER_TSO, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativePstPerTso());
        jsonGenerator.writeObjectField(MAX_CURATIVE_RA_PER_TSO, parameters.getRaUsageLimitsPerContingencyParameters().getMaxCurativeRaPerTso());
        jsonGenerator.writeEndObject();
    }
}
