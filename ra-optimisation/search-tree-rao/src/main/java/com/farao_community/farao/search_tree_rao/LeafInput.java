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
    private SensitivityAndLoopflowResults sensitivityAndLoopflowResults;
    private Map<BranchCnec, Double> commercialFlows;
    private Set<NetworkAction> appliedNetworkActions;
    private NetworkAction networkActionToApply;
    private Set<NetworkAction> availableNetworkActions;
    private Network network;
    private Set<RangeAction> rangeActions;
    private Set<BranchCnec> cnecs;
    private Set<BranchCnec> loopflowCnecs;
    private ZonalData<LinearGlsk> glskProvider;
    private ReferenceProgram referenceProgram;
    private Map<BranchCnec, Double> prePerimeterMarginsInAbsoluteMW;
    private Map<RangeAction, Double> preperimeterSetpoints; // can be removed if we don't change taps in the network after each depth
    private CnecResults initialCnecResults;
    private Set<String> countriesNotToOptimize;

    public boolean hasSensitivityValues() {
        return !Objects.isNull(sensitivityAndLoopflowResults);
    }

    public SensitivityAndLoopflowResults getSensitivityAndLoopflowResults() {
        return sensitivityAndLoopflowResults;
    }

    public void setSensitivityAndLoopflowResults(SensitivityAndLoopflowResults sensitivityAndLoopflowResults) {
        this.sensitivityAndLoopflowResults = sensitivityAndLoopflowResults;
    }

    public Map<BranchCnec, Double> getCommercialFlows() {
        return commercialFlows;
    }

    public Set<NetworkAction> getAppliedNetworkActions() {
        return appliedNetworkActions;
    }

    public NetworkAction getNetworkActionToApply() {
        return networkActionToApply;
    }

    public Set<NetworkAction> getAvailableNetworkActions() {
        return availableNetworkActions;
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

    public Map<RangeAction, Double> getPreperimeterSetpoints() {
        return preperimeterSetpoints;
    }

    public Double getPreperimeterSetpoint(RangeAction rangeAction) {
        return preperimeterSetpoints.get(rangeAction);
    }

    public CnecResults getInitialCnecResults() {
        return initialCnecResults;
    }

    public Set<String> getCountriesNotToOptimize() {
        return countriesNotToOptimize;
    }
}
