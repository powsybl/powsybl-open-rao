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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Injection range remedial action: choose the optimal value for a load or generator setpoint.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@JsonTypeName("injection-range")
public final class InjectionRange extends AbstractElementaryRangeAction {

    protected static int injectionRangeTempValue = 0;

    @JsonCreator
    public InjectionRange(@JsonProperty("id") String id,
                          @JsonProperty("name") String name,
                          @JsonProperty("operator") String operator,
                          @JsonProperty("usageRules") List<UsageRule> usageRules,
                          @JsonProperty("ranges") List<Range> ranges,
                          @JsonProperty("networkElement") NetworkElement networkElement) {
        super(id, name, operator, usageRules, ranges, networkElement);
    }

    @Override
    protected double getMinValueWithRange(Network network, Range range) {
        // to implement - specific to InjectionRange
        return injectionRangeTempValue;
    }

    @Override
    public double getMaxValueWithRange(Network network, Range range) {
        // to implement - specific to InjectionRange
        return injectionRangeTempValue;
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }

}
