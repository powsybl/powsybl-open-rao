/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.loopflow_computation;

import com.powsybl.open_rao.commons.FaraoException;
import com.powsybl.open_rao.data.crac_api.cnec.BranchCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowResult {

    private final Map<BranchCnec<?>, Map<Side, LoopFlow>> loopFlowMap;

    private static class LoopFlow {
        double loopFlowValue;
        double commercialFlowValue;
        double totalFlowValue;

        LoopFlow(double loopFlow, double commercialFlow, double totalFlow) {
            this.loopFlowValue = loopFlow;
            this.commercialFlowValue = commercialFlow;
            this.totalFlowValue = totalFlow;
        }

        double getLoopFlow() {
            return loopFlowValue;
        }

        double getCommercialFlow() {
            return commercialFlowValue;
        }

        double getTotalFlow() {
            return totalFlowValue;
        }
    }

    public LoopFlowResult() {
        this.loopFlowMap = new HashMap<>();
    }

    public void addCnecResult(BranchCnec<?> cnec, Side side, double loopFlowValue, double commercialFlowValue, double referenceFlowValue) {
        loopFlowMap.computeIfAbsent(cnec, k -> new EnumMap<>(Side.class)).put(side, new LoopFlow(loopFlowValue, commercialFlowValue, referenceFlowValue));
    }

    public double getLoopFlow(BranchCnec<?> cnec, Side side) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new FaraoException(String.format("No loop-flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getLoopFlow();
    }

    public double getCommercialFlow(BranchCnec<?> cnec, Side side) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new FaraoException(String.format("No commercial flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getCommercialFlow();
    }

    public double getReferenceFlow(BranchCnec<?> cnec, Side side) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new FaraoException(String.format("No reference flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getTotalFlow();
    }
}
