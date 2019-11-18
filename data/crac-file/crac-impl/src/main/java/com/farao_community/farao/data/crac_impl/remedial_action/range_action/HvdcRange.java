/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_impl.NetworkElement;
import com.powsybl.iidm.network.Network;

/**
 * Elementary HVDC range remedial action: choose the optimal value for an HVDC line setpoint.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class HvdcRange extends AbstractNetworkElementRangeAction {

    public HvdcRange(NetworkElement networkElement) {
        super(networkElement);
    }

    @Override
    public void apply(Network network, double setpoint) {
        throw new UnsupportedOperationException();
    }
}
