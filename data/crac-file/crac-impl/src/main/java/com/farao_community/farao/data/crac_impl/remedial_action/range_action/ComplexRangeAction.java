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

import java.util.*;

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
    private Set<ApplicableRangeAction> applicableRangeActions;

    @JsonCreator
    public ComplexRangeAction(@JsonProperty("id") String id, @JsonProperty("name") String name,
                              @JsonProperty("operator") String operator,
                              @JsonProperty("usageRules") List<UsageRule> usageRules,
                              @JsonProperty("ranges") List<Range> ranges,
                              @JsonProperty("applicableRangeActions") Set<ApplicableRangeAction> applicableRangeActions) {
        super(id, name, operator, usageRules);
        this.ranges = ranges;
        this.applicableRangeActions = new HashSet<>(applicableRangeActions);
    }

    public ComplexRangeAction(String id, String operator, List<UsageRule> usageRules, List<Range> ranges, Set<ApplicableRangeAction> applicableRangeActions) {
        this (id, id, operator, usageRules, ranges, applicableRangeActions);
    }

    public ComplexRangeAction(String id, String name, String operator) {
        this (id, name, operator, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
    }

    public ComplexRangeAction(String id, String operator) {
        this (id, id, operator, new ArrayList<>(), new ArrayList<>(), new HashSet<>());
    }

    public List<Range> getRanges() {
        return ranges;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        Set<NetworkElement> set = new HashSet<>();
        applicableRangeActions.forEach(applicableRangeAction -> set.addAll(applicableRangeAction.getNetworkElements()));
        return set;
    }

    @Override
    public double getMinValue(Network network) {
        return ranges.stream().map(range -> range.getMinValue(network)).max(Double::compareTo).orElseThrow(FaraoException::new);
    }

    @Override
    public double getMaxValue(Network network) {
        return ranges.stream().map(range -> range.getMaxValue(network)).min(Double::compareTo).orElseThrow(FaraoException::new);
    }

    @Override
    public void apply(Network network, double setpoint) {
        applicableRangeActions.forEach(applicableRangeAction -> applicableRangeAction.apply(network, setpoint));
    }

    @Override
    public Map<NetworkElement, Double> getCurrentValues(Network network) {
        Map<NetworkElement, Double> values = new HashMap<>();
        applicableRangeActions.forEach(applicableRangeAction -> values.putAll(applicableRangeAction.getCurrentValues(network)));
        return values;
    }

    @JsonProperty("ranges")
    public void addRange(Range range) {
        this.ranges.add(range);
    }

    @JsonProperty("applicableRangeActions")
    public void addApplicableRangeAction(ApplicableRangeAction elementaryRangeAction) {
        this.applicableRangeActions.add(elementaryRangeAction);
    }

    @Override
    public Set<ApplicableRangeAction> getApplicableRangeActions() {
        return applicableRangeActions;
    }
}
