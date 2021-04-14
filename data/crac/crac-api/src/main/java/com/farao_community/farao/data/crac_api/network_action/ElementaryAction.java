package com.farao_community.farao.data.crac_api.network_action;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.Network;

/**
 * Generic interface for the definition of elementary actions
 *
 * An elementary action is an action on the network which can be
 * activated by a {@link NetworkAction}
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public interface ElementaryAction {

    /**
     * Apply the elementary action on a given network.
     */
    void apply(Network network);

    /**
     * Get the Network Element associated to the elementary action
     */
    NetworkElement getNetworkElement();
}
