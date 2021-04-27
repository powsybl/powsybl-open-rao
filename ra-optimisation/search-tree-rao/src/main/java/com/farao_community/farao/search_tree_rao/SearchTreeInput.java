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

public class SearchTreeInput {
    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<NetworkAction> networkActions;
    private Set<RangeAction> rangeActions;

    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    private CnecResults initialCnecResults;
    private SensitivityAndLoopflowResults prePerimeterSensitivityAndLoopflowResults;
    private Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW;
    private Map<RangeAction, Double> prePerimeterSetpoints;
    private Map<BranchCnec, Double> prePerimeterCommercialFlows;

    public boolean hasPrePerimeterSensitivityValues() {
        return !Objects.isNull(prePerimeterSensitivityAndLoopflowResults);
    }

    public SensitivityAndLoopflowResults getPrePerimeterSensitivityAndLoopflowResults() {
        return prePerimeterSensitivityAndLoopflowResults;
    }

    public void setPrePerimeterSensitivityAndLoopflowResults(SensitivityAndLoopflowResults prePerimeterSensitivityAndLoopflowResults) {
        this.prePerimeterSensitivityAndLoopflowResults = prePerimeterSensitivityAndLoopflowResults;
    }

    public Map<BranchCnec, Double> getCommercialFlows() {
        return prePerimeterCommercialFlows;
    }

    public Set<NetworkAction> getNetworkActions() {
        return networkActions;
    }

    public Network getNetwork() {
        return network;
    }

    public Set<RangeAction> getRangeActions() {
        return rangeActions;
    }

    public Set<BranchCnec> getCnecs() {
        return cnecs;
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

    public Map<BranchCnec, Double> getPrePerimeterMarginsInAbsoluteMW() {
        return prePerimeterMarginsInAbsoluteMW;
    }

    public Map<RangeAction, Double> getPrePerimeterSetpoints() {
        return prePerimeterSetpoints;
    }

    public Double getPrePerimeterSetpoint(RangeAction rangeAction) {
        return prePerimeterSetpoints.get(rangeAction);
    }

    public CnecResults getInitialCnecResults() {
        return initialCnecResults;
    }

    public void setPrePerimeterCommercialFlows(Map<BranchCnec, Double> prePerimeterCommercialFlows) {
        this.prePerimeterCommercialFlows = prePerimeterCommercialFlows;
    }

    public void setNetworkActions(Set<NetworkAction> networkActions) {
        this.networkActions = networkActions;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setRangeActions(Set<RangeAction> rangeActions) {
        this.rangeActions = rangeActions;
    }

    public void setCnecs(Set<BranchCnec> cnecs) {
        this.cnecs = cnecs;
    }

    public void setLoopflowCnecs(Set<BranchCnec> loopflowCnecs) {
        this.loopflowCnecs = loopflowCnecs;
    }

    public void setGlskProvider(ZonalData<LinearGlsk> glskProvider) {
        this.glskProvider = glskProvider;
    }

    public void setReferenceProgram(ReferenceProgram referenceProgram) {
        this.referenceProgram = referenceProgram;
    }

    public void setPrePerimeterMarginsInAbsoluteMW(Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW) {
        this.prePerimeterMarginsInAbsoluteMW = prePerimeterMarginsInAbsoluteMW;
    }

    public void setPrePerimeterSetpoints(Map<RangeAction, Double> prePerimeterSetpoints) {
        this.prePerimeterSetpoints = prePerimeterSetpoints;
    }

    public void setInitialCnecResults(CnecResults initialCnecResults) {
        this.initialCnecResults = initialCnecResults;
    }
}
