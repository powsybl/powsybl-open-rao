package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.cnec.BranchCnec;

import java.util.Map;

public class CnecResults {

    private Map<BranchCnec, Double> flowsInMW;
    private Map<BranchCnec, Double> flowsInA;

    private Map<BranchCnec, Double> loopflowsInMW; //loopflow value in MW
    private Map<BranchCnec, Double> loopflowThresholdInMW; //loopflow threshold in MW. Normally = max(Tso input, initial calculated lp)
    private Map<BranchCnec, Double> commercialFlowsInMW;

    private Map<BranchCnec, Double> absolutePtdfSums;

    public Map<BranchCnec, Double> getFlowsInMW() {
        return flowsInMW;
    }

    public void setFlowsInMW(Map<BranchCnec, Double> flowsInMW) {
        this.flowsInMW = flowsInMW;
    }

    public Map<BranchCnec, Double> getFlowsInA() {
        return flowsInA;
    }

    public void setFlowsInA(Map<BranchCnec, Double> flowsInA) {
        this.flowsInA = flowsInA;
    }

    public Map<BranchCnec, Double> getLoopflowsInMW() {
        return loopflowsInMW;
    }

    public void setLoopflowsInMW(Map<BranchCnec, Double> loopflowsInMW) {
        this.loopflowsInMW = loopflowsInMW;
    }

    public Map<BranchCnec, Double> getLoopflowThresholdInMW() {
        return loopflowThresholdInMW;
    }

    public void setLoopflowThresholdInMW(Map<BranchCnec, Double> loopflowThresholdInMW) {
        this.loopflowThresholdInMW = loopflowThresholdInMW;
    }

    public Map<BranchCnec, Double> getCommercialFlowsInMW() {
        return commercialFlowsInMW;
    }

    public void setCommercialFlowsInMW(Map<BranchCnec, Double> commercialFlowsInMW) {
        this.commercialFlowsInMW = commercialFlowsInMW;
    }

    public Map<BranchCnec, Double> getAbsolutePtdfSums() {
        return absolutePtdfSums;
    }

    public void setAbsolutePtdfSums(Map<BranchCnec, Double> absolutePtdfSums) {
        this.absolutePtdfSums = absolutePtdfSums;
    }
}
