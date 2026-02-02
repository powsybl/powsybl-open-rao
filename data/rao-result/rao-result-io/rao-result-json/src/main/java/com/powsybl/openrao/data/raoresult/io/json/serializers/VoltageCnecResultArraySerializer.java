/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.io.json.serializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.function.TriFunction;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.commons.MeasurementRounding.roundValueBasedOnMargin;
import static com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonConstants.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
final class VoltageCnecResultArraySerializer {

    private VoltageCnecResultArraySerializer() {
    }

    static void serialize(RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        List<VoltageCnec> sortedListOfVoltageCnecs = crac.getVoltageCnecs().stream()
            .sorted(Comparator.comparing(VoltageCnec::getId))
            .toList();

        jsonGenerator.writeArrayFieldStart(VOLTAGECNEC_RESULTS);
        for (VoltageCnec voltageCnec : sortedListOfVoltageCnecs) {
            serializeVoltageCnecResult(voltageCnec, raoResult, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeVoltageCnecResult(VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {
        if (containsAnyResultForVoltageCnec(raoResult, voltageCnec)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(VOLTAGECNEC_ID, voltageCnec.getId());
            serializeVoltageCnecResultForOptimizationState(voltageCnec.getState().getInstant(), voltageCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationState(Instant optInstant, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, voltageCnec, optInstant)) {
            jsonGenerator.writeObjectFieldStart(serializeInstantId(optInstant));
            serializeVoltageCnecResultForOptimizationStateAndUnit(optInstant, Unit.KILOVOLT, voltageCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationStateAndUnit(Instant optInstant, Unit unit, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double minVoltage = safeGetMinVoltage(raoResult, voltageCnec, optInstant, unit);
        double maxVoltage = safeGetMaxVoltage(raoResult, voltageCnec, optInstant, unit);
        double margin = safeGetMargin(raoResult, voltageCnec, optInstant, unit);

        jsonGenerator.writeObjectFieldStart(serializeUnit(unit));
        if (!Double.isNaN(margin)) {
            jsonGenerator.writeNumberField(MARGIN, roundValueBasedOnMargin(margin, margin, 2));
        }
        if (!Double.isNaN(minVoltage)) {
            jsonGenerator.writeNumberField(MIN_VOLTAGE, roundValueBasedOnMargin(minVoltage, margin, 2));
        }
        if (!Double.isNaN(maxVoltage)) {
            jsonGenerator.writeNumberField(MAX_VOLTAGE, roundValueBasedOnMargin(maxVoltage, margin, 2));
        }
        jsonGenerator.writeEndObject();
    }

    private static boolean containsAnyResultForVoltageCnec(RaoResult raoResult, VoltageCnec voltageCnec) {

        return containsAnyResultForOptimizationState(raoResult, voltageCnec, voltageCnec.getState().getInstant());
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant) {
        return !Double.isNaN(safeGetMaxVoltage(raoResult, voltageCnec, optInstant, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMinVoltage(raoResult, voltageCnec, optInstant, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMargin(raoResult, voltageCnec, optInstant, Unit.KILOVOLT));
    }

    private static double safeGetMinVoltage(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        return safeGetValue(raoResult::getMinVoltage, optInstant, voltageCnec, unit);
    }

    private static double safeGetMaxVoltage(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        return safeGetValue(raoResult::getMaxVoltage, optInstant, voltageCnec, unit);
    }

    private static double safeGetMargin(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        return safeGetValue(raoResult::getMargin, optInstant, voltageCnec, unit);
    }

    private static double safeGetValue(TriFunction<Instant, VoltageCnec, Unit, Double> callable, Instant optInstant, VoltageCnec voltageCnec, Unit unit) {
        try {
            return callable.apply(optInstant, voltageCnec, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }
}
