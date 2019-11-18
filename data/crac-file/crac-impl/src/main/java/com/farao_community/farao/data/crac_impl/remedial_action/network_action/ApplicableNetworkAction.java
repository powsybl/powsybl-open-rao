/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.powsybl.iidm.network.Network;

/**
 * Generic object to implement a simple network action (setpoint, topology, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public interface ApplicableNetworkAction {

    void apply(Network network);
}
