/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * NetworkArea is defined as a list of participating voltage levels and a list of border devices.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public interface NetworkArea {

    /**
     * @return a list of voltage level
     */
    List<VoltageLevel> getAreaVoltageLevels(Network network);

    /**
     * @return Border devices are a subset of <code>Branch</code>, <code>HvdcLine</code>
     * or <code>ThreeWindingsTransformer</code> objects
     * that connect a voltage level of the area to a voltage level that is not part of the area.
     */
    List<BorderDevice> getBorderDevices(Network network);

    /**
     * @return Sum of the flows leaving the area
     */
    double getNetPosition(Network network);

    String getName();

}
