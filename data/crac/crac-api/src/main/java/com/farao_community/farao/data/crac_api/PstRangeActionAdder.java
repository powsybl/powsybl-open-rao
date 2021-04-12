/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_api;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface PstRangeActionAdder extends RemedialActionAdder<PstRangeActionAdder> {

    PstRangeActionAdder withNetworkElement(String networkElementId);

    PstRangeActionAdder withNetworkElement(String networkElementId, String networkElementName);

    /**
     * Set the group ID if the PST is part of a group
     * @param groupId: ID of the group
     * @return the {@code PstRangeActionAdder} instance
     */
    PstRangeActionAdder withGroupId(String groupId);

    TapRangeAdder newPstRange();

    /**
     * Add the new PST Range Action to the Crac
     * @return the {@code Crac} instance
     */
    PstRangeAction add();
}
