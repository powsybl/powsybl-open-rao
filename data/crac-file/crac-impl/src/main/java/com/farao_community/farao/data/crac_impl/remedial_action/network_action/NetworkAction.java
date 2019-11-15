/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.remedial_action.network_action;

import com.powsybl.iidm.network.Network;
import java.util.List;

/**
 * Group of simple elementary remedial actions (setpoint, open/close, ...).
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class NetworkAction implements ApplicableNetworkAction {

    private List<ApplicableNetworkAction> networkActions;

    public NetworkAction(List<ApplicableNetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public void setNetworkActions(List<ApplicableNetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public List<ApplicableNetworkAction> getNetworkActions() {
        return networkActions;
    }

    @Override
    public void apply(Network network) {
        throw new UnsupportedOperationException();
    }

    public void addNetworkAction(ApplicableNetworkAction networkAction) {
        this.networkActions.add(networkAction);
    }
}
