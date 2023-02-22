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
final class NotOptimizedCnecsParametersSerializer {

    private NotOptimizedCnecsParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(NOT_OPTIMIZED_CNECS);
        jsonGenerator.writeBooleanField(DO_NOT_OPTIMIZE_CURATIVE_CNECS, parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());
        jsonGenerator.writeObjectField(DO_NOT_OPTIMIZE_CNECS_SECURED_BY_ITS_PST, parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCnecsSecuredByTheirPst());
        jsonGenerator.writeEndObject();
    }
}
