/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class AngleExtension extends AbstractRaoResultExtension {
    private static final String EXTENSION_NAME = "angle-results";

    private final Map<AngleCnec, AngleCnecResult> results;

    public AngleExtension() {
        this.results = new HashMap<>();
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    /**
     * It gives the angle on an {@link AngleCnec} at a given {@link Instant} and in a
     * given {@link Unit}.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param angleCnec        The angle cnec to be studied.
     * @param unit             The unit in which the flow is queried. Only accepted value for now is DEGREE.
     * @return The angle on the cnec at the optimization state in the given unit.
     */
    public double getAngle(Instant optimizedInstant, AngleCnec angleCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(angleCnec, AngleCnecResult.of(angleCnec)).getAngle(optimizedInstant).orElse(Double.NaN);
    }

    public void addAngle(double angle, Instant optimizedInstant, AngleCnec angleCnec, Unit unit) {
        checkUnit(unit);
        results.computeIfAbsent(angleCnec, k -> AngleCnecResult.of(angleCnec)).addAngleMeasurement(optimizedInstant, angle);
    }

    /**
     * It gives the margin on an {@link AngleCnec} at a given {@link Instant} and in a
     * given {@link Unit}. It is basically the difference between the angle and the most constraining threshold in the
     * angle direction of the given branch. If it is negative the cnec is under constraint.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param angleCnec        The angle cnec to be studied.
     * @param unit             The unit in which the margin is queried. Only accepted for now is DEGREE.
     * @return The margin on the angle cnec at the optimization state in the given unit.
     */
    public double getMargin(Instant optimizedInstant, AngleCnec angleCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(angleCnec, AngleCnecResult.of(angleCnec)).getMargin(optimizedInstant).orElse(Double.NaN);
    }

    private static void checkUnit(Unit unit) {
        if (!Unit.DEGREE.equals(unit)) {
            throw new OpenRaoException("AngleCNEC results are only allowed for degrees.");
        }
    }

    // serialization

    public void serialize(JsonGenerator jsonGenerator) throws IOException {
        jsonGenerator.writeStartArray();
        for (AngleCnec angleCnec : results.keySet().stream().sorted(Comparator.comparing(Identifiable::getId)).toList()) {
            results.get(angleCnec).serialize(jsonGenerator);
        }
        jsonGenerator.writeEndArray();
    }

    // result data model

    private static class AngleCnecResult {
        private final AngleCnec angleCnec;
        private Double initialAngle;
        private Double initialMargin;
        private final Map<Instant, Double> anglePerInstant;
        private final Map<Instant, Double> marginPerInstant;

        public AngleCnecResult(AngleCnec angleCnec) {
            this.angleCnec = angleCnec;
            this.initialAngle = null;
            this.initialMargin = null;
            this.anglePerInstant = new HashMap<>();
            this.marginPerInstant = new HashMap<>();
        }

        public AngleCnec getAngleCnec() {
            return angleCnec;
        }

        public Optional<Double> getAngle(Instant instant) {
            return instant == null ? Optional.ofNullable(initialAngle) : Optional.ofNullable(anglePerInstant.getOrDefault(instant, null));
        }

        public Optional<Double> getMargin(Instant instant) {
            return instant == null ? Optional.ofNullable(initialMargin) : Optional.ofNullable(marginPerInstant.getOrDefault(instant, null));
        }

        public void addAngleMeasurement(Instant instant, double angle) {
            if (instant == null) {
                initialAngle = angle;
                initialMargin = computeMargin(angle);
            } else {
                anglePerInstant.put(instant, angle);
                marginPerInstant.put(instant, computeMargin(angle));
            }
        }

        private double computeMargin(double angle) {
            return Math.min(
                angle - angleCnec.getLowerBound(Unit.DEGREE).orElse(-Double.MAX_VALUE),
                angleCnec.getUpperBound(Unit.DEGREE).orElse(Double.MAX_VALUE) - angle
            );
        }

        public static AngleCnecResult of(AngleCnec angleCnec) {
            return new AngleCnecResult(angleCnec);
        }

        public void serialize(JsonGenerator jsonGenerator) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("angleCnecId", angleCnec.getId());
            jsonGenerator.writeObjectFieldStart("measurements");
            jsonGenerator.writeArrayFieldStart(Unit.DEGREE.name().toLowerCase());
            serializeInitialResults(jsonGenerator);
            for (Instant instant : anglePerInstant.keySet().stream().sorted().toList()) {
                serializeMeasurementsForInstant(jsonGenerator, instant);
            }
            jsonGenerator.writeEndArray();
            jsonGenerator.writeEndObject();
            jsonGenerator.writeEndObject();
        }

        private void serializeInitialResults(JsonGenerator jsonGenerator) throws IOException {
            if (initialAngle != null && initialMargin != null) {
                serializeResults(jsonGenerator, "initial", initialAngle, initialMargin);
            }
        }

        private void serializeMeasurementsForInstant(JsonGenerator jsonGenerator, Instant instant) throws IOException {
            serializeResults(jsonGenerator, instant.getId(), anglePerInstant.get(instant), marginPerInstant.get(instant));
        }

        private static void serializeResults(JsonGenerator jsonGenerator, String instantId, double angle, double margin) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("instant", instantId);
            jsonGenerator.writeNumberField("angle", angle);
            jsonGenerator.writeNumberField("margin", margin);
            jsonGenerator.writeEndObject();
        }
    }
}
