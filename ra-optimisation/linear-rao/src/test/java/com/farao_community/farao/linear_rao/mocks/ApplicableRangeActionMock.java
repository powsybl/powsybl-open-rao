package com.farao_community.farao.linear_rao.mocks;

import com.farao_community.farao.data.crac_api.ApplicableRangeAction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ApplicableRangeActionMock implements ApplicableRangeAction {
    private NetworkElement networkElement;

    public ApplicableRangeActionMock(NetworkElement networkElement) {
        this.networkElement = networkElement;
    }

    @Override
    public void apply(Network network, double setpoint) {

    }

    @Override
    public Set<NetworkElement> getNetworkElements() {
        return Collections.singleton(networkElement);
    }
}
