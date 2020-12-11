/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.cnec.adder;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.cnec.adder.BranchCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.adder.BranchThresholdAdder;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.threshold.BranchThresholdImpl;
import com.farao_community.farao.data.crac_impl.threshold.BranchThresholdAdderImpl;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public abstract class AbstractBranchCnecAdder extends AbstractCnecAdderImpl<BranchCnec, BranchCnecAdder> implements BranchCnecAdder {

    protected Set<BranchThreshold> thresholds = new HashSet<>();

    protected AbstractBranchCnecAdder(SimpleCrac parent) {
        super(parent);
    }

    public void addThreshold(BranchThresholdImpl threshold) {
        thresholds.add(threshold);
    }

    @Override
    protected void checkCnec() {
        super.checkCnec();
        if (parent.getBranchCnec(id) != null) {
            throw new FaraoException(format("Cannot add a cnec with an already existing ID - %s.", id));
        }
        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold.");
        }
    }

    @Override
    public BranchThresholdAdder newThreshold() {
        return new BranchThresholdAdderImpl(this);
    }

    @Override
    public BranchCnecAdder addThreshold(BranchThreshold threshold) {
        thresholds.add(threshold);
        return this;
    }
}
