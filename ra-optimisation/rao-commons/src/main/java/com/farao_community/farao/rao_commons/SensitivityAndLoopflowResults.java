package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.rao_api.results.SensitivityStatus;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SensitivityAndLoopflowResults {
    SystematicSensitivityResult systematicSensitivityResult;
    Map<BranchCnec, Double> commercialFlows;
    SensitivityStatus sensitivityStatus;

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, SensitivityStatus sensitivityStatus) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.sensitivityStatus = sensitivityStatus;
        this.commercialFlows = null;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, SensitivityStatus sensitivityStatus, Map<BranchCnec, Double> commercialFlows) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.sensitivityStatus = sensitivityStatus;
        this.commercialFlows = commercialFlows;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, SensitivityStatus sensitivityStatus, LoopFlowResult loopFlowResult, Set<BranchCnec> loopflowCnecs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.sensitivityStatus = sensitivityStatus;
        this.commercialFlows = new HashMap<>();
        loopflowCnecs.forEach(cnec -> commercialFlows.put(cnec, loopFlowResult.getCommercialFlow(cnec)));
    }

    public SystematicSensitivityResult getSystematicSensitivityResult() {
        return systematicSensitivityResult;
    }

    public double getCommercialFlow(BranchCnec cnec) {
        return this.commercialFlows.get(cnec);
    }

    public Map<BranchCnec, Double> getCommercialFlows() {
        return commercialFlows;
    }

    public double getLoopflow(BranchCnec cnec) {
        return systematicSensitivityResult.getReferenceFlow(cnec) - this.commercialFlows.get(cnec);
    }

    public SensitivityStatus getSensitivityStatus() {
        return sensitivityStatus;
    }

    public boolean isFallback() {
        return sensitivityStatus.equals(SensitivityStatus.FALLBACK);
    }
}
