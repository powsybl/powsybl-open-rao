/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.raoimpl.algorithms.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoimpl.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.raoimpl.result.api.SensitivityResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getHvdcSensitivityThreshold;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getInjectionRaSensitivityThreshold;
import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getPstSensitivityThreshold;

/**
 * Computes which range actions (and at which state) impact a flow CNEC in the linear problem,
 * with the corresponding sensitivity.
 */
public class RangeActionImpacts {
    public record Impact(RangeAction<?> rangeAction, State state, double sensitivity) {
    }

    private final OptimizationPerimeter perimeter;
    private final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    private final Unit unit;
    private final Map<RangeAction<?>, Set<RangeAction<?>>> memoizedSameRangeActions = new HashMap<>();

    public RangeActionImpacts(OptimizationPerimeter perimeter, SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension, Unit unit) {
        this.perimeter = perimeter;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.unit = unit;
    }

    public List<Impact> getImpacts(FlowCnec cnec, TwoSides side, SensitivityResult sensitivityResult) {
        List<State> statesBeforeCnec = FillersUtil.getPreviousStates(cnec.getState(), perimeter).stream()
            .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())) // start with curative state
            .toList();

        List<Impact> impacts = new ArrayList<>();
        Set<RangeAction<?>> alreadyConsideredAction = new HashSet<>();

        for (State state : statesBeforeCnec) {
            // Impact of range action on cnec is only added on the last instant on which rangeAction is available
            for (RangeAction<?> rangeAction : perimeter.getRangeActionsPerState().get(state)) {
                if (!alreadyConsideredAction.contains(rangeAction)) {
                    double sensitivity = sensitivityResult.getSensitivityValue(cnec, side, rangeAction, unit);
                    if (isRangeActionSensitivityAboveThreshold(rangeAction, Math.abs(sensitivity))) {
                        impacts.add(new Impact(rangeAction, state, sensitivity));
                    }
                    alreadyConsideredAction.addAll(getAvailableRangeActionsOnSameAction(rangeAction));
                }
            }
        }
        return impacts;
    }

    private boolean isRangeActionSensitivityAboveThreshold(RangeAction<?> rangeAction, double sensitivity) {
        if (rangeAction instanceof PstRangeAction) {
            return sensitivity >= getPstSensitivityThreshold(rangeActionParametersExtension);
        } else if (rangeAction instanceof HvdcRangeAction) {
            return sensitivity >= getHvdcSensitivityThreshold(rangeActionParametersExtension);
        } else if (rangeAction instanceof InjectionRangeAction) {
            return sensitivity >= getInjectionRaSensitivityThreshold(rangeActionParametersExtension);
        } else {
            throw new OpenRaoException("Type of RangeAction not yet handled by the LinearRao.");
        }
    }

    private Set<RangeAction<?>> getAvailableRangeActionsOnSameAction(RangeAction<?> rangeAction) {
        if (memoizedSameRangeActions.containsKey(rangeAction)) {
            return memoizedSameRangeActions.get(rangeAction);
        }
        Set<RangeAction<?>> rangeActions = new HashSet<>();
        perimeter.getRangeActionsPerState().forEach((state, raSet) ->
            raSet.forEach(ra -> {
                if (ra.getId().equals(rangeAction.getId()) || ra.getNetworkElementsIds().equals(rangeAction.getNetworkElementsIds())) {
                    rangeActions.add(ra);
                }
            })
        );
        memoizedSameRangeActions.put(rangeAction, rangeActions);
        return rangeActions;
    }
}
