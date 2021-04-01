package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.Set;

public class LinearOptimizerInput {
    Set<BranchCnec> loopflowCnecs;
    Set<BranchCnec> cnecs;
    Set<RangeAction> rangeActions;
    Network network;
    Map<RangeAction, Double> preperimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    BranchCnec mostLimitingElement;

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    public Set<BranchCnec> getCnecs() {
        return cnecs;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Network getNetwork() {
        return network;
    }

    public Map<RangeAction, Double> getPreperimeterSetpoints() {
        return preperimeterSetpoints;
    }

    public BranchCnec getMostLimitingElement() {
        return mostLimitingElement;
    }
}
