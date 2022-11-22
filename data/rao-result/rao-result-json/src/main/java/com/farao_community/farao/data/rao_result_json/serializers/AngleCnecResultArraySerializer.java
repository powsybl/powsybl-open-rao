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
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
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
final class AngleCnecResultArraySerializer {

    private AngleCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<AngleCnec> sortedListOfAngleCnecs = crac.getAngleCnecs().stream()
            .sorted(Comparator.comparing(AngleCnec::getId))
            .collect(Collectors.toList());

        jsonGenerator.writeArrayFieldStart(ANGLECNEC_RESULTS);
        for (AngleCnec angleCnec : sortedListOfAngleCnecs) {
            serializeAngleCnecResult(angleCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeAngleCnecResult(AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForAngleCnec(raoResult, angleCnec)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(ANGLECNEC_ID, angleCnec.getId());

            serializeAngleCnecResultForOptimizationState(OptimizationState.INITIAL, angleCnec, raoResult, jsonGenerator);
            serializeAngleCnecResultForOptimizationState(OptimizationState.AFTER_PRA, angleCnec, raoResult, jsonGenerator);

            if (!angleCnec.getState().isPreventive()) {
                serializeAngleCnecResultForOptimizationState(OptimizationState.AFTER_ARA, angleCnec, raoResult, jsonGenerator);
                serializeAngleCnecResultForOptimizationState(OptimizationState.AFTER_CRA, angleCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationState(OptimizationState optState, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, angleCnec, optState)) {
            jsonGenerator.writeObjectFieldStart(serializeOptimizationState(optState));
            serializeAngleCnecResultForOptimizationStateAndUnit(optState, Unit.DEGREE, angleCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationStateAndUnit(OptimizationState optState, Unit unit, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double angle = safeGetAngle(raoResult, angleCnec, optState, unit);
        double margin = safeGetMargin(raoResult, angleCnec, optState, unit);

        if (Double.isNaN(angle) && Double.isNaN(margin)) {
            return;
        }

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(angle)) {
            jsonGenerator.writeNumberField(ANGLE, Math.round(100.0 * angle) / 100.0);
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, Math.round(100.0 * margin) / 100.0);
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForAngleCnec(RaoResult raoResult, AngleCnec angleCnec) {

        if (angleCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.INITIAL) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.AFTER_PRA);
        } else {
            return containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.INITIAL) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.AFTER_PRA) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.AFTER_ARA) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, OptimizationState.AFTER_CRA);
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, AngleCnec angleCnec, OptimizationState optState) {
        return !Double.isNaN(safeGetAngle(raoResult, angleCnec, optState, Unit.DEGREE)) ||
            !Double.isNaN(safeGetMargin(raoResult, angleCnec, optState, Unit.DEGREE));
    }

    private static double safeGetAngle(RaoResult raoResult, AngleCnec angleCnec, OptimizationState optState, Unit unit) {
        // methods getAngle can return an exception if RAO is executed on one state only
        try {
            return raoResult.getAngle(optState, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, AngleCnec angleCnec, OptimizationState optState, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optState, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
