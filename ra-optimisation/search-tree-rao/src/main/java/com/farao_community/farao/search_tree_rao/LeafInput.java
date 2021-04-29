package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
import com.farao_community.farao.rao_commons.objective_function_evaluator.ObjectiveFunctionEvaluator;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LeafInput {
    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<NetworkAction> appliedNetworkActions;
    private NetworkAction networkActionToApply;
    private Set<NetworkAction> allNetworkActions;
    private Set<RangeAction> rangeActions;
    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    private ObjectiveFunctionEvaluator objectiveFunctionEvaluator;

    private CnecResults initialCnecResults;
    private Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW;
    private Map<RangeAction, Double> prePerimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    private Map<BranchCnec, Double> commercialFlows;
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;

    public LeafInput(SearchTreeInput searchTreeInput, Set<NetworkAction> appliedNetworkActions, NetworkAction networkActionToApply, ObjectiveFunctionEvaluator objectiveFunctionEvaluator) {
        this.network = searchTreeInput.getNetwork();
        this.cnecs = searchTreeInput.getCnecs();
        this.appliedNetworkActions = appliedNetworkActions;
        this.networkActionToApply = networkActionToApply;
        this.allNetworkActions = searchTreeInput.getNetworkActions();
        this.rangeActions = searchTreeInput.getRangeActions();
        this.loopflowCnecs = searchTreeInput.getLoopflowCnecs();
        this.glskProvider = searchTreeInput.getGlskProvider();
        this.referenceProgram = searchTreeInput.getReferenceProgram();

        this.objectiveFunctionEvaluator = objectiveFunctionEvaluator;

        this.initialCnecResults = searchTreeInput.getInitialCnecResults();
        this.prePerimeterMarginsInAbsoluteMW = searchTreeInput.getPrePerimeterMarginsInAbsoluteMW();
        this.prePerimeterSetpoints = searchTreeInput.getPrePerimeterSetpoints();
        this.commercialFlows = searchTreeInput.getPrePerimeterCommercialFlows();
        if(appliedNetworkActions.isEmpty() && Objects.isNull(networkActionToApply)) {
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

    public Set<NetworkAction> getAppliedNetworkActions() {
        return appliedNetworkActions;
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

    public ObjectiveFunctionEvaluator getObjectiveFunctionEvaluator() {
        return objectiveFunctionEvaluator;
    }

    public CnecResults getInitialCnecResults() {
        return initialCnecResults;
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

    public Map<BranchCnec, Double> getPrePerimeterMarginsInAbsoluteMW() {
        return prePerimeterMarginsInAbsoluteMW;
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
