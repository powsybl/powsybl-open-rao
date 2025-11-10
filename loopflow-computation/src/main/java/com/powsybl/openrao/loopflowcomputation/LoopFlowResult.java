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
import java.util.List;
import java.util.Map;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowResult {

    private final Map<BranchCnec<?>, Map<TwoSides, LoopFlow>> loopFlowMap;

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

        double getLoopFlow(Unit unit) {
            if (unit == this.unit) {
                return loopFlowValue;
            } else {
                throw new OpenRaoException(
                    String.format("Asked for unit %s but the loopflow was computed in %s", unit, this.unit)
                );
            }
        }

        double getCommercialFlow(Unit unit) {
            if (unit == this.unit) {
                return commercialFlowValue;
            } else {
                throw new OpenRaoException(
                    String.format("Asked for unit %s but the commercial flow was computed in %s", unit, this.unit)
                );
            }
        }

        double getTotalFlow(Unit unit) {
            if (unit == this.unit) {
                return totalFlowValue;
            } else {
                throw new OpenRaoException(
                    String.format("Asked for unit %s but the total flow was computed in %s", unit, this.unit)
                );
            }
        }

    }

    public LoopFlowResult() {
        this.loopFlowMap = new HashMap<>();
    }

    public void addCnecResult(BranchCnec<?> cnec, TwoSides side, double loopFlowValue, double commercialFlowValue, double referenceFlowValue, Unit unit) {
        loopFlowMap.computeIfAbsent(cnec, k -> new EnumMap<>(TwoSides.class)).put(side, new LoopFlow(loopFlowValue, commercialFlowValue, referenceFlowValue, unit));
    }

    public double getLoopFlow(BranchCnec<?> cnec, TwoSides side, Unit unit) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new OpenRaoException(String.format("No loop-flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getLoopFlow(unit);
    }

    public double getCommercialFlow(BranchCnec<?> cnec, TwoSides side, Unit unit ) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new OpenRaoException(String.format("No commercial flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getCommercialFlow(unit);
    }

    public double getReferenceFlow(BranchCnec<?> cnec, TwoSides side, Unit unit) {
        if (!loopFlowMap.containsKey(cnec) || !loopFlowMap.get(cnec).containsKey(side)) {
            throw new OpenRaoException(String.format("No reference flow value found for cnec %s on side %s", cnec.getId(), side));
        }
        return loopFlowMap.get(cnec).get(side).getTotalFlow(unit);
    }

    public Map<FlowCnec, Map<TwoSides, Map<Unit, Double>>> getCommercialFlowsMap() {
        Map<FlowCnec, Map<TwoSides, Map<Unit, Double>>> result = new HashMap<>();
        loopFlowMap.keySet().stream()
            .filter(FlowCnec.class::isInstance)
            .map(FlowCnec.class::cast)
            .forEach(cnec -> {
                Map<TwoSides, Map<Unit, Double>> sideMap = new EnumMap<>(TwoSides.class);
                loopFlowMap.get(cnec).keySet().forEach(side -> {
                    Map<Unit, Double> unitValues = new HashMap<>();
                    for (Unit unit : List.of(Unit.MEGAWATT, Unit.AMPERE)) {
                        unitValues.put(unit, this.getCommercialFlow(cnec, side, unit));
                    }
                    sideMap.put(side, unitValues);
                });
                result.put(cnec, sideMap);
            });
        return result;
    }

}
