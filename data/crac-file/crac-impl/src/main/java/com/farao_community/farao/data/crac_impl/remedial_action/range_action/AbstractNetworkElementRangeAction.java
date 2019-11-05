/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.range_action;

import com.farao_community.farao.data.crac_impl.NetworkElement;

/**
 * Generic object to define any simple range action on a network element
 * (HVDC line, PST, injection, redispatching, etc.).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractNetworkElementRangeAction implements ApplicableRangeAction {

    protected NetworkElement networkElement;

    public AbstractNetworkElementRangeAction(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    public NetworkElement getNetworkElement() {
        return networkElement;
    }

    public void setNetworkElement(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }
}
