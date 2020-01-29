/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao.mocks;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ThresholdMock implements Threshold {
    private double min;
    private double max;

    public ThresholdMock(double min, double max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public Optional<Double> getMinThreshold() throws SynchronizationException {
        return Optional.of(min);
    }

    @Override
    public Optional<Double> getMinThreshold(Unit unit) throws SynchronizationException {
        return Optional.of(min);
    }

    @Override
    public Optional<Double> getMaxThreshold() throws SynchronizationException {
        return Optional.of(max);
    }

    @Override
    public Optional<Double> getMaxThreshold(Unit unit) throws SynchronizationException {
        return Optional.of(max);
    }

    @Override
    public PhysicalParameter getPhysicalParameter() {
        return PhysicalParameter.FLOW;
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        return false;
    }

    @Override
    public double computeMargin(Network network, Cnec cnec) throws SynchronizationException {
        return 0;
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {

    }

    @Override
    public void desynchronize() {
    }
}
