/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.intertemporalconstraint;

import com.powsybl.openrao.commons.OpenRaoException;

import java.util.Optional;

// TODO: delete this class and this module and replace by GeneratorConstraints

/**
 * Power Gradient (in MW/hour) that applies on a generator or a load.
 * It has a negative minimum value and/or a positive maximum value.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
@Deprecated
public final class PowerGradient {
    private final String networkElementId;
    private final Double minValue;
    private final Double maxValue;

    public PowerGradient(String networkElementId, Double minValue, Double maxValue) {
        this.networkElementId = networkElementId;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public String getNetworkElementId() {
        return networkElementId;
    }

    public Optional<Double> getMinValue() {
        return Optional.ofNullable(minValue);
    }

    public Optional<Double> getMaxValue() {
        return Optional.ofNullable(maxValue);
    }

    public static PowerGradientBuilder builder() {
        return new PowerGradientBuilder();
    }

    public static final class PowerGradientBuilder {
        private String networkElementId;
        private Double minValue;
        private Double maxValue;

        private PowerGradientBuilder() {
        }

        public PowerGradientBuilder withNetworkElementId(String networkElementId) {
            this.networkElementId = networkElementId;
            return this;
        }

        public PowerGradientBuilder withMinValue(Double minPowerGradient) {
            this.minValue = minPowerGradient;
            return this;
        }

        public PowerGradientBuilder withMaxValue(Double maxPowerGradient) {
            this.maxValue = maxPowerGradient;
            return this;
        }

        public PowerGradient build() {
            if (networkElementId == null) {
                throw new OpenRaoException("No network element id provided.");
            }
            if (minValue == null && maxValue == null) {
                throw new OpenRaoException("At least one of min or max power gradient's value must be provided.");
            }
            if (minValue != null && minValue > 0) {
                throw new OpenRaoException("The min value of the power gradient must be negative.");
            }
            if (maxValue != null && maxValue < 0) {
                throw new OpenRaoException("The max value of the power gradient must be positive.");
            }
            return new PowerGradient(networkElementId, minValue, maxValue);
        }
    }
}
