/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.ApplicableRangeAction;
import com.farao_community.farao.data.crac_api.Range;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Group of simultaneously applied range remedial actions.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class ComplexRangeAction extends AbstractRemedialAction implements ApplicableRangeAction, RangeAction {

    @JsonProperty("ranges")
    private List<Range> ranges;

    @JsonProperty("applicableRangeActions")
    private List<ApplicableRangeAction> applicableRangeActions;

    @JsonCreator
    public ComplexRangeAction(@JsonProperty("id") String id, @JsonProperty("name") String name,
                              @JsonProperty("usageRules") List<UsageRule> usageRules,
                              @JsonProperty("ranges") List<Range> ranges,
                              @JsonProperty("applicableRangeActions") List<ApplicableRangeAction> applicableRangeActions) {
        super(id, name, usageRules);
        this.ranges = ranges;
        this.applicableRangeActions = applicableRangeActions;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public List<ApplicableRangeAction> getApplicableRangeActions() {
        return applicableRangeActions;
    }

    public void setApplicableRangeActions(List<ApplicableRangeAction> applicableRangeActions) {
        this.applicableRangeActions = applicableRangeActions;
    }

    @Override
    public double getMinValue(Network network) {
        return 0;
    }

    @Override
    public double getMaxValue(Network network) {
        return 0;
    }

    @Override
    public void apply(Network network, double setpoint) {
        applicableRangeActions.forEach(applicableRangeAction -> applicableRangeAction.apply(network, setpoint));
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
