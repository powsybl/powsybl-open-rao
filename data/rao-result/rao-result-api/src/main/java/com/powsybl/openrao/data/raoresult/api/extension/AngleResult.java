/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.raoresult.api.extension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class AngleResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "angle-results";

    private final Map<AngleCnec, AngleCnecResult> results;

    private AngleResult(Map<AngleCnec, AngleCnecResult> results) {
        this.results = new HashMap<>();
        results.forEach((angleCnec, angleCnecResult) ->
            this.results.put(angleCnec, new AngleCnecResult(angleCnecResult)));
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

    // result data model

    private static class AngleCnecResult {
        private final AngleCnec angleCnec;
        private Double initialAngle;
        private Double initialMargin;
        private final Map<Instant, Double> anglePerInstant;
        private final Map<Instant, Double> marginPerInstant;

        AngleCnecResult(AngleCnec angleCnec) {
            this.angleCnec = angleCnec;
            this.initialAngle = null;
            this.initialMargin = null;
            this.anglePerInstant = new HashMap<>();
            this.marginPerInstant = new HashMap<>();
        }

        AngleCnecResult(AngleCnecResult angleCnecResult) {
            this.angleCnec = angleCnecResult.angleCnec;
            this.initialAngle = angleCnecResult.initialAngle;
            this.initialMargin = angleCnecResult.initialMargin;
            this.anglePerInstant = new HashMap<>();
            this.anglePerInstant.putAll(angleCnecResult.anglePerInstant);
            this.marginPerInstant = new HashMap<>();
            this.marginPerInstant.putAll(angleCnecResult.marginPerInstant);
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AngleResult.AngleCnecResult that = (AngleResult.AngleCnecResult) o;
            return angleCnec.equals(that.angleCnec);
        }

        @Override
        public int hashCode() {
            return Objects.hash(angleCnec);
        }
    }

    // builder

    public static AngleResult.AngleResultBuilder build() {
        return new AngleResult.AngleResultBuilder();
    }

    public static final class AngleResultBuilder {
        private final Map<AngleCnec, AngleCnecResult> results;

        private AngleResultBuilder() {
            this.results = new HashMap<>();
        }

        public AngleResultBuilder addMeasurement(double angle, Instant instant, AngleCnec angleCnec, Unit unit) {
            checkUnit(unit);
            results.computeIfAbsent(angleCnec, AngleResult.AngleCnecResult::new).addAngleMeasurement(instant, angle);
            return this;
        }

        public AngleResult build() {
            return new AngleResult(results);
        }
    }
}
