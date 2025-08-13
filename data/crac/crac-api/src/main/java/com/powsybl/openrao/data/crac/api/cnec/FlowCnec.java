package com.powsybl.openrao.data.crac.api.cnec;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.PhysicalParameter;

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
}
