/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.range_domain.AbstractRange;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Set;

/**
 * Range remedial action with several {@link NetworkElement}s sharing common characteristics:
 * id, name, operator, {@link UsageRule}s and {@link AbstractRange}s.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class AlignedRangeAction extends AbstractRemedialAction implements RangeAction {

    public static final double TEMP_MIN_VALUE = 0;
    public static final double TEMP_MAX_VALUE = 0;

    @JsonProperty("ranges")
    private List<AbstractRange> ranges;

    @JsonProperty("networkElements")
    private Set<NetworkElement> networkElements;

    @JsonCreator
    public AlignedRangeAction(@JsonProperty("id") String id,
                              @JsonProperty("name") String name,
                              @JsonProperty("operator") String operator,
                              @JsonProperty("usageRules") List<UsageRule> usageRules,
                              @JsonProperty("ranges") List<AbstractRange> ranges,
                              @JsonProperty("applicableRangeActions") Set<NetworkElement> networkElements) {
        super(id, name, operator, usageRules);
        this.ranges = ranges;
        this.networkElements = networkElements;
    }

    public List<AbstractRange> getRanges() {
        return ranges;
    }

    @Override
    public double getMinValue(Network network) {
        return TEMP_MIN_VALUE;
    }

    @Override
    public double getMaxValue(Network network) {
        return TEMP_MAX_VALUE;
    }

    @Override
    public void apply(Network network, double setpoint) {
        // to implement
    }

    public void synchronize(Network network) {
        // to implement?
    }

    @JsonProperty("ranges")
    public void addRange(AbstractRange range) {
        this.ranges.add(range);
    }

    @JsonProperty("networkElement")
    public void addNetworkElement(NetworkElement networkElement) {
        this.networkElements.add(networkElement);
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return networkElements;
    }
}
