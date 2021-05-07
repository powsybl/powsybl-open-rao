package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_api.RangeAction;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.rao_api.results.BranchResult;
import com.farao_community.farao.rao_api.results.SensitivityResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.util.Map;
import java.util.Set;

public class SearchTreeInput {
    private Network network;
    private Set<BranchCnec> cnecs;
    private Set<NetworkAction> networkActions;
    private Set<RangeAction> rangeActions;
    private Set<String> countriesNotToOptimize;

    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;

    private BranchResult initialBranchResult;
    private BranchResult prePerimeterBranchResult;
    private SensitivityResult prePerimeterSensitivityResult;
    private Map<RangeAction, Double> prePerimeterSetpoints;

    public SensitivityResult getPrePerimeterSensitivityResult() {
        return prePerimeterSensitivityResult;
    }

    public void setPrePerimeterSensitivityResult(SensitivityResult prePerimeterSensitivityResult) {
        this.prePerimeterSensitivityResult = prePerimeterSensitivityResult;
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

    public Set<String> getCountriesNotToOptimize() {
        return countriesNotToOptimize;
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

    public Map<RangeAction, Double> getPrePerimeterSetpoints() {
        return prePerimeterSetpoints;
    }

    public BranchResult getInitialBranchResult() {
        return initialBranchResult;
    }

    public BranchResult getPrePerimeterBranchResult() {
        return prePerimeterBranchResult;
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

    public void setPrePerimeterSetpoints(Map<RangeAction, Double> prePerimeterSetpoints) {
        this.prePerimeterSetpoints = prePerimeterSetpoints;
    }

    public void setInitialBranchResult(BranchResult initialBranchResult) {
        this.initialBranchResult = initialBranchResult;
    }

    public void setPrePerimeterBranchResult(BranchResult prePerimeterBranchResult) {
        this.prePerimeterBranchResult = prePerimeterBranchResult;
    }

    public void setCountriesNotToOptimize(Set<String> countriesNotToOptimize) {
        this.countriesNotToOptimize = countriesNotToOptimize;
    }
}
