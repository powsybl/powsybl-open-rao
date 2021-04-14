/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range_action;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface PstRangeActionAdder extends RemedialActionAdder<PstRangeActionAdder> {

    PstRangeActionAdder withNetworkElement(String networkElementId);

    PstRangeActionAdder withNetworkElement(String networkElementId, String networkElementName);

    PstRangeActionAdder withGroupId(String groupId);

    TapRangeAdder newPstRange();

    PstRangeAction add();
}
