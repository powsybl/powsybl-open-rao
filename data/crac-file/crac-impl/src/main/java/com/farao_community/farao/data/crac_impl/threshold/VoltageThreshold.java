/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.PhysicalParameter;
import com.farao_community.farao.data.crac_api.Unit;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Optional;

import static com.farao_community.farao.data.crac_api.Unit.KILOVOLT;

/**
 * Limits for voltage.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("voltage-threshold")
public class VoltageThreshold extends AbstractThreshold {

    private double minValue;
    private double maxValue;

    @JsonCreator
    public VoltageThreshold(@JsonProperty("minValue") double minValue, @JsonProperty("maxValue") double maxValue) {
        super(KILOVOLT);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.VOLTAGE;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    @Override
    public Optional<Double> getMinThreshold(Unit unit) {
        if (unit.equals(KILOVOLT)) {
            return Optional.empty();
        } else {
            throw new FaraoException(String.format("Unit of voltage threshold should be KILOVOLT, %s is not a valid value", unit.toString()));
        }
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit unit) {
        if (unit.equals(KILOVOLT)) {
            return Optional.empty();
        } else {
            throw new FaraoException(String.format("Unit of voltage threshold should be KILOVOLT, %s is not a valid value", unit.toString()));
        }
    }

    @Override
    public AbstractThreshold copy() {
        return new VoltageThreshold(minValue, maxValue);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VoltageThreshold threshold = (VoltageThreshold) o;
        return minValue == threshold.minValue && maxValue == threshold.maxValue;
    }

    @Override
    public int hashCode() {
        int result = (int) minValue * 100;
        result = 31 * result + (int) maxValue * 100;
        return result;
    }
}
