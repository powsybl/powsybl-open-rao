/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.Network;

/**
 * Border devices are a subset of "Branch", "HvdcLine" or "ThreeWindingsTransformer" objects
 * that connect a voltage level of the area to a voltage level that is not part of the area.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public interface BorderDevice {

    /**
     * @return flow leaving the network area throw this border device
     * The flow is oriented as positive if leaving the area and as negative if feeding the area
     */
    double getLeavingFlow(Network network, NetworkArea networkArea);

    String getId();
}
