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
import java.util.Optional;

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
            .toList();

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
            Instant instantPrev = crac.getInstant(InstantKind.PREVENTIVE);
            serializeAngleCnecResultForOptimizationState(instantPrev.getId(), angleCnec, raoResult, jsonGenerator);

            if (!angleCnec.getState().isPreventive()) {
                Instant instantAuto = crac.getInstant(InstantKind.AUTO);
                Instant instantCurative = crac.getInstant(InstantKind.CURATIVE);
                serializeAngleCnecResultForOptimizationState(instantAuto.getId(), angleCnec, raoResult, jsonGenerator);
                serializeAngleCnecResultForOptimizationState(instantCurative.getId(), angleCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationState(String optInstantId, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, angleCnec, optInstantId)) {
            jsonGenerator.writeObjectFieldStart(Optional.ofNullable(optInstantId).orElse("")); // TODO use serializer ?
            serializeAngleCnecResultForOptimizationStateAndUnit(optInstantId, Unit.DEGREE, angleCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationStateAndUnit(String optInstantId, Unit unit, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double angle = safeGetAngle(raoResult, angleCnec, optInstantId, unit);
        double margin = safeGetMargin(raoResult, angleCnec, optInstantId, unit);

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
                containsAnyResultForOptimizationState(raoResult, angleCnec, angleCnec.getState().getInstant().getId());
        } else {
            Instant instantPrev = crac.getInstant(InstantKind.PREVENTIVE);
            Instant instantAuto = crac.getInstant(InstantKind.AUTO);
            Instant instantCurative = crac.getInstant(InstantKind.CURATIVE);
            return containsAnyResultForOptimizationState(raoResult, angleCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, instantPrev.getId()) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, instantAuto.getId()) ||
                containsAnyResultForOptimizationState(raoResult, angleCnec, instantCurative.getId());
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, AngleCnec angleCnec, String optInstantId) {
        return !Double.isNaN(safeGetAngle(raoResult, angleCnec, optInstantId, Unit.DEGREE)) ||
            !Double.isNaN(safeGetMargin(raoResult, angleCnec, optInstantId, Unit.DEGREE));
    }

    private static double safeGetAngle(RaoResult raoResult, AngleCnec angleCnec, String optInstantId, Unit unit) {
        // methods getAngle can return an exception if RAO is executed on one state only
        try {
            return raoResult.getAngle(optInstantId, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, AngleCnec angleCnec, String optInstantId, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstantId, angleCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
