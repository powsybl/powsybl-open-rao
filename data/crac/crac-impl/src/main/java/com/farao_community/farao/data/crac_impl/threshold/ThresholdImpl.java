/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.Threshold;
import com.fasterxml.jackson.annotation.*;

import java.util.Optional;

/**
 * Generic threshold (flow, voltage, etc.) in the CRAC file.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("threshold")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BranchThresholdImpl.class, name = "branch-threshold")
    })
public class ThresholdImpl implements Threshold {

    protected Unit unit;
    protected Double min;
    protected Double max;

    public ThresholdImpl(Unit unit) {
        this.unit = unit;
    }

    @JsonCreator
    public ThresholdImpl(@JsonProperty("unit") Unit unit,
                         @JsonProperty("min") Double min,
                         @JsonProperty("max") Double max) {
        this.unit = unit;
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min == null ? Double.NaN : min;
    }

    public double getMax() {
        return max == null ? Double.NaN : max;
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Optional<Double> min() {
        // So that it returns Optional.empty() if min value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMin()) ? null : min);
    }

    @Override
    public Optional<Double> max() {
        // So that it returns Optional.empty() if max value is null or NaN
        return Optional.ofNullable(Double.isNaN(getMax()) ? null : max);
    }
}
