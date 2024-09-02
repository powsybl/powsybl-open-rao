/*
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.raoresultjson.serializers;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.openrao.data.raoresultjson.RaoResultJsonConstants.*;

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
            serializeVoltageCnecResult(voltageCnec, raoResult, crac, jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    private static void serializeVoltageCnecResult(VoltageCnec voltageCnec, RaoResult raoResult, Crac crac, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForVoltageCnec(raoResult, crac, voltageCnec)) {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField(VOLTAGECNEC_ID, voltageCnec.getId());

            serializeVoltageCnecResultForOptimizationState(null, voltageCnec, raoResult, jsonGenerator);
            serializeVoltageCnecResultForOptimizationState(crac.getPreventiveInstant(), voltageCnec, raoResult, jsonGenerator);

            if (!voltageCnec.getState().isPreventive()) {
                serializeVoltageCnecResultForOptimizationState(crac.getInstant(InstantKind.AUTO), voltageCnec, raoResult, jsonGenerator);
                crac.getInstants(InstantKind.CURATIVE).stream().sorted(Comparator.comparingInt(Instant::getOrder)).forEach(
                    curativeInstant -> {
                        try {
                            serializeVoltageCnecResultForOptimizationState(curativeInstant, voltageCnec, raoResult, jsonGenerator);
                        } catch (IOException e) {
                            throw new OpenRaoException("An error occured when serializing Voltage Cnec results", e);
                        }
                    }
                );
            }
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

        double voltage = safeGetVoltage(raoResult, voltageCnec, optInstant, unit);
        double margin = safeGetMargin(raoResult, voltageCnec, optInstant, unit);

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

    private static boolean containsAnyResultForVoltageCnec(RaoResult raoResult, Crac crac, VoltageCnec voltageCnec) {

        if (voltageCnec.getState().isPreventive()) {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, voltageCnec.getState().getInstant());
        } else {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, crac.getPreventiveInstant()) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, crac.getInstant(InstantKind.AUTO)) ||
                crac.getInstants(InstantKind.CURATIVE).stream().anyMatch(curativeInstant -> containsAnyResultForOptimizationState(raoResult, voltageCnec, curativeInstant));
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant) {
        return !Double.isNaN(safeGetVoltage(raoResult, voltageCnec, optInstant, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMargin(raoResult, voltageCnec, optInstant, Unit.KILOVOLT));
    }

    private static double safeGetVoltage(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        try {
            return raoResult.getVoltage(optInstant, voltageCnec, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, VoltageCnec voltageCnec, Instant optInstant, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstant, voltageCnec, unit);
        } catch (OpenRaoException e) {
            return Double.NaN;
        }
    }
}
