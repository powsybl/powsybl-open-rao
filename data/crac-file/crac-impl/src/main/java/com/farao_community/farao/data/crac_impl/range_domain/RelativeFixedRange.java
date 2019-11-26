/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.range_domain;

import com.farao_community.farao.data.crac_api.Range;
import com.powsybl.iidm.network.Network;

/**
 * Definition of a range relative to the initial network.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RelativeFixedRange implements Range {

    private double min;
    private double max;

    public RelativeFixedRange(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public double getMinValue(Network network) {
        return -100;
    }

    @Override
    public double getMaxValue(Network network) {
        return -100;
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
