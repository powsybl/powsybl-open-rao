/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons.parameters;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.raoapi.parameters.NotOptimizedCnecsParameters;
import com.powsybl.openrao.searchtreerao.commons.RaoCommonsReports;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;
    private final Map<FlowCnec, RangeAction<?>> unoptimizedCnecsInSeriesWithPsts;

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize, Map<FlowCnec, RangeAction<?>> unoptimizedCnecsInSeriesWithPsts) {
        this.operatorNotToOptimize = operatorNotToOptimize;
        this.unoptimizedCnecsInSeriesWithPsts = unoptimizedCnecsInSeriesWithPsts;
    }

    public Map<FlowCnec, RangeAction<?>> getDoNotOptimizeCnecsSecuredByTheirPst() {
        return unoptimizedCnecsInSeriesWithPsts;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    // unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together.
    public static UnoptimizedCnecParameters build(NotOptimizedCnecsParameters parameters, Set<String> operatorsNotSharingCras, Crac crac, ReportNode reportNode) {
        if (parameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras()
                && !parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            throw new OpenRaoException("SearchTreeRaoParameters : unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together");
        } else if (parameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras()) {
            return new UnoptimizedCnecParameters(
                operatorsNotSharingCras,
                null);
        } else if (!parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            return new UnoptimizedCnecParameters(
                    null,
                    getDoNotOptimizeCnecsSecuredByTheirPstFromIds(parameters.getDoNotOptimizeCnecsSecuredByTheirPst(), crac, reportNode));
        } else {
            return null;
        }
    }

    public static Map<FlowCnec, RangeAction<?>> getDoNotOptimizeCnecsSecuredByTheirPst(NotOptimizedCnecsParameters parameters, Crac crac, ReportNode reportNode) {
        if (!parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            return getDoNotOptimizeCnecsSecuredByTheirPstFromIds(parameters.getDoNotOptimizeCnecsSecuredByTheirPst(), crac, reportNode);
        } else {
            return Collections.emptyMap();
        }
    }

    private static Map<FlowCnec, RangeAction<?>> getDoNotOptimizeCnecsSecuredByTheirPstFromIds(Map<String, String> ids, Crac crac, ReportNode reportNode) {
        Map<FlowCnec, RangeAction<?>> mapOfUnoptimizedCnecsAndPsts = new HashMap<>();
        // Create map elements for all cnecs with network element id in ids.keySet()
        for (Map.Entry<String, String> entrySet : ids.entrySet()) {
            String cnecId = entrySet.getKey();
            String pstId = entrySet.getValue();

            Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(flowCnec -> flowCnec.getNetworkElement().getId().equals(cnecId)).collect(Collectors.toSet());
            Set<PstRangeAction> pstRangeActions = crac.getPstRangeActions().stream().filter(pstRangeAction -> pstRangeAction.getNetworkElement().getId().equals(pstId)).collect(Collectors.toSet());

            if (skipEntry(cnecId, pstId, flowCnecs, pstRangeActions, reportNode)) {
                continue;
            }

            for (FlowCnec flowCnec : flowCnecs) {
                Set<PstRangeAction> availablePstRangeActions = pstRangeActions.stream().filter(pstRangeAction ->
                    pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.AVAILABLE) ||
                        pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.FORCED)).collect(Collectors.toSet());

                if (skipFlowCnec(availablePstRangeActions, pstId, reportNode)) {
                    continue;
                }

                mapOfUnoptimizedCnecsAndPsts.put(flowCnec, availablePstRangeActions.iterator().next());
            }
        }
        return mapOfUnoptimizedCnecsAndPsts;
    }

    private static boolean skipEntry(String cnecId, String pstId, Set<FlowCnec> flowCnecs, Set<PstRangeAction> pstRangeActions, ReportNode reportNode) {
        if (flowCnecs.isEmpty()) {
            RaoCommonsReports.reportNoFlowCnecWithId(reportNode, cnecId);
            return true;
        }

        if (pstRangeActions.isEmpty()) {
            RaoCommonsReports.reportNoPstRangeActionWithNetworkElement(reportNode, pstId);
            return true;
        }
        return false;
    }

    private static boolean skipFlowCnec(Set<PstRangeAction> availablePstRangeActions, String pstId, ReportNode reportNode) {
        if (availablePstRangeActions.size() > 1) {
            RaoCommonsReports.reportMultiplePstRangeActions(reportNode, availablePstRangeActions.size(), pstId);
            return true;
        }

        return availablePstRangeActions.isEmpty();
    }
}
