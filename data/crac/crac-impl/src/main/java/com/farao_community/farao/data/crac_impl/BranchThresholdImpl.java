/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

/**
 * Limits of a flow (in MEGAWATT or AMPERE) through a branch.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class BranchThresholdImpl extends ThresholdImpl implements BranchThreshold {

    /**
     * Side of the network element which is monitored
     */
    protected Side side;

    /**
     * Direction in which the flow is monitored
     */
    protected final BranchThresholdRule rule;

    BranchThresholdImpl(Unit unit, Double min, Double max, BranchThresholdRule rule) {
        super(unit, min, max);
        this.rule = rule;
    }

    @Override
    public Side getSide() {
        Objects.requireNonNull(side, "Side has not been defined.");
        return side;
    }

    void setSide(Side side) {
        this.side = side;
    }

    @Override
    public BranchThresholdRule getRule() {
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BranchThresholdImpl otherT = (BranchThresholdImpl) o;

        return super.equals(otherT)
                && rule.equals(otherT.getRule());
    }

    @Override
    public int hashCode() {
        return super.hashCode()
                + 31 * rule.hashCode();
    }
}
