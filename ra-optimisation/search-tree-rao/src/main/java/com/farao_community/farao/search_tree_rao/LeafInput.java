package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.linear_optimisation.IteratingLinearOptimizer;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunction;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.farao_community.farao.commons.Unit.MEGAWATT;

public class LeafInput {
    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<NetworkAction> preAppliedNetworkActions;
    private NetworkAction networkActionToApply;
    private Set<NetworkAction> allNetworkActions;
    private Set<RangeAction> rangeActions;
    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    private ObjectiveFunction objectiveFunction;
    private IteratingLinearOptimizer iteratingLinearOptimizer;

    private BranchResult initialBranchResult;
    private BranchResult prePerimeterBranchResult;
    private Map<RangeAction, Double> prePerimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    private Map<BranchCnec, Double> commercialFlows;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    public LeafInput(SearchTreeInput searchTreeInput, Network network, Set<NetworkAction> preAppliedNetworkActions, NetworkAction networkActionToApply, ObjectiveFunction objectiveFunction, IteratingLinearOptimizer iteratingLinearOptimizer, boolean isRaoWithLoopflowLimitation) {
        this.network = network;
        this.cnecs = searchTreeInput.getCnecs();
        this.preAppliedNetworkActions = preAppliedNetworkActions;
        this.networkActionToApply = networkActionToApply;
        this.allNetworkActions = searchTreeInput.getNetworkActions();
        this.rangeActions = searchTreeInput.getRangeActions();
        this.loopflowCnecs = searchTreeInput.getLoopflowCnecs();
        this.glskProvider = searchTreeInput.getGlskProvider();
        this.referenceProgram = searchTreeInput.getReferenceProgram();

        this.objectiveFunction = objectiveFunction;
        this.iteratingLinearOptimizer = iteratingLinearOptimizer;

        this.initialBranchResult = searchTreeInput.getInitialBranchResult();
        this.prePerimeterBranchResult = searchTreeInput.getPrePerimeterBranchResult();

        if (isRaoWithLoopflowLimitation) {
            Map<BranchCnec, Double> prePerimeterCommercialFlows = new HashMap<>();
            loopflowCnecs.forEach(cnec -> prePerimeterCommercialFlows.put(cnec, searchTreeInput.getPrePerimeterBranchResult().getCommercialFlow(cnec, MEGAWATT)));
            this.commercialFlows = prePerimeterCommercialFlows;
        }

        this.prePerimeterSetpoints = searchTreeInput.getPrePerimeterSetpoints();
        if (preAppliedNetworkActions.isEmpty() && Objects.isNull(networkActionToApply)) {
            this.sensitivityAndLoopflowResults = searchTreeInput.getPrePerimeterSensitivityAndLoopflowResults();
        } else {
            this.sensitivityAndLoopflowResults = null;
        }
    }

    public Network getNetwork() {
        return network;
    }

    public Set<BranchCnec> getCnecs() {
        return cnecs;
    }

    public Set<NetworkAction> getPreAppliedNetworkActions() {
        return preAppliedNetworkActions;
    }

    public NetworkAction getNetworkActionToApply() {
        return networkActionToApply;
    }

    public Set<NetworkAction> getAllNetworkActions() {
        return allNetworkActions;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    public ZonalData<LinearGlsk> getGlskProvider() {
        return glskProvider;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public ObjectiveFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    public IteratingLinearOptimizer getIteratingLinearOptimizer() {
        return iteratingLinearOptimizer;
    }

    public BranchResult getInitialBranchResult() {
        return initialBranchResult;
    }

    public BranchResult getPrePerimeterBranchResult() {
        return prePerimeterBranchResult;
    }

    public boolean hasSensitivityAndLoopflowResults() {
        return !Objects.isNull(sensitivityAndLoopflowResults);
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }

    public Map<BranchCnec, Double> getCommercialFlows() {
        return commercialFlows;
    }

    public Map<RangeAction, Double> getPrePerimeterSetpoints() {
        return prePerimeterSetpoints;
    }

    public void setCommercialFlows(Map<BranchCnec, Double> commercialFlows) {
        this.commercialFlows = commercialFlows;
    }

    public void setSensitivityAndLoopflowResults(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
    }
}
