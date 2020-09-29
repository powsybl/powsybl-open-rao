package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Cnec;

import java.util.HashMap;
import java.util.Map;

public class LoopFlowResult {

    private Map<Cnec, LoopFlow> loopFlowmap;

    private class LoopFlow {
        double loopFlow;
        double commercialFlow;
        double totalFlow;

        LoopFlow(double loopFlow, double commercialFlow, double totalFlow) {
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

    LoopFlowResult() {
        this.loopFlowmap = new HashMap<>();
    }

    void addCnecResult(Cnec cnec, double loopFlowValue, double commercialFlowValue, double referenceFlowValue) {
        loopFlowmap.put(cnec, new LoopFlow(loopFlowValue, commercialFlowValue, referenceFlowValue));
    }

    public double getLoopFlow(Cnec cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No loop-flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getLoopFlow();
    }

    public double getCommercialFlow(Cnec cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No commercial flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getCommercialFlow();
    }

    public double getReferenceFlow(Cnec cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No reference flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getTotalFlow();
    }
}
