/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.farao_community.farao.data.crac_api.Cnec;
import com.farao_community.farao.data.crac_api.SynchronizationException;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

import static com.farao_community.farao.data.crac_api.Unit.KILOVOLT;

/**
 * Limits for voltage.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class VoltageThreshold extends AbstractThreshold {

    private double minValue;
    private double maxValue;

    @JsonCreator
    public VoltageThreshold(@JsonProperty("minValue") double minValue, @JsonProperty("maxValue") double maxValue) {
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
    public Optional<Double> getMinThreshold() throws SynchronizationException {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getMaxThreshold() throws SynchronizationException {
        return Optional.empty();
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        throw new NotImplementedException("Voltage threshold not implemented");
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        throw new NotImplementedException("Voltage threshold not implemented");
    }

    @Override
    public double computeMargin(Network network, Cnec cnec) throws SynchronizationException {
        return 0;
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
