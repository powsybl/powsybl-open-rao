package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;

import java.util.HashMap;
import java.util.Map;

public class LoopFlows {

    private Map<Cnec, LoopFlowResult> loopFlowmap;

    private class LoopFlowResult {
        double loopFlow;
        double commercialFlow;
        double totalFlow;

        LoopFlowResult(double loopFlow, double commercialFlow, double totalFlow) {
            this.loopFlow = loopFlow;
            this.commercialFlow = commercialFlow;
            this.totalFlow = totalFlow;
        }

        double getLoopFlow() {
            return loopFlow;
        }

        double getCommercialFlow() {
            return commercialFlow;
        }

        double getTotalFlow() {
            return totalFlow;
        }
    }

    LoopFlows() {
        this.loopFlowmap = new HashMap<>();
    }

    void addCnecResult(Cnec cnec, double loopFlowValue, double commercialFlowValue, double referenceFlowValue) {
        loopFlowmap.put(cnec, new LoopFlowResult(loopFlowValue, commercialFlowValue, referenceFlowValue));
    }

    public double getLoopFlow(Cnec cnec) {
        LoopFlowResult loopFlowResult = loopFlowmap.get(cnec);
        if (loopFlowResult == null) {
            throw new FaraoException(String.format("No loop-flow value found for cnec %s", cnec.getId()));
        }
        return loopFlowResult.getLoopFlow();
    }

    public double getCommercialFlow(Cnec cnec) {
        LoopFlowResult loopFlowResult = loopFlowmap.get(cnec);
        if (loopFlowResult == null) {
            throw new FaraoException(String.format("No commercial flow value found for cnec %s", cnec.getId()));
        }
        return loopFlowResult.getCommercialFlow();
    }

    public double getReferenceFlow(Cnec cnec) {
        LoopFlowResult loopFlowResult = loopFlowmap.get(cnec);
        if (loopFlowResult == null) {
            throw new FaraoException(String.format("No reference flow value found for cnec %s", cnec.getId()));
        }
        return loopFlowResult.getTotalFlow();
    }
}
