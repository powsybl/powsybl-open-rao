/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.timecoupledconstraints;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Optional;

/**
 * Set of physical and operational constraints that apply on a generator.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public final class AdjustmentConstraints {
    private final String rangeActionId;
    private final Double minimumAdjustmentTime;
    private final Double upwardPowerGradient;
    private final Double downwardPowerGradient;

    private AdjustmentConstraints(String rangeActionId, Double minimumAdjustmentTime, Double upwardPowerGradient, Double downwardPowerGradient) {
        this.rangeActionId = rangeActionId;
        this.minimumAdjustmentTime = minimumAdjustmentTime;
        this.upwardPowerGradient = upwardPowerGradient;
        this.downwardPowerGradient = downwardPowerGradient;
    }

    /**
     * Get the id of the generator on which the constraints apply.
     *
     * @return if of the generator
     */
    public String getRangeActionId() {
        return rangeActionId;
    }

    /**
     * Get the minimum adjustment time of the range action, i.e. how long it should stay at a given power when adjusting.
     *
     * @return minimum adjustment time of the range action
     */
    public Optional<Double> getMinimumAdjustmentTime() {
        return Optional.ofNullable(minimumAdjustmentTime);
    }

    /**
     * Get the upward power gradient of the generator in MW/hours.
     * It only applies when the generator is on, i.e. when its power is greater than pMin.
     * Its value is always positive.
     *
     * @return upward power gradient of the generator
     */
    public Optional<Double> getUpwardPowerGradient() {
        return Optional.ofNullable(upwardPowerGradient);
    }

    /**
     * Get the downward power gradient of the generator in MW/hours.
     * It only applies when the generator is on, i.e. when its power is greater than pMin.
     * Its value is always negative.
     *
     * @return downward power gradient of the generator
     */
    public Optional<Double> getDownwardPowerGradient() {
        return Optional.ofNullable(downwardPowerGradient);
    }

    public static GeneratorConstraintsBuilder create() {
        return new GeneratorConstraintsBuilder();
    }

    public static final class GeneratorConstraintsBuilder {
        private String generatorId;
        private Double minimumAdjustmentTime;
        private Double upwardPowerGradient;
        private Double downwardPowerGradient;

        private GeneratorConstraintsBuilder() {
        }

        public GeneratorConstraintsBuilder withGeneratorId(String generatorId) {
            this.generatorId = generatorId;
            return this;
        }

        public GeneratorConstraintsBuilder withMinimumAdjustmentTime(Double minimumAdjustmentTime) {
            this.minimumAdjustmentTime = minimumAdjustmentTime;
            return this;
        }

        public GeneratorConstraintsBuilder withUpwardPowerGradient(Double upwardPowerGradient) {
            this.upwardPowerGradient = upwardPowerGradient;
            return this;
        }

        public GeneratorConstraintsBuilder withDownwardPowerGradient(Double downwardPowerGradient) {
            this.downwardPowerGradient = downwardPowerGradient;
            return this;
        }

        public AdjustmentConstraints build() {
            if (generatorId == null) {
                throw new OpenRaoException("The id of the generator is mandatory.");
            }
            if (minimumAdjustmentTime != null && minimumAdjustmentTime < 0) {
                throw new OpenRaoException("The minimum adjustment time of the range action must be positive.");
            }
            if (upwardPowerGradient != null && upwardPowerGradient < 0) {
                throw new OpenRaoException("The upward power gradient of the generator must be positive.");
            }
            if (downwardPowerGradient != null && downwardPowerGradient > 0) {
                throw new OpenRaoException("The downward power gradient of the generator must be negative.");
            }
            return new AdjustmentConstraints(generatorId, minimumAdjustmentTime, upwardPowerGradient, downwardPowerGradient);
        }
    }
}
