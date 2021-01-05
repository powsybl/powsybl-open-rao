/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Redispatching remedial action.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("redispatching")
public final class Redispatching extends AbstractElementaryRangeAction {

    public static final int TEMP_VALUE_REDISPATCH = 0;

    private double minimumPower;
    private double maximumPower;
    private double targetPower;
    private double startupCost;
    private double marginalCost;

    public Redispatching(String id, String name, String operator, List<UsageRule> usageRules, List<Range> ranges,
                         double minimumPower, double maximumPower, double targetPower, double startupCost,
                         double marginalCost, NetworkElement generator, String groupId) {
        super(id, name, operator, usageRules, ranges, generator, groupId);
        this.minimumPower = minimumPower;
        this.maximumPower = maximumPower;
        this.targetPower = targetPower;
        this.startupCost = startupCost;
        this.marginalCost = marginalCost;
    }

    public Redispatching(String id, String name, String operator, List<UsageRule> usageRules, List<Range> ranges,
                         double minimumPower, double maximumPower, double targetPower, double startupCost,
                         double marginalCost, NetworkElement networkElement) {
        this(id, name, operator, usageRules, ranges, minimumPower, maximumPower, targetPower, startupCost, marginalCost, networkElement, null);
    }

    public Redispatching(String id, String name, String operator, double anyValue, NetworkElement generator) {
        super(id, name, operator, generator);
        this.minimumPower = anyValue;
        this.maximumPower = anyValue;
        this.targetPower = anyValue;
        this.startupCost = anyValue;
        this.marginalCost = anyValue;
    }

    public Redispatching(String id, NetworkElement generator, double anyValue) {
        super(id, generator);
        this.minimumPower = anyValue;
        this.maximumPower = anyValue;
        this.targetPower = anyValue;
        this.startupCost = anyValue;
        this.marginalCost = anyValue;
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
    protected double getMinValueWithRange(Network network, Range range) {
        // to implement - specific to Redispatching
        return 0;
    }

    @Override
    public double getMaxValueWithRange(Network network, Range range) {
        // to implement - specific to Redispatching
        return 0;
    }

    @Override
    public double getCurrentValue(Network network) {
        return TEMP_VALUE_REDISPATCH;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Redispatching otherRedispatching = (Redispatching) o;

        return super.equals(o)
                && minimumPower == otherRedispatching.minimumPower
                && maximumPower == otherRedispatching.maximumPower
                && targetPower == otherRedispatching.targetPower
                && startupCost == otherRedispatching.startupCost
                && marginalCost == otherRedispatching.marginalCost;
    }

    @Override
    public int hashCode() {
        return String.format("%s%f%f%f%f%f", getId(), marginalCost, maximumPower, minimumPower, targetPower, startupCost).hashCode();
    }
}
