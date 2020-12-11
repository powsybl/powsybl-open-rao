/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.adder.BranchCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.adder.BranchThresholdAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;

import java.util.Objects;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class BranchThresholdAdderImpl extends ThresholdAdderImpl<BranchThresholdAdder> implements BranchThresholdAdder {

    private final BranchCnecAdder parent;
    private BranchThresholdRule rule;

    public BranchThresholdAdderImpl(BranchCnecAdder parent) {
        Objects.requireNonNull(parent);
        this.parent = parent;
    }

    @Override
    public BranchThresholdAdder setRule(BranchThresholdRule rule) {
        this.rule = rule;
        return this;
    }

    @Override
    public BranchCnecAdder add() {
        super.checkThreshold();
        if (rule == null) {
            throw new FaraoException("Cannot add a threshold without a rule. Please use setRule.");
        }
        parent.addThreshold(new BranchThresholdImpl(unit, min, max, rule));
        return parent;
    }
}
