/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.farao_community.farao.data.crac_api.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Network;

/**
 * Definition of an absolute range.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class AbsoluteFixedRange implements Range {

    private double min;
    private double max;

    @JsonCreator
    public AbsoluteFixedRange(@JsonProperty("min") double min, @JsonProperty("max") double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public double getMinValue(Network network) {
        return min;
    }

    @Override
    public double getMaxValue(Network network) {
        return max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }
}
