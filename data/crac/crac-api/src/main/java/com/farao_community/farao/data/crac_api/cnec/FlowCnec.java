package com.farao_community.farao.data.crac_api.cnec;

import com.farao_community.farao.commons.PhysicalParameter;

/**
 * Specific type of {@link BranchCnec} whose monitored {@link PhysicalParameter} is implicitly
 * the flow on the branch.
 */
public interface FlowCnec extends BranchCnec<FlowCnec> {

}
