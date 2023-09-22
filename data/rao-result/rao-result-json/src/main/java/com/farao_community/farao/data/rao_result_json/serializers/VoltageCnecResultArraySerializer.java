/*
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.rao_result_json.serializers;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.rao_result_json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class VoltageCnecResultArraySerializer {

    private VoltageCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<VoltageCnec> sortedListOfVoltageCnecs = crac.getVoltageCnecs().stream()
            .sorted(Comparator.comparing(VoltageCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(VOLTAGECNEC_RESULTS);
        for (VoltageCnec voltageCnec : sortedListOfVoltageCnecs) {
            serializeVoltageCnecResult(voltageCnec, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeVoltageCnecResult(VoltageCnec voltageCnec, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForVoltageCnec(raoResult, voltageCnec, crac)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(VOLTAGECNEC_ID, voltageCnec.getId());

            serializeVoltageCnecResultForOptimizationState(OptimizationState.initial(crac), voltageCnec, raoResult, jsonGenerator);
            serializeVoltageCnecResultForOptimizationState(OptimizationState.afterPra(crac), voltageCnec, raoResult, jsonGenerator);

            if (!voltageCnec.getState().isPreventive()) {
                serializeVoltageCnecResultForOptimizationState(OptimizationState.afterAra(crac), voltageCnec, raoResult, jsonGenerator);
                serializeVoltageCnecResultForOptimizationState(OptimizationState.afterCra(crac), voltageCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationState(OptimizationState optState, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, voltageCnec, optState)) {
            jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
            serializeVoltageCnecResultForOptimizationStateAndUnit(optState, Unit.KILOVOLT, voltageCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double voltage = safeGetVoltage(raoResult, voltageCnec, optState, unit);
        double margin = safeGetMargin(raoResult, voltageCnec, optState, unit);

        if (Double.isNaN(voltage) && Double.isNaN(margin)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(voltage)) {
            jsonGenerator.writeNumberField(VOLTAGE, Math.round(100.0 * voltage) / 100.0);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, Math.round(100.0 * margin) / 100.0);
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForVoltageCnec(RaoResult raoResult, VoltageCnec voltageCnec, Crac crac) {

        if (voltageCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.initial(crac)) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.afterPra(crac));
        } else {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.initial(crac)) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.afterPra(crac)) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.afterAra(crac)) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, OptimizationState.afterCra(crac));
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState) {
        return !Double.isNaN(safeGetVoltage(raoResult, voltageCnec, optState, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMargin(raoResult, voltageCnec, optState, Unit.KILOVOLT));
    }

    private static double safeGetVoltage(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        try {
            return raoResult.getVoltage(optState, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, VoltageCnec voltageCnec, OptimizationState optState, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optState, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
