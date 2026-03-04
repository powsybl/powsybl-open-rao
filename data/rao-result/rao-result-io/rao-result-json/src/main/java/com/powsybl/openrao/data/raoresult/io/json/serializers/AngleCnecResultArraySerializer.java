/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class AngleCnecResultArraySerializer {

    private AngleCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<AngleCnec> sortedListOfAngleCnecs = crac.getAngleCnecs().stream()
            .filter(angleCnec -> angleCnec.getState().getInstant() == crac.getPreventiveInstant() || (angleCnec.getState().getInstant() == crac.getLastInstant() & angleCnec.getState().getInstant().isCurative()))
            .sorted(Comparator.comparing(AngleCnec::getId))
            .toList();

        jsonGenerator.writeArrayFieldStart(RaoResultJsonConstants.ANGLECNEC_RESULTS);
        for (AngleCnec angleCnec : sortedListOfAngleCnecs) {
            serializeAngleCnecResult(angleCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeAngleCnecResult(AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForAngleCnec(angleCnec, raoResult)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(RaoResultJsonConstants.ANGLECNEC_ID, angleCnec.getId());
            serializeAngleCnecResultForOptimizationState(angleCnec.getState().getInstant(), angleCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationState(Instant optInstant, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, angleCnec, optInstant)) {
            jsonGenerator.writeObjectFieldStart(RaoResultJsonConstants.serializeInstantId(optInstant));
            serializeAngleCnecResultForOptimizationStateAndUnit(optInstant, Unit.DEGREE, angleCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeAngleCnecResultForOptimizationStateAndUnit(Instant optInstant, Unit unit, AngleCnec angleCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double angle = raoResult.getAngle(optInstant, angleCnec, Unit.DEGREE);
        double margin = raoResult.getMargin(optInstant, angleCnec, Unit.DEGREE);

        jsonGenerator.writeObjectFieldStart(RaoResultJsonConstants.serializeUnit(unit));
        if (!Double.isNaN(angle)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.ANGLE, roundValueBasedOnMargin(angle, margin, 2));
        }
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(RaoResultJsonConstants.MARGIN, roundValueBasedOnMargin(margin, margin, 2));
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForAngleCnec(AngleCnec angleCnec, RaoResult raoResult) {
        return containsAnyResultForOptimizationState(raoResult, angleCnec, angleCnec.getState().getInstant());
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, AngleCnec angleCnec, Instant optInstant) {
        return !Double.isNaN(raoResult.getAngle(optInstant, angleCnec, Unit.DEGREE)) ||
            !Double.isNaN(raoResult.getMargin(optInstant, angleCnec, Unit.DEGREE));
    }
}
