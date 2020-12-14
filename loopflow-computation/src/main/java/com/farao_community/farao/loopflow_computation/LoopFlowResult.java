/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.cnec.Cnec;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowResult {

    private Map<Cnec<?>, LoopFlow> loopFlowmap;

    private class LoopFlow {
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
        this.loopFlowmap = new HashMap<>();
    }

    public void addCnecResult(Cnec<?> cnec, double loopFlowValue, double commercialFlowValue, double referenceFlowValue) {
        loopFlowmap.put(cnec, new LoopFlow(loopFlowValue, commercialFlowValue, referenceFlowValue));
    }

    public double getLoopFlow(Cnec<?> cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No loop-flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getLoopFlow();
    }

    public double getCommercialFlow(Cnec<?> cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No commercial flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getCommercialFlow();
    }

    public double getReferenceFlow(Cnec<?> cnec) {
        LoopFlow loopFlow = loopFlowmap.get(cnec);
        if (loopFlow == null) {
            throw new FaraoException(String.format("No reference flow value found for cnec %s", cnec.getId()));
        }
        return loopFlow.getTotalFlow();
    }

    public boolean containValues(Cnec<?> cnec) {
        return loopFlowmap.get(cnec) != null;
    }
}
