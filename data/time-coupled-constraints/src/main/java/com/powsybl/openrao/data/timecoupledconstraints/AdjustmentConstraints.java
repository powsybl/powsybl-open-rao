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
 * Set of physical and operational constraints that apply on an adjustment.
 *
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
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
     * Get the id of the range action on which the constraints apply.
     *
     * @return id of the range action
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
     * Get the upward power gradient of the range action in MW/h (injections) or degrees/h (psts).
     * It only applies when the generator is being adjusted, i.e. when its power is different from the initial value in the network.
     * Its value is always positive.
     *
     * @return upward power gradient of the range action
     */
    public Optional<Double> getUpwardPowerGradient() {
        return Optional.ofNullable(upwardPowerGradient);
    }

    /**
     * Get the downward power gradient of the range action in MW/h (injections) or degrees/h (psts).
     * It only applies when the generator is being adjusted, i.e. when its power is different from the initial value in the network.
     * Its value is always negative.
     *
     * @return downward power gradient of the range action
     */
    public Optional<Double> getDownwardPowerGradient() {
        return Optional.ofNullable(downwardPowerGradient);
    }

    public static AdjustmentConstraintsBuilder create() {
        return new AdjustmentConstraintsBuilder();
    }

    public static final class AdjustmentConstraintsBuilder {
        private String rangeActionId;
        private Double minimumAdjustmentTime;
        private Double upwardPowerGradient;
        private Double downwardPowerGradient;

        private AdjustmentConstraintsBuilder() {
        }

        public AdjustmentConstraintsBuilder withRangeActionId(String rangeActionId) {
            this.rangeActionId = rangeActionId;
            return this;
        }

        public AdjustmentConstraintsBuilder withMinimumAdjustmentTime(Double minimumAdjustmentTime) {
            this.minimumAdjustmentTime = minimumAdjustmentTime;
            return this;
        }

        public AdjustmentConstraintsBuilder withUpwardPowerGradient(Double upwardPowerGradient) {
            this.upwardPowerGradient = upwardPowerGradient;
            return this;
        }

        public AdjustmentConstraintsBuilder withDownwardPowerGradient(Double downwardPowerGradient) {
            this.downwardPowerGradient = downwardPowerGradient;
            return this;
        }

        public AdjustmentConstraints build() {
            if (rangeActionId == null) {
                throw new OpenRaoException("The id of the range action is mandatory.");
            }
            if (minimumAdjustmentTime != null && minimumAdjustmentTime < 0) {
                throw new OpenRaoException("The minimum adjustment time of the range action must be positive.");
            }
            if (upwardPowerGradient != null && upwardPowerGradient < 0) {
                throw new OpenRaoException("The upward power gradient of the range action must be positive.");
            }
            if (downwardPowerGradient != null && downwardPowerGradient > 0) {
                throw new OpenRaoException("The downward power gradient of the range action must be negative.");
            }
            return new AdjustmentConstraints(rangeActionId, minimumAdjustmentTime, upwardPowerGradient, downwardPowerGradient);
        }
    }
}
