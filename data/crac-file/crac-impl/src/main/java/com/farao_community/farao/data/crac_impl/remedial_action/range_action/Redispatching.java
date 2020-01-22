/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Redispatching remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class Redispatching extends AbstractNetworkElementRangeAction {

    private double minimumPower;
    private double maximumPower;
    private double targetPower;
    private double startupCost;
    private double marginalCost;

    @JsonCreator
    public Redispatching(@JsonProperty("id") String id,
                         @JsonProperty("name") String name,
                         @JsonProperty("operator") String operator,
                         @JsonProperty("usageRules") List<UsageRule> usageRules,
                         @JsonProperty("ranges") List<AbstractRange> ranges,
                         @JsonProperty("minimumPower") double minimumPower,
                         @JsonProperty("maximumPower") double maximumPower,
                         @JsonProperty("targetPower") double targetPower,
                         @JsonProperty("startupCost") double startupCost,
                         @JsonProperty("marginalCost") double marginalCost,
                         @JsonProperty("generator") NetworkElement generator) {
        super(id, name, operator, usageRules, ranges, generator);
        this.minimumPower = minimumPower;
        this.maximumPower = maximumPower;
        this.targetPower = targetPower;
        this.startupCost = startupCost;
        this.marginalCost = marginalCost;
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

    @Override
    protected double getMinValueWithRange(Network network, AbstractRange range) {
        // to implement - specific to Redispatching
        return 0;
    }

    @Override
    public double getMinValue(Network network) {
        return 0;
    }

    @Override
    public double getMaxValueWithRange(Network network, AbstractRange range) {
        // to implement - specific to Redispatching
        return 0;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return new HashSet<>();
    }
}
