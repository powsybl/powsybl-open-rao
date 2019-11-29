/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import static com.farao_community.farao.data.crac_api.Unit.KILOVOLT;

/**
 * Limits for voltage.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public class VoltageThreshold extends AbstractThreshold {

    private double minValue;
    private double maxValue;

    public VoltageThreshold(double minValue, double maxValue) {
        super(KILOVOLT);
        this.minValue = minValue;
        this.maxValue = maxValue;
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
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        throw new NotImplementedException("Voltage threshold not implemented");
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        throw new NotImplementedException("Voltage threshold not implemented");
    }
}
