/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.range.StandardRange;
import com.farao_community.farao.data.crac_api.range_action.InjectionShiftRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InjectionShiftRangeActionImpl extends AbstractRangeAction<InjectionShiftRangeAction> implements InjectionShiftRangeAction {

    private final Map<NetworkElement, Double> injectionShiftKeys;
    private final List<StandardRange> ranges;

    InjectionShiftRangeActionImpl(String id, String name, String operator, String groupId, List<UsageRule> usageRules,
                                  List<StandardRange> ranges, Map<NetworkElement, Double> injectionShiftKeys) {
        super(id, name, operator, usageRules, groupId);
        this.ranges = ranges;
        this.injectionShiftKeys = injectionShiftKeys;
    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return injectionShiftKeys.keySet();
    }

    @Override
    public double getMinAdmissibleSetpoint(double previousInstantSetPoint) {
        return ranges.stream().mapToDouble(StandardRange::getMin).max().orElseThrow();
    }

    @Override
    public double getMaxAdmissibleSetpoint(double previousInstantSetPoint) {
        return ranges.stream().mapToDouble(StandardRange::getMax).min().orElseThrow();
    }

    @Override
    public void apply(Network network, double targetSetpoint) {
        // todo
    }

    @Override
    public double getCurrentSetpoint(Network network) {
        // todo
        return 0.0;
    }

    @Override
    public Map<NetworkElement, Double> getInjectionShiftKeys() {
        return injectionShiftKeys;
    }

    @Override
    public List<StandardRange> getRanges() {
        return ranges;
    }
}
