package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;
import com.powsybl.iidm.network.Network;

/**
 * Specific type of {@link BranchCnec} whose monitored {@link PhysicalParameter} is implicitly
 * the flow on the branch.
 */
public interface FlowCnec extends BranchCnec<FlowCnec> {

    /**
     * Getter of the Imax on each {@link Side} of the {@code FlowCnec}.
     *
     * @param side: The {@link Side} on which the Imax is queried.
     * @return The value of the iMax, given in Unit.AMPERE.
     */
    Double getIMax(Side side);

    /**
     * Says if a FlowCnec's NetworkElement is connected in a Network
     *
     * @param network: the network to look into
     * @return true if the CNEC is connected
     */
    boolean isConnected(Network network);

}
