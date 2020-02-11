/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.UsageRule;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Elementary HVDC range remedial action: choose the optimal value for an HVDC line setpoint.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public final class HvdcRange extends AbstractElementaryRangeAction {

    protected static int hvdcRangeTempValue = 0;

    @JsonCreator
    public HvdcRange(@JsonProperty("id") String id,
                     @JsonProperty("name") String name,
                     @JsonProperty("operator") String operator,
                     @JsonProperty("usageRules") List<UsageRule> usageRules,
                     @JsonProperty("ranges") List<Range> ranges,
                     @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges, networkElement);
    }

    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        // to implement - specific to HvdcRange
        return hvdcRangeTempValue;
    }

    @Override
    public double getMaxValueWithRange(Network network, Range range) {
        // to implement - specific to HvdcRange
        return hvdcRangeTempValue;
    }

    @Override
    public double getMaxNegativeVariation(Network network) {
        return Math.abs(getMinValue(network) - getCurrentSetpoint(network));
    }

    @Override
    public double getMaxPositiveVariation(Network network) {
        return Math.abs(getMaxValue(network) - getCurrentSetpoint(network));
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

    public double getCurrentSetpoint(Network network) {
        HvdcLine hvdcLine = network.getHvdcLine(networkElement.getId());
        return hvdcLine.getActivePowerSetpoint();
    }
}
