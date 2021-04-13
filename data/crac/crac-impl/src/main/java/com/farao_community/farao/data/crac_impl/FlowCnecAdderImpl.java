/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecAdderImpl extends AbstractCnecAdderImpl<FlowCnecAdder> implements FlowCnecAdder {

    protected Set<BranchThreshold> thresholds = new HashSet<>();

    public FlowCnecAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public BranchThresholdAdder newThreshold() {
        return new BranchThresholdAdderImpl(this);
    }

    void addThreshold(BranchThresholdImpl threshold) {
        thresholds.add(threshold);
    }

    @Override
    protected String getTypeDescription() {
        return "FlowCnec";
    }

    @Override
    public FlowCnec add() {
        checkCnec();
        if (owner.getBranchCnec(id) != null) {
            throw new FaraoException(format("Cannot add a cnec with an already existing ID - %s.", id));
        }
        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold.");
        }
        State state;
        if (instant != Instant.PREVENTIVE) {
            state = owner.addState(owner.getContingency(contingencyId), instant);
        } else {
            state = owner.addPreventiveState();
        }
        FlowCnec cnec = new FlowCnecImpl(id, name, owner.getNetworkElement(networkElementId), operator, state, optimized, monitored, thresholds, reliabilityMargin);
        owner.addFlowCnec(cnec);
        return cnec;
    }
}
