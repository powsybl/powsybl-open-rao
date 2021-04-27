package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;

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

}
