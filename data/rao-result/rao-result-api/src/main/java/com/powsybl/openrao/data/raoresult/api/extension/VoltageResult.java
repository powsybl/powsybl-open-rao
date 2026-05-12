/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class VoltageResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "voltage-results";

    private final Map<VoltageCnec, VoltageCnecResult> results;

    public VoltageResult() {
        this.results = new HashMap<>();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    /**
     * It gives the minimum voltage on an {@link VoltageCnec} at a given {@link Instant} and in a
     * given {@link Unit}.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param voltageCnec      The voltage cnec to be studied.
     * @param unit             The unit in which the flow is queried. Only accepted value for now is KILOVOLT.
     * @return The minimum voltage on the cnec at the optimization state in the given unit.
     */
    public double getMinVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMinVoltage(optimizedInstant).orElse(Double.NaN);
    }

    /**
     * It gives the minimum voltage on an {@link VoltageCnec} at a given {@link Instant} and in a
     * given {@link Unit}.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param voltageCnec      The voltage cnec to be studied.
     * @param unit             The unit in which the flow is queried. Only accepted value for now is KILOVOLT.
     * @return The maximum voltage on the cnec at the optimization state in the given unit.
     */
    public double getMaxVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMaxVoltage(optimizedInstant).orElse(Double.NaN);
    }

    /**
     * It gives the margin on an {@link VoltageCnec} at a given {@link Instant} and in a
     * given {@link Unit}. It is basically the difference between the voltage and the most constraining threshold in the
     * angle direction of the given branch. If it is negative the cnec is under constraint.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param voltageCnec      The voltage cnec to be studied.
     * @param unit             The unit in which the margin is queried. Only accepted for now is DEGREE.
     * @return The margin on the voltage cnec at the optimization state in the given unit.
     */
    public double getMargin(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMargin(optimizedInstant).orElse(Double.NaN);
    }

    public void addMeasurement(double minVoltage, double maxVoltage, Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        results.computeIfAbsent(voltageCnec, k -> VoltageCnecResult.of(voltageCnec)).addVoltageMeasurement(optimizedInstant, minVoltage, maxVoltage);
    }

    private static void checkUnit(Unit unit) {
        if (!Unit.KILOVOLT.equals(unit)) {
            throw new OpenRaoException("VoltageCNEC results are only allowed for kilovolts.");
        }
    }

    // serialization

    public void serialize(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartArray();
        for (VoltageCnec voltageCnec : results.keySet().stream().sorted(Comparator.comparing(Identifiable::getId)).toList()) {
            results.get(voltageCnec).serialize(jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    // result data model

    private static class VoltageCnecResult {
        private final VoltageCnec voltageCnec;
        private Double initialMinVoltage;
        private Double initialMaxVoltage;
        private Double initialMargin;
        private final Map<Instant, Double> minVoltagePerInstant;
        private final Map<Instant, Double> maxVoltagePerInstant;
        private final Map<Instant, Double> marginPerInstant;

        public VoltageCnecResult(VoltageCnec voltageCnec) {
            this.voltageCnec = voltageCnec;
            this.initialMinVoltage = null;
            this.initialMaxVoltage = null;
            this.initialMargin = null;
            this.minVoltagePerInstant = new HashMap<>();
            this.maxVoltagePerInstant = new HashMap<>();
            this.marginPerInstant = new HashMap<>();
        }

        public VoltageCnec getVoltageCnec() {
            return voltageCnec;
        }

        public Optional<Double> getMinVoltage(Instant instant) {
            return instant == null ? Optional.ofNullable(initialMinVoltage) : Optional.ofNullable(minVoltagePerInstant.getOrDefault(instant, null));
        }

        public Optional<Double> getMaxVoltage(Instant instant) {
            return instant == null ? Optional.ofNullable(initialMaxVoltage) : Optional.ofNullable(maxVoltagePerInstant.getOrDefault(instant, null));
        }

        public Optional<Double> getMargin(Instant instant) {
            return instant == null ? Optional.ofNullable(initialMargin) : Optional.ofNullable(marginPerInstant.getOrDefault(instant, null));
        }

        public void addVoltageMeasurement(Instant instant, double minVoltage, double maxVoltage) {
            if (instant == null) {
                initialMinVoltage = minVoltage;
                initialMaxVoltage = maxVoltage;
                initialMargin = computeMargin(minVoltage, maxVoltage);
            } else {
                minVoltagePerInstant.put(instant, minVoltage);
                maxVoltagePerInstant.put(instant, maxVoltage);
                marginPerInstant.put(instant, computeMargin(minVoltage, maxVoltage));
            }
        }

        private double computeMargin(double minVoltage, double maxVoltage) {
            return Math.min(
                minVoltage - voltageCnec.getLowerBound(Unit.KILOVOLT).orElse(-Double.MAX_VALUE),
                voltageCnec.getUpperBound(Unit.KILOVOLT).orElse(Double.MAX_VALUE) - maxVoltage
            );
        }

        public static VoltageCnecResult of(VoltageCnec voltageCnec) {
            return new VoltageCnecResult(voltageCnec);
        }

        public void serialize(JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("voltageCnecId", voltageCnec.getId());
            jsonGenerator.writeObjectFieldStart("measurements");
            jsonGenerator.writeArrayFieldStart(Unit.KILOVOLT.name().toLowerCase());
            serializeInitialResults(jsonGenerator);
            for (Instant instant : minVoltagePerInstant.keySet().stream().sorted().toList()) {
                serializeMeasurementsForInstant(jsonGenerator, instant);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }

        private void serializeInitialResults(JsonGenerator jsonGenerator) throws IOException {
            if (initialMinVoltage != null && initialMaxVoltage != null && initialMargin != null) {
                serializeResults(jsonGenerator, "initial", initialMinVoltage, initialMaxVoltage, initialMargin);
            }
        }

        private void serializeMeasurementsForInstant(JsonGenerator jsonGenerator, Instant instant) throws IOException {
            serializeResults(jsonGenerator, instant.getId(), minVoltagePerInstant.get(instant), maxVoltagePerInstant.get(instant), marginPerInstant.get(instant));
        }

        private static void serializeResults(JsonGenerator jsonGenerator, String instantId, double minVoltage, double maxVoltage, double margin) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("instant", instantId);
            jsonGenerator.writeNumberField("minVoltage", minVoltage);
            jsonGenerator.writeNumberField("maxVoltage", maxVoltage);
            jsonGenerator.writeNumberField("margin", margin);
            jsonGenerator.writeEndObject();
        }
    }
}
