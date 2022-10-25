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
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.commons.logs.FaraoLoggerProvider.BUSINESS_WARNS;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class UnoptimizedCnecParameters {
    private final Set<String> operatorNotToOptimize;
    private final Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts;

    public UnoptimizedCnecParameters(Set<String> operatorNotToOptimize, Map<FlowCnec, PstRangeAction> unoptimizedCnecsInSeriesWithPsts) {
        this.operatorNotToOptimize = operatorNotToOptimize;
        this.unoptimizedCnecsInSeriesWithPsts = unoptimizedCnecsInSeriesWithPsts;
    }

    public Map<FlowCnec, PstRangeAction>  getUnoptimizedCnecsInSeriesWithPsts() {
        return unoptimizedCnecsInSeriesWithPsts;
    }

    public Set<String> getOperatorsNotToOptimize() {
        return operatorNotToOptimize;
    }

    // unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together.
    public static UnoptimizedCnecParameters build(RaoParameters raoParameters, Set<String> operatorsNotSharingCras, Crac crac) {
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }
        if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()
                && !searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty()) {
            throw new FaraoException("SearchTreeRaoParameters : unoptimizedCnecsInSeriesWithPsts and operatorNotToOptimize cannot be activated together");
        } else if (!searchTreeRaoParameters.getCurativeRaoOptimizeOperatorsNotSharingCras()) {
            return new UnoptimizedCnecParameters(
                operatorsNotSharingCras,
                null);
        } else if (!searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty()) {
            return new UnoptimizedCnecParameters(
                    null,
                    getUnoptimizedCnecsInSeriesWithPstsFromIds(searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds(), crac));
        } else {
            return null;
        }
    }

    public static Map<FlowCnec, PstRangeAction> getUnoptimizedCnecsInSeriesWithPsts(RaoParameters raoParameters, Crac crac) {
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        if (searchTreeRaoParameters == null) {
            throw new FaraoException("RaoParameters must contain SearchTreeRaoParameters when running a SearchTreeRao");
        }
        if (!searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds().isEmpty()) {
            return getUnoptimizedCnecsInSeriesWithPstsFromIds(searchTreeRaoParameters.getUnoptimizedCnecsInSeriesWithPstsIds(), crac);
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

            if (flowCnecs.isEmpty()) {
                BUSINESS_WARNS.warn("No flowCnec with network element id {} exists in unoptimized-cnecs-in-series-with-psts parameter", cnecId);
                continue;
            }

            for (FlowCnec flowCnec : flowCnecs) {
                Set<PstRangeAction> availablePstRangeActions = pstRangeActions.stream().filter(pstRangeAction ->
                        pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.AVAILABLE) ||
                                pstRangeAction.getUsageMethod(flowCnec.getState()).equals(UsageMethod.TO_BE_EVALUATED)).collect(Collectors.toSet());

                if (skipFlowCnec(availablePstRangeActions)) {
                    continue;
                }

                if (availablePstRangeActions.size() == 1) {
                    mapOfUnoptimizedCnecsAndPsts.put(flowCnec, availablePstRangeActions.iterator().next());
                }
            }
        }
        return mapOfUnoptimizedCnecsAndPsts;
    }

    private static boolean skipFlowCnec(Set<PstRangeAction> availablePstRangeActions) {
        if (availablePstRangeActions.size() > 1) {
            BUSINESS_WARNS.warn("{} pst range actions are defined with network element {} instead of 1", availablePstRangeActions.size(), pstId);
            return true;
        }

        if (availablePstRangeActions.isEmpty()) {
            BUSINESS_WARNS.warn("No pst range actions are defined with network element {}", pstId);
            return true;
        }
        return false;
    }
}
