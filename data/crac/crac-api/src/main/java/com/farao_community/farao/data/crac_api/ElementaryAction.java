package com.farao_community.farao.data.crac_api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.powsybl.iidm.network.Network;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ElementaryAction extends Identifiable {
    /**
     * Trigger the actions on a given network.
     * @param network The network in which the action is triggered
     */
    void apply(Network network);

    NetworkElement getNetworkElement();
}
