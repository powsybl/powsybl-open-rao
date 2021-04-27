package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_commons.CnecResults;
import com.farao_community.farao.rao_commons.SensitivityAndLoopflowResults;
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
    private Set<NetworkAction> availableNetworkActions;
    private Set<RangeAction> rangeActions;
    private Set<String> countriesNotToOptimize;
    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    private CnecResults initialCnecResults;
    private SensitivityAndLoopflowResults prePerimeterSensitivityAndLoopflowResults;
    private Map<BranchCnec, Double> prePerimeterCommercialFlows;
    private Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW;
    private Map<RangeAction, Double> prePerimeterSetpoints; // can be removed if we don't change taps in the network after each depth

    public LeafInput(SearchTreeInput searchTreeInput) {

    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public Set<BranchCnec> getCnecs() {
        return cnecs;
    }

    public void setCnecs(Set<BranchCnec> cnecs) {
        this.cnecs = cnecs;
    }

    public Set<NetworkAction> getAppliedNetworkActions() {
        return appliedNetworkActions;
    }

    public void setAppliedNetworkActions(Set<NetworkAction> appliedNetworkActions) {
        this.appliedNetworkActions = appliedNetworkActions;
    }

    public NetworkAction getNetworkActionToApply() {
        return networkActionToApply;
    }

    public void setNetworkActionToApply(NetworkAction networkActionToApply) {
        this.networkActionToApply = networkActionToApply;
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return availableNetworkActions;
    }

    public void setAvailableNetworkActions(Set<NetworkAction> availableNetworkActions) {
        this.availableNetworkActions = availableNetworkActions;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public void setRangeActions(Set<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    public Set<String> getCountriesNotToOptimize() {
        return countriesNotToOptimize;
    }

    public void setCountriesNotToOptimize(Set<String> countriesNotToOptimize) {
        this.countriesNotToOptimize = countriesNotToOptimize;
    }

    public Set<BranchCnec> getLoopflowCnecs() {
        return loopflowCnecs;
    }

    public void setLoopflowCnecs(Set<BranchCnec> loopflowCnecs) {
        this.loopflowCnecs = loopflowCnecs;
    }

    public ZonalData<LinearGlsk> getGlskProvider() {
        return glskProvider;
    }

    public void setGlskProvider(ZonalData<LinearGlsk> glskProvider) {
        this.glskProvider = glskProvider;
    }

    public ReferenceProgram getReferenceProgram() {
        return referenceProgram;
    }

    public void setReferenceProgram(ReferenceProgram referenceProgram) {
        this.referenceProgram = referenceProgram;
    }

    public CnecResults getInitialCnecResults() {
        return initialCnecResults;
    }

    public void setInitialCnecResults(CnecResults initialCnecResults) {
        this.initialCnecResults = initialCnecResults;
    }

    public SensitivityAndLoopflowResults getPrePerimeterSensitivityAndLoopflowResults() {
        return prePerimeterSensitivityAndLoopflowResults;
    }

    public void setPrePerimeterSensitivityAndLoopflowResults(SensitivityAndLoopflowResults prePerimeterSensitivityAndLoopflowResults) {
        this.prePerimeterSensitivityAndLoopflowResults = prePerimeterSensitivityAndLoopflowResults;
    }

    public Map<BranchCnec, Double> getPrePerimeterCommercialFlows() {
        return prePerimeterCommercialFlows;
    }

    public void setPrePerimeterCommercialFlows(Map<BranchCnec, Double> prePerimeterCommercialFlows) {
        this.prePerimeterCommercialFlows = prePerimeterCommercialFlows;
    }

    public Map<BranchCnec, Double> getPrePerimeterMarginsInAbsoluteMW() {
        return prePerimeterMarginsInAbsoluteMW;
    }

    public void setPrePerimeterMarginsInAbsoluteMW(Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW) {
        this.prePerimeterMarginsInAbsoluteMW = prePerimeterMarginsInAbsoluteMW;
    }

    public Map<RangeAction, Double> getPrePerimeterSetpoints() {
        return prePerimeterSetpoints;
    }

    public void setPrePerimeterSetpoints(Map<RangeAction, Double> prePerimeterSetpoints) {
        this.prePerimeterSetpoints = prePerimeterSetpoints;
    }
}
