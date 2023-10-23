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
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.VoltageCnec;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

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
            serializeVoltageCnecResultForOptimizationState(crac.getUniqueInstant(InstantKind.PREVENTIVE).getId(), voltageCnec, raoResult, jsonGenerator);

            if (!voltageCnec.getState().isPreventive()) {
                serializeVoltageCnecResultForOptimizationState(crac.getUniqueInstant(InstantKind.AUTO).getId(), voltageCnec, raoResult, jsonGenerator);
                serializeVoltageCnecResultForOptimizationState(crac.getUniqueInstant(InstantKind.CURATIVE).getId(), voltageCnec, raoResult, jsonGenerator);
            }
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationState(String optInstantId, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        if (containsAnyResultForOptimizationState(raoResult, voltageCnec, optInstantId)) {
            jsonGenerator.writeObjectFieldStart(optInstantId);
            serializeVoltageCnecResultForOptimizationStateAndUnit(optInstantId, Unit.KILOVOLT, voltageCnec, raoResult, jsonGenerator);
            jsonGenerator.writeEndObject();
        }
    }

    private static void serializeVoltageCnecResultForOptimizationStateAndUnit(String optInstantId, Unit unit, VoltageCnec voltageCnec, RaoResult raoResult, JsonGenerator jsonGenerator) throws IOException {

        double voltage = safeGetVoltage(raoResult, voltageCnec, optInstantId, unit);
        double margin = safeGetMargin(raoResult, voltageCnec, optInstantId, unit);

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
                containsAnyResultForOptimizationState(raoResult, voltageCnec, voltageCnec.getState().getInstant().getId());
        } else {
            return containsAnyResultForOptimizationState(raoResult, voltageCnec, null) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, crac.getUniqueInstant(InstantKind.PREVENTIVE).getId()) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, crac.getUniqueInstant(InstantKind.AUTO).getId()) ||
                containsAnyResultForOptimizationState(raoResult, voltageCnec, crac.getUniqueInstant(InstantKind.CURATIVE).getId());
        }
    }

    private static boolean containsAnyResultForOptimizationState(RaoResult raoResult, VoltageCnec voltageCnec, String optInstantId) {
        return !Double.isNaN(safeGetVoltage(raoResult, voltageCnec, optInstantId, Unit.KILOVOLT)) ||
            !Double.isNaN(safeGetMargin(raoResult, voltageCnec, optInstantId, Unit.KILOVOLT));
    }

    private static double safeGetVoltage(RaoResult raoResult, VoltageCnec voltageCnec, String optInstantId, Unit unit) {
        // methods getVoltage can return an exception if RAO is executed on one state only
        try {
            return raoResult.getVoltage(optInstantId, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }

    private static double safeGetMargin(RaoResult raoResult, VoltageCnec voltageCnec, String optInstantId, Unit unit) {
        // methods getMargin can return an exception if RAO is executed on one state only
        try {
            return raoResult.getMargin(optInstantId, voltageCnec, unit);
        } catch (FaraoException e) {
            return Double.NaN;
        }
    }
}
