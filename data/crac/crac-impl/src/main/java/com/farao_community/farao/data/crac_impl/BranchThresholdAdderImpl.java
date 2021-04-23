/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchThresholdAdderImpl extends ThresholdAdderImpl<BranchThresholdAdder> implements BranchThresholdAdder {

    private final FlowCnecAdderImpl ownerAdder;
    private BranchThresholdRule rule;

    public BranchThresholdAdderImpl(FlowCnecAdder ownerAdder) {
        Objects.requireNonNull(ownerAdder);
        this.ownerAdder = (FlowCnecAdderImpl) ownerAdder;
    }

    @Override
    public BranchThresholdAdder withRule(BranchThresholdRule rule) {
        this.rule = rule;
        return this;
    }

    @Override
    public FlowCnecAdder add() {
        super.checkThreshold();
        AdderUtils.assertAttributeNotNull(this.rule, "BranchThreshold", "Rule", "withRule()");

        ownerAdder.addThreshold(new BranchThresholdImpl(unit, min, max, rule));
        return ownerAdder;
    }
}
