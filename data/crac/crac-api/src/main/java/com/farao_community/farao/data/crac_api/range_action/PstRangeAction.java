/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.TapConvention;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface PstRangeAction extends RangeAction {

    NetworkElement getNetworkElement();

    List<TapRange> getRanges();

    int getCurrentTapPosition(Network network, TapConvention rangeDefinition);

    double convertTapToAngle(int tap);

    int convertAngleToTap(double finalAngle);
}
