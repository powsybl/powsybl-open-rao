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
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * Generic object to define any simple range action on a network element
 * (HVDC line, PST, injection, redispatching, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractElementaryRangeAction extends AbstractRangeAction {
    protected NetworkElement networkElement;
    protected String groupId;

    public AbstractElementaryRangeAction(String id, String name, String operator, List<UsageRule> usageRules,
                                         List<Range> ranges, NetworkElement networkElement, String groupId) {
        super(id, name, operator, usageRules, ranges);
        this.networkElement = networkElement;
        this.groupId = groupId;
    }

    public AbstractElementaryRangeAction(String id, String name, String operator, List<UsageRule> usageRules,
                                         List<Range> ranges, NetworkElement networkElement) {
        this(id, name, operator, usageRules, ranges, networkElement, null);
    }

    public AbstractElementaryRangeAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    @Override
    public void synchronize(Network network) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void desynchronize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSynchronized() {
        throw new UnsupportedOperationException();
    }

    protected abstract double getMinValueWithRange(Network network, Range range);

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.NEGATIVE_INFINITY;
        for (Range range: ranges) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    protected abstract double getMaxValueWithRange(Network network, Range range);

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.POSITIVE_INFINITY;
        for (Range range: ranges) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @Override
    public Optional<String> getGroupId() {
        return Optional.ofNullable(groupId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractElementaryRangeAction otherAbstractElementaryRangeAction = (AbstractElementaryRangeAction) o;

        return super.equals(o)
                && networkElement.equals(otherAbstractElementaryRangeAction.getNetworkElement());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + networkElement.hashCode();
        return result;
    }
}
