package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Specific type of {@link BranchCnec} whose monitored {@link PhysicalParameter} is implicitly
 * the flow on the branch.
 */
public interface FlowCnec extends BranchCnec<FlowCnec> {

    /**
     * Getter of the Imax on each {@link TwoSides} of the {@code FlowCnec}.
     *
     * @param side: The {@link TwoSides} on which the Imax is queried.
     * @return The value of the iMax, given in Unit.AMPERE.
     */
    Optional<Double> getIMax(TwoSides side);

    /**
     * Says if a FlowCnec's NetworkElement is connected in a Network
     *
     * @param network: the network to look into
     * @return true if the CNEC is connected
     */
    boolean isConnected(Network network);

}
