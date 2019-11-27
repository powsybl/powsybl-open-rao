/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

/**
 * Limits of a flow through an equipment.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class AbsoluteFlowThreshold extends AbstractFlowThreshold {

    public AbsoluteFlowThreshold(Unit unit, Side side, Direction direction, double maxValue) {
        super(unit, side, direction);
        this.maxValue = maxValue;
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) {
        return maxValue < network.getBranch(cnec.getCriticalNetworkElement().getId()).getCurrentLimits(Branch.Side.ONE).getPermanentLimit();
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {

    }
}
