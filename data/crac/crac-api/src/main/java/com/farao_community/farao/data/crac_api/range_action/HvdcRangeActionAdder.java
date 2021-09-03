/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api.range_action;

import com.farao_community.farao.data.crac_api.RemedialActionAdder;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface HvdcRangeActionAdder extends RemedialActionAdder<HvdcRangeActionAdder> {

    HvdcRangeActionAdder withNetworkElement(String networkElementId);

    HvdcRangeActionAdder withNetworkElement(String networkElementId, String networkElementName);

    HvdcRangeActionAdder withGroupId(String groupId);

    HvdcRangeAdder newHvdcRange();

    HvdcRangeAction add();
}
