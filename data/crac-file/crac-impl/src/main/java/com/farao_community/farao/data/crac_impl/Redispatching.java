/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

/**
 * Redispatching remedial action
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class Redispatching extends AbstractRangeLever {

    private double minimumPower;
    private double maximumPower;
    private double targetPower;
    private double startupCost;
    private double marginalCost;
    private NetworkElement generator;

    public Redispatching(double minimumPower, double maximumPower, double targetPower, double startupCost, double marginalCost, NetworkElement generator) {
        this.minimumPower = minimumPower;
        this.maximumPower = maximumPower;
        this.targetPower = targetPower;
        this.startupCost = startupCost;
        this.marginalCost = marginalCost;
        this.generator = generator;
    }

    public double getMinimumPower() {
        return minimumPower;
    }

    public void setMinimumPower(double minimumPower) {
        this.minimumPower = minimumPower;
    }

    public double getMaximumPower() {
        return maximumPower;
    }

    public void setMaximumPower(double maximumPower) {
        this.maximumPower = maximumPower;
    }

    public double getTargetPower() {
        return targetPower;
    }

    public void setTargetPower(double targetPower) {
        this.targetPower = targetPower;
    }

    public double getStartupCost() {
        return startupCost;
    }

    public void setStartupCost(double startupCost) {
        this.startupCost = startupCost;
    }

    public double getMarginalCost() {
        return marginalCost;
    }

    public void setMarginalCost(double marginalCost) {
        this.marginalCost = marginalCost;
    }

    public NetworkElement getGenerator() {
        return generator;
    }

    public void setGenerator(NetworkElement generator) {
        this.generator = generator;
    }
}
