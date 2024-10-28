package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;

public class NetworkActionsCostEvaluator implements RemedialActionsCostEvaluator {
    private final NetworkActionCombination networkActionCombination;

    public NetworkActionsCostEvaluator(NetworkActionCombination networkActionCombination) {
        this.networkActionCombination = networkActionCombination;
    }

    public double getTotalCost() {
        double totalCost = 0;
        for (NetworkAction networkAction : networkActionCombination.getNetworkActionSet()) {
            totalCost += networkAction.getActivationCost().orElse(0d);
        }
        return totalCost;
    }
}
