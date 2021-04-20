package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.loopflow_computation.LoopFlowResult;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SensitivityAndLoopflowResults {
    SystematicSensitivityResult systematicSensitivityResult;
    Map<BranchCnec, Double> commercialFlows;

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = null;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, Map<BranchCnec, Double> commercialFlows) {
        this.systematicSensitivityResult = systematicSensitivityResult;
        this.commercialFlows = commercialFlows;
    }

    public SensitivityAndLoopflowResults(SystematicSensitivityResult systematicSensitivityResult, LoopFlowResult loopFlowResult, Set<BranchCnec> loopflowCnecs) {
        this.systematicSensitivityResult = systematicSensitivityResult;
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
}
