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
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.raoresult.api.RaoResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class VoltageResult extends AbstractExtension<RaoResult> {
    private static final String EXTENSION_NAME = "voltage-results";

    private final Map<VoltageCnec, VoltageCnecResult> results;

    private VoltageResult(Map<VoltageCnec, VoltageCnecResult> results) {
        this.results = new HashMap<>(results);
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
     * @param voltageCnec      The VoltageCNEC to be studied.
     * @param unit             The unit in which the voltage is queried. The only accepted value for now is KILOVOLT.
     * @return The minimum voltage on the CNEC at the optimization state in the given unit.
     */
    public double getMinVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMinVoltage(optimizedInstant).orElse(Double.NaN);
    }

    /**
     * It gives the maximum voltage on an {@link VoltageCnec} at a given {@link Instant} and in a
     * given {@link Unit}.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param voltageCnec      The VoltageCNEC to be studied.
     * @param unit             The unit in which the voltage is queried. The only accepted value for now is KILOVOLT.
     * @return The maximum voltage on the CNEC at the optimization state in the given unit.
     */
    public double getMaxVoltage(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMaxVoltage(optimizedInstant).orElse(Double.NaN);
    }

    /**
     * It gives the margin on an {@link VoltageCnec} at a given {@link Instant} and in a
     * given {@link Unit}. It is basically the difference between the voltage and the most constraining threshold in the
     * voltage direction of the given branch. If it is negative, the cnec is under constraint.
     *
     * @param optimizedInstant The optimized instant to be studied (set to null to access initial results)
     * @param voltageCnec      The VoltageCNEC to be studied.
     * @param unit             The unit in which the margin is queried. The only accepted value for now is KILOVOLT.
     * @return The margin on the VoltageCNEC at the optimization state in the given unit.
     */
    public double getMargin(Instant optimizedInstant, VoltageCnec voltageCnec, Unit unit) {
        checkUnit(unit);
        return results.getOrDefault(voltageCnec, VoltageCnecResult.of(voltageCnec)).getMargin(optimizedInstant).orElse(Double.NaN);
    }

    private static void checkUnit(Unit unit) {
        if (!Unit.KILOVOLT.equals(unit)) {
            throw new OpenRaoException("VoltageCNEC results are only allowed for kilovolts.");
        }
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

        VoltageCnecResult(VoltageCnec voltageCnec) {
            this.voltageCnec = voltageCnec;
            this.initialMinVoltage = null;
            this.initialMaxVoltage = null;
            this.initialMargin = null;
            this.minVoltagePerInstant = new HashMap<>();
            this.maxVoltagePerInstant = new HashMap<>();
            this.marginPerInstant = new HashMap<>();
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
    }

    // builder

    public static VoltageResult.VoltageResultBuilder build() {
        return new VoltageResult.VoltageResultBuilder();
    }

    public static final class VoltageResultBuilder {
        private final Map<VoltageCnec, VoltageCnecResult> results;

        private VoltageResultBuilder() {
            results = new HashMap<>();
        }

        public VoltageResult.VoltageResultBuilder addMeasurement(double minVoltage, double maxVoltage, Instant instant, VoltageCnec voltageCnec, Unit unit) {
            checkUnit(unit);
            results.computeIfAbsent(voltageCnec, VoltageCnecResult::new).addVoltageMeasurement(instant, minVoltage, maxVoltage);
            return this;
        }

        public VoltageResult build() {
            return new VoltageResult(results);
        }
    }
}
