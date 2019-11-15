/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_impl.AbstractRemedialAction;
import com.farao_community.farao.data.crac_impl.remedial_action.range_domain.Range;
import com.farao_community.farao.data.crac_impl.remedial_action.usage_rule.AbstractUsageRule;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Group of simultaneously applied range remedial actions.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class RangeAction extends AbstractRemedialAction implements Range, ApplicableRangeAction {

    private List<Range> ranges;
    private List<ApplicableRangeAction> elementaryRangeActions;

    public RangeAction(String id, String name, List<AbstractUsageRule> abstractUsageRules, List<Range> ranges, List<ApplicableRangeAction> elementaryRangeActions) {
        super(id, name, abstractUsageRules);
        this.ranges = ranges;
        this.elementaryRangeActions = elementaryRangeActions;
    }

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public List<ApplicableRangeAction> getElementaryRangeActions() {
        return elementaryRangeActions;
    }

    public void setElementaryRangeActions(List<ApplicableRangeAction> elementaryRangeActions) {
        this.elementaryRangeActions = elementaryRangeActions;
    }

    @Override
    public double getMin(Network network) {
        return 0;
    }

    @Override
    public double getMax(Network network) {
        return 0;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    public void addRange(Range range) {
        this.ranges.add(range);
    }

    public void addElementaryRangeAction(ApplicableRangeAction elementaryRangeAction) {
        this.elementaryRangeActions.add(elementaryRangeAction);
    }
}
