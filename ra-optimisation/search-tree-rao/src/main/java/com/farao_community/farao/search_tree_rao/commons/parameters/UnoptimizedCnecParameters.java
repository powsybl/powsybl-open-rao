/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.rao_api.parameters.NotOptimizedCnecsParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;
    private final Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts;
    private final static Map<Integer, Map<FlowCnec, PstRangeAction>> UNOPTIMIZED_CNECS_PSTS_CACHE = new HashMap<>();

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize, Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts) {
        this.operatorNotToOptimize = operatorNotToOptimize;
        this.unoptimizedCnecsInSeriesWithPsts = unoptimizedCnecsInSeriesWithPsts;
    }

    public Map<FlowCnec, PstRangeAction> getUnoptimizedCnecsInSeriesWithPsts() {
        return unoptimizedCnecsInSeriesWithPsts;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    // unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together.
    public static UnoptimizedCnecParameters build(NotOptimizedCnecsParameters parameters, Set<String> operatorsNotSharingCras, Crac crac) {
        if (parameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras()
                && !parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            throw new FaraoException("SearchTreeRaoParameters : unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together");
        } else if (parameters.getDoNotOptimizeCurativeCnecsForTsosWithoutCras()) {
            return new UnoptimizedCnecParameters(
                operatorsNotSharingCras,
                null);
        } else if (!parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            return new UnoptimizedCnecParameters(
                    null,
                    getUnoptimizedCnecsInSeriesWithPstsFromIds(parameters.getDoNotOptimizeCnecsSecuredByTheirPst(), crac));
        } else {
            return null;
        }
    }

    public static Map<FlowCnec, PstRangeAction> getUnoptimizedCnecsInSeriesWithPsts(NotOptimizedCnecsParameters parameters, Crac crac) {
        if (!parameters.getDoNotOptimizeCnecsSecuredByTheirPst().isEmpty()) {
            return getUnoptimizedCnecsInSeriesWithPstsFromIds(parameters.getDoNotOptimizeCnecsSecuredByTheirPst(), crac);
        } else {
            return Collections.emptyMap();
        }
    }

    private static Map<FlowCnec, PstRangeAction> getUnoptimizedCnecsInSeriesWithPstsFromIds(Map<String, String> ids, Crac crac) {
        Map<FlowCnec, PstRangeAction> mapOfUnoptimizedCnecsAndPsts = new HashMap<>();
        // Create map elements for all cnecs with network element id in ids.keySet()
        for (Map.Entry<String, String> entrySet : ids.entrySet()) {
            String cnecId = entrySet.getKey();
            String pstId = entrySet.getValue();

            Set<FlowCnec> flowCnecs = crac.getFlowCnecs().stream().filter(flowCnec -> flowCnec.getNetworkElement().getId().equals(cnecId)).collect(Collectors.toSet());
            Set<PstRangeAction> pstRangeActions = crac.getPstRangeActions().stream().filter(pstRangeAction -> pstRangeAction.getNetworkElement().getId().equals(pstId)).collect(Collectors.toSet());

            if (skipEntry(cnecId, pstId, flowCnecs, pstRangeActions)) {
                continue;
            }

            for (FlowCnec flowCnec : flowCnecs) {
                Set<PstRangeAction> availablePstRangeActions = pstRangeActions.stream().filter(pstRangeAction ->
                        pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.AVAILABLE) ||
                                pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.TO_BE_EVALUATED)).collect(Collectors.toSet());

                if (skipFlowCnec(availablePstRangeActions, pstId)) {
                    continue;
                }

                mapOfUnoptimizedCnecsAndPsts.put(flowCnec, availablePstRangeActions.iterator().next());
            }
        }
        return mapOfUnoptimizedCnecsAndPsts;
    }

    private static boolean skipEntry(String cnecId, String pstId, Set<FlowCnec> flowCnecs, Set<PstRangeAction> pstRangeActions) {
        if (flowCnecs.isEmpty()) {
            TECHNICAL_LOGS.debug("No flowCnec with network element id {} exists in unoptimized-cnecs-in-series-with-psts parameter", cnecId);
            return true;
        }

        if (pstRangeActions.isEmpty()) {
            TECHNICAL_LOGS.debug("No pst range actions are defined with network element {}", pstId);
            return true;
        }
        return false;
    }

    private static boolean skipFlowCnec(Set<PstRangeAction> availablePstRangeActions, String pstId) {
        if (availablePstRangeActions.size() > 1) {
            TECHNICAL_LOGS.debug("{} pst range actions are defined with network element {} instead of 1", availablePstRangeActions.size(), pstId);
            return true;
        }

        return availablePstRangeActions.isEmpty();
    }
}
