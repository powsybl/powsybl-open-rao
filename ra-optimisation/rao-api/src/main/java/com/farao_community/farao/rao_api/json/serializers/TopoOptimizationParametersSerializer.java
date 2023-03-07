/*
 *  Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.rao_api.json.serializers;

import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.List;

import static com.farao_community.farao.rao_api.RaoParametersConstants.*;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
final class TopoOptimizationParametersSerializer {

    private TopoOptimizationParametersSerializer() {
    }

    static void serialize(RaoParameters parameters, JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeObjectFieldStart(TOPOLOGICAL_ACTIONS_OPTIMIZATION);
        jsonGenerator.writeNumberField(MAX_SEARCH_TREE_DEPTH, parameters.getTopoOptimizationParameters().getMaxSearchTreeDepth());
        jsonGenerator.writeFieldName(PREDEFINED_COMBINATIONS);
        jsonGenerator.writeStartArray();
        for (List<String> naIdCombination : parameters.getTopoOptimizationParameters().getPredefinedCombinations()) {
            jsonGenerator.writeStartArray();
            for (String naId : naIdCombination) {
                jsonGenerator.writeString(naId);
            }
            jsonGenerator.writeEndArray();
        }
        jsonGenerator.writeEndArray();
        jsonGenerator.writeNumberField(RELATIVE_MINIMUM_IMPACT_THRESHOLD, parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        jsonGenerator.writeNumberField(ABSOLUTE_MINIMUM_IMPACT_THRESHOLD, parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        jsonGenerator.writeBooleanField(SKIP_ACTIONS_FAR_FROM_MOST_LIMITING_ELEMENT, parameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        jsonGenerator.writeNumberField(MAX_NUMBER_OF_BOUNDARIES_FOR_SKIPPING_ACTIONS, parameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());
        jsonGenerator.writeEndObject();
    }
}
