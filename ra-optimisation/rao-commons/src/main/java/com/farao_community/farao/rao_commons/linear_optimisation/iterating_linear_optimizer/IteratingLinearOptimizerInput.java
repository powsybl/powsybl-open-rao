package com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer;

import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_result_extensions.CnecResult;
import com.farao_community.farao.rao_commons.linear_optimisation.LinearOptimizerInput;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class IteratingLinearOptimizerInput {
    private Set<BranchCnec> loopflowCnecs;
    private Set<BranchCnec> cnecs;
    private Set<RangeAction> rangeActions;
    private Network network;
    private Map<RangeAction, Double> preperimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    private Map<BranchCnec, CnecResult> initialCnecResults;
    private Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW;

    // TODO : replace with builder
    public IteratingLinearOptimizerInput(Set<BranchCnec> loopflowCnecs, Set<BranchCnec> cnecs, Set<RangeAction> rangeActions, Network network, Map<RangeAction, Double> preperimeterSetpoints, Map<BranchCnec, CnecResult> initialCnecResults, Map<BranchCnec, Double> prePerimeterCnecMarginsInAbsoluteMW) {
        this.loopflowCnecs = loopflowCnecs;
        this.cnecs = cnecs;
        this.rangeActions = rangeActions;
        this.network = network;
        this.preperimeterSetpoints = preperimeterSetpoints;
        this.initialCnecResults = initialCnecResults;
        this.prePerimeterCnecMarginsInAbsoluteMW = prePerimeterCnecMarginsInAbsoluteMW;
    }

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

    public Map<BranchCnec, CnecResult> getInitialCnecResults() {
        return initialCnecResults;
    }

    public Map<BranchCnec, Double> getPrePerimeterCnecMarginsInAbsoluteMW() {
        return prePerimeterCnecMarginsInAbsoluteMW;
    }
}
