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
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * Generic object to define any simple range action on a network element
 * (HVDC line, PST, injection, redispatching, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PstRange.class, name = "pst-range"),
        @JsonSubTypes.Type(value = HvdcRange.class, name = "hvdc-range"),
        @JsonSubTypes.Type(value = InjectionRange.class, name = "injection-range"),
        @JsonSubTypes.Type(value = Redispatching.class, name = "redispatching")
    })
public abstract class AbstractElementaryRangeAction extends AbstractRemedialAction implements RangeAction {
    protected List<Range> ranges;
    protected NetworkElement networkElement;

    @JsonCreator
    public AbstractElementaryRangeAction(@JsonProperty("id") String id,
                                         @JsonProperty("name") String name,
                                         @JsonProperty("operator") String operator,
                                         @JsonProperty("usageRules") List<UsageRule> usageRules,
                                         @JsonProperty("ranges") List<Range> ranges,
                                         @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules);
        this.ranges = new ArrayList<>(ranges);
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, String name, String operator, NetworkElement networkElement) {
        super(id, name, operator);
        this.ranges = new ArrayList<>();
        this.networkElement = networkElement;
    }

    public AbstractElementaryRangeAction(String id, NetworkElement networkElement) {
        super(id);
        this.ranges = new ArrayList<>();
        this.networkElement = networkElement;
    }

    public final List<Range> getRanges() {
        return ranges;
    }

    public void addRange(Range range) {
        this.ranges.add(range);
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    protected abstract double getMinValueWithRange(Network network, Range range);

    @Override
    public double getMinValue(Network network) {
        double minValue = Double.NEGATIVE_INFINITY;
        for (Range range: ranges
             ) {
            minValue = Math.max(getMinValueWithRange(network, range), minValue);
        }
        return minValue;
    }

    protected abstract double getMaxValueWithRange(Network network, Range range);

    @Override
    public double getMaxValue(Network network) {
        double maxValue = Double.POSITIVE_INFINITY;
        for (Range range: ranges
        ) {
            maxValue = Math.min(getMaxValueWithRange(network, range), maxValue);
        }
        return maxValue;
    }

    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
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
                && new HashSet<>(ranges).equals(new HashSet<>(otherAbstractElementaryRangeAction.getRanges()))
                && networkElement.equals(otherAbstractElementaryRangeAction.getNetworkElement());
    }

    @Override
    public int hashCode() {
        return String.format("%s%s%d", getId(), networkElement.getId(), ranges.size()).hashCode();
    }
}
