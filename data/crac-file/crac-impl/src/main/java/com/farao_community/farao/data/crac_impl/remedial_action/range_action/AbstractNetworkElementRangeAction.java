/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Generic object to define any simple range action on a network element
 * (HVDC line, PST, injection, redispatching, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractNetworkElementRangeAction extends AbstractRemedialAction implements RangeAction {

    protected List<AbstractRange> ranges;

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    protected NetworkElement networkElement;

    @JsonCreator
    public AbstractNetworkElementRangeAction(@JsonProperty("id") String id,
                                             @JsonProperty("name") String name,
                                             @JsonProperty("operator") String operator,
                                             @JsonProperty("usageRules") List<UsageRule> usageRules,
                                             @JsonProperty("ranges") List<AbstractRange> ranges,
                                             @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules);
        this.ranges = ranges;
        this.networkElement = networkElement;
    }

    public AbstractNetworkElementRangeAction(@JsonProperty("id") String id,
                                             @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id);
        this.networkElement = networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    protected abstract double getMinValueWithRange(Network network, AbstractRange range);

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.POSITIVE_INFINITY;
        for (AbstractRange range: ranges
             ) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    protected abstract double getMaxValueWithRange(Network network, AbstractRange range);

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.NEGATIVE_INFINITY;
        for (AbstractRange range: ranges
        ) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }

    @JsonProperty("ranges")
    public void addRange(AbstractRange range) {
        this.ranges.add(range);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractNetworkElementRangeAction otherAbstractNetworkElementRangeAction = (AbstractNetworkElementRangeAction) o;

        return super.equals(o)
                && networkElement == otherAbstractNetworkElementRangeAction.getNetworkElement()
                && ranges == otherAbstractNetworkElementRangeAction.ranges;
    }

    @Override
    public int hashCode() {
        return String.format("%s%s%d", getId(), networkElement.getId(), ranges.size()).hashCode();
    }
}
