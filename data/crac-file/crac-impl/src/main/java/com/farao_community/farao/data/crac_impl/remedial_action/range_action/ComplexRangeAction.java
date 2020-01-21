/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group of simultaneously applied range remedial actions.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class ComplexRangeAction extends AbstractRemedialAction implements RangeAction {

    @JsonProperty("ranges")
    private List<Range> ranges;

    @JsonProperty("applicableRangeActions")
    private List<ApplicableRangeAction> applicableRangeActions;

    @JsonCreator
    public ComplexRangeAction(@JsonProperty("id") String id, @JsonProperty("name") String name,
                              @JsonProperty("operator") String operator,
                              @JsonProperty("usageRules") List<UsageRule> usageRules,
                              @JsonProperty("ranges") List<Range> ranges,
                              @JsonProperty("applicableRangeActions") List<ApplicableRangeAction> applicableRangeActions) {
        super(id, name, operator, usageRules);
        this.ranges = ranges;
        this.applicableRangeActions = applicableRangeActions;
    }

    public ComplexRangeAction(String id, String operator, List<UsageRule> usageRules, List<Range> ranges, List<ApplicableRangeAction> applicableRangeActions) {
        this (id, id, operator, usageRules, ranges, applicableRangeActions);
    }

    public ComplexRangeAction(String id, String name, String operator) {
        this (id, name, operator, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public ComplexRangeAction(String id, String operator) {
        this (id, id, operator, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> set = new HashSet<>();
        applicableRangeActions.forEach(applicableRangeAction -> set.addAll(applicableRangeAction.getNetworkElements()));
        return set;
    }

    public void setApplicableRangeActions(List<ApplicableRangeAction> applicableRangeActions) {
        this.applicableRangeActions = applicableRangeActions;
    }

    @Override
    public double getMinValue(Network network) {
        double minValue = ranges.iterator().next().getMinValue(network);
        for (Range range : ranges) {
            minValue = Math.max(range.getMinValue(network), minValue);
        }
        return minValue;
    }

    @Override
    public double getMaxValue(Network network) {
        double maxValue = ranges.iterator().next().getMaxValue(network);
        for (Range range : ranges) {
            maxValue = Math.min(range.getMaxValue(network), maxValue);
        }
        return maxValue;
    }

    @Override
    public void apply(Network network, double setpoint) {
        if (getMinValue(network) <= setpoint && setpoint <= getMaxValue(network)) {
            applicableRangeActions.forEach(applicableRangeAction -> applicableRangeAction.apply(network, setpoint));
        } else {
            throw new FaraoException("Impossible to apply ComplexRangeAction " + getId() + " because setpoint value is out of boundaries");
        }
    }

    @JsonProperty("ranges")
    public void addRange(Range range) {
        this.ranges.add(range);
    }

    @JsonProperty("applicableRangeActions")
    public void addApplicableRangeAction(ApplicableRangeAction elementaryRangeAction) {
        this.applicableRangeActions.add(elementaryRangeAction);
    }
}
