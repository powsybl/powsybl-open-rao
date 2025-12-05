/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.loopflowcomputation;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowResult {

    private final Map<BranchCnec<?>, Map<TwoSides, Map<Unit, LoopFlow>>> loopFlowMap;

    private static final class LoopFlow {
        double loopFlowValue;
        double commercialFlowValue;
        double totalFlowValue;
        Unit unit;

        LoopFlow(double loopFlow, double commercialFlow, double totalFlow, Unit unit) {
            this.loopFlowValue = loopFlow;
            this.commercialFlowValue = commercialFlow;
            this.totalFlowValue = totalFlow;
            this.unit = unit;
        }

        Double getLoopFlow() {
            return loopFlowValue;
        }

        Double getCommercialFlow() {
            return commercialFlowValue;
        }

        Double getTotalFlow() {
            return totalFlowValue;
        }

        Unit getUnit() {
            return unit;
        }
    }

    public LoopFlowResult() {
        this.loopFlowMap = new HashMap<>();
    }

    public void addCnecResult(BranchCnec<?> cnec, TwoSides side, double loopFlowValue, double commercialFlowValue, double referenceFlowValue, Unit unit) {
        loopFlowMap.computeIfAbsent(cnec, k -> new EnumMap<>(TwoSides.class))
            .computeIfAbsent(side, s -> new EnumMap<>(Unit.class))
            .put(unit, new LoopFlow(loopFlowValue, commercialFlowValue, referenceFlowValue, unit));
    }

    public Double getLoopFlow(BranchCnec<?> cnec, TwoSides side, Unit unit) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side) || !loopFlowMap.get(cnec).get(side).containsKey(unit)) {
            throw new OpenRaoException(String.format("No loop-flow value found for cnec %s on side %s in %s", cnec.getId(), side, unit));
        }
        return loopFlowMap.get(cnec).get(side).get(unit).getLoopFlow();
    }

    public Double getCommercialFlow(BranchCnec<?> cnec, TwoSides side, Unit unit) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side) || !loopFlowMap.get(cnec).get(side).containsKey(unit)) {
            throw new OpenRaoException(String.format("No commercial flow value found for cnec %s on side %s in %s", cnec.getId(), side, unit));
        }
        return loopFlowMap.get(cnec).get(side).get(unit).getCommercialFlow();
    }

    public Double getReferenceFlow(BranchCnec<?> cnec, TwoSides side, Unit unit) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side) || !loopFlowMap.get(cnec).get(side).containsKey(unit)) {
            throw new OpenRaoException(String.format("No reference flow value found for cnec %s on side %s in %s", cnec.getId(), side, unit));
        }
        return loopFlowMap.get(cnec).get(side).get(unit).getTotalFlow();
    }

    public Map<FlowCnec, Map<TwoSides, Map<Unit, Double>>> getCommercialFlowsMap() {
        return loopFlowMap.entrySet().stream()
            // only keep FlowCnec keys
            .filter(e -> e.getKey() instanceof FlowCnec)
            .collect(Collectors.toMap(
                e -> (FlowCnec) e.getKey(),
                e -> e.getValue().entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey, // TwoSides
                        sideEntry -> sideEntry.getValue().entrySet().stream()
                            .collect(Collectors.toMap(
                                Map.Entry::getKey, // Unit
                                unitEntry -> getCommercialFlow(e.getKey(), sideEntry.getKey(), unitEntry.getKey())
                            ))
                    ))
            ));

    }

}
