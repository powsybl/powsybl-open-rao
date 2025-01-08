/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraint;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Optional;

/**
 * Power Gradient Constraint that applies on a generator or a load.
 * It is always positive and represents the rate of change of the set-point (in MW/hour) and
 * can apply either for upward or downward variation.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public final class PowerGradientConstraint {
    private final String networkElementId;
    private final Double minPowerGradient;
    private final Double maxPowerGradient;

    private PowerGradientConstraint(String networkElementId, Double minPowerGradient, Double maxPowerGradient) {
        this.networkElementId = networkElementId;
        this.minPowerGradient = minPowerGradient;
        this.maxPowerGradient = maxPowerGradient;
    }

    public String getNetworkElementId() {
        return networkElementId;
    }

    public Optional<Double> getMinPowerGradient() {
        return Optional.ofNullable(minPowerGradient);
    }

    public Optional<Double> getMaxPowerGradient() {
        return Optional.ofNullable(maxPowerGradient);
    }

    public static PowerGradientConstraintBuilder builder() {
        return new PowerGradientConstraintBuilder();
    }

    public static final class PowerGradientConstraintBuilder {
        private String networkElementId;
        private Double minPowerGradient;
        private Double maxPowerGradient;

        private PowerGradientConstraintBuilder() {
        }

        public PowerGradientConstraintBuilder withNetworkElementId(String networkElementId) {
            this.networkElementId = networkElementId;
            return this;
        }

        public PowerGradientConstraintBuilder withMinPowerGradient(Double minPowerGradient) {
            this.minPowerGradient = minPowerGradient;
            return this;
        }

        public PowerGradientConstraintBuilder withMaxPowerGradient(Double maxPowerGradient) {
            this.maxPowerGradient = maxPowerGradient;
            return this;
        }

        public PowerGradientConstraint build() {
            if (networkElementId == null) {
                throw new OpenRaoException("The network element id of the gradient constraint must be provided.");
            }
            if (minPowerGradient == null && maxPowerGradient == null) {
                throw new OpenRaoException("At least one min or max power gradient value must be provided.");
            }
            if (minPowerGradient != null && minPowerGradient > 0) {
                throw new OpenRaoException("The min power gradient must be negative.");
            }
            if (maxPowerGradient != null && maxPowerGradient < 0) {
                throw new OpenRaoException("The max power gradient must be positive.");
            }
            return new PowerGradientConstraint(networkElementId, minPowerGradient, maxPowerGradient);
        }
    }
}
