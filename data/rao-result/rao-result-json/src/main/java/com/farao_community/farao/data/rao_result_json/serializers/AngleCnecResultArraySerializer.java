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
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.AngleCnec;
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
            serializeAngleCnecResult(angleCnec, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeAngleCnecResult(AngleCnec angleCnec, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForAngleCnec(angleCnec, raoResult, crac)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(ANGLECNEC_ID, angleCnec.getId());

            serializeAngleCnecResultForOptimizationState(null, angleCnec, raoResult, jsonGenerator);
            serializeAngleCnecResultForOptimizationState(crac.getInstant(InstantKind.PREVENTIVE), angleCnec, raoResult, jsonGenerator);

            if (!angleCnec.getState().isPreventive()) {
                serializeAngleCnecResultForOptimizationState(crac.getInstant(InstantKind.AUTO), angleCnec, raoResult, jsonGenerator);
                serializeAngleCnecResultForOptimizationState(crac.getInstant(InstantKind.CURATIVE), angleCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationState(Instant optInstant, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, angleCnec, optInstant)) {
            jsonGenerator.writeObjectFieldStart(serializeInstantId(optInstant));
            serializeAngleCnecResultForOptimizationStateAndUnit(optInstant, Unit.DEGREE, angleCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationStateAndUnit(Instant optInstant, Unit unit, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double angle = safeGetAngle(raoResult, angleCnec, optInstant, unit);
        double margin = safeGetMargin(raoResult, angleCnec, optInstant, unit);

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

    private static boolean containsAnyResultForAngleCnec(AngleCnec angleCnec, RaoResult raoResult, Crac crac) {

        if (angleCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, angleCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, angleCnec.getState().getInstant());
        } else {
            return containsAnyResultForOptimizationState(raoResult, angleCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, crac.getInstant(InstantKind.PREVENTIVE)) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, crac.getInstant(InstantKind.AUTO)) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, crac.getInstant(InstantKind.CURATIVE));
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, AngleCnec angleCnec, Instant optInstant) {
        return !Double.isNaN(safeGetAngle(raoResult, angleCnec, optInstant, Unit.DEGREE)) ||
            !Double.isNaN(safeGetMargin(raoResult, angleCnec, optInstant, Unit.DEGREE));
    }

    private static double safeGetAngle(RaoResult raoResult, AngleCnec angleCnec, Instant optInstant, Unit unit) {
        // methods getAngle can return an exception if RAO is executed on one state only
        try {
            return raoResult.getAngle(optInstant, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, AngleCnec angleCnec, Instant optInstant, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstant, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
