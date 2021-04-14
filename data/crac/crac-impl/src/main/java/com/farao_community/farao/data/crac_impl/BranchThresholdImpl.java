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
@JsonTypeName("branch-threshold")
public class BranchThresholdImpl extends ThresholdImpl implements BranchThreshold {

    /**
     * Side of the network element which is monitored
     */
    @JsonIgnore
    protected Side side;

    /**
     * Direction in which the flow is monitored
     */
    protected final BranchThresholdRule rule;

    @JsonCreator
    public BranchThresholdImpl(@JsonProperty("unit") Unit unit,
                               @JsonProperty("min") Double min,
                               @JsonProperty("max") Double max,
                               @JsonProperty("rule") BranchThresholdRule rule) {
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
}
