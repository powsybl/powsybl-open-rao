/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.marmot.results;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.marmot.MarmotUtils;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** This class aggregates RangeActionActivationResult stored in TemporalData<RangeActionActivationResult> in one big RangeActionActivationResult.
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 */
public class GlobalRangeActionActivationResult extends AbstractGlobalResult<RangeActionActivationResult> implements RangeActionActivationResult {
    public GlobalRangeActionActivationResult(TemporalData<RangeActionActivationResult> rangeActionActivationPerTimestamp) {
        super(rangeActionActivationPerTimestamp);
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        Set<RangeAction<?>> allRangeActions = new HashSet<>();
        resultPerTimestamp.map(RangeActionActivationResult::getRangeActions).getDataPerTimestamp().values().forEach(allRangeActions::addAll);
        return allRangeActions;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getActivatedRangeActions(state);
    }

    @Override
    public Map<State, Set<RangeAction<?>>> getActivatedRangeActionsPerState() {
        Map<State, Set<RangeAction<?>>> activatedRangeActionsPerState = new HashMap<>();
        resultPerTimestamp.map(RangeActionActivationResult::getActivatedRangeActionsPerState).getDataPerTimestamp().values().forEach(activatedRangeActionsPerState::putAll);
        return activatedRangeActionsPerState;
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getOptimizedSetpointsOnState(state);
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getSetPointVariation(rangeAction, state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getOptimizedTapsOnState(state);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return MarmotUtils.getDataFromState(resultPerTimestamp, state).getTapVariation(pstRangeAction, state);
    }
}
