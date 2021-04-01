package com.farao_community.farao.data.crac_api;

import com.powsybl.iidm.network.Network;

public interface ElementaryAction extends Identifiable {
    /**
     * Trigger the actions on a given network.
     * @param network The network in which the action is triggered
     */
    void apply(Network network);

    NetworkElement getNetworkElement();
}
