package com.farao_community.farao.data.crac_result_extensions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecResult {

    private double flowInMW;
    private double flowInA;

    @JsonCreator
    public CnecResult(@JsonProperty("flowInMw") double flowInMW, @JsonProperty("flowInA") double flowInA) {
        this.flowInMW = flowInMW;
        this.flowInA = flowInA;
    }

    public CnecResult(double flowInMW) {
        this.flowInMW = flowInMW;
        this.flowInA = Double.NaN;
    }

    public CnecResult() {
        this.flowInMW = Double.NaN;
        this.flowInA = Double.NaN;
    }

    public void setFlowInMW(double flow) {
        this.flowInMW = flow;
    }

    public double getFlowInMW() {
        return flowInMW;
    }

    public void setFlowInA(double flow) {
        this.flowInA = flow;
    }

    public double getFlowInA() {
        return flowInA;
    }
}
