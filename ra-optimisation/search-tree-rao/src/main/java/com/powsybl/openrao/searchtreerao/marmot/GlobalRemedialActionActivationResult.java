package com.powsybl.openrao.searchtreerao.marmot;

import com.powsybl.openrao.commons.TemporalData;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GlobalRemedialActionActivationResult implements RemedialActionActivationResult {
    private final TemporalData<RemedialActionActivationResult> remedialActionActivationResultPerTimestamp;

    public GlobalRemedialActionActivationResult(TemporalData<RemedialActionActivationResult> remedialActionActivationResultPerTimestamp) {
        this.remedialActionActivationResultPerTimestamp = remedialActionActivationResultPerTimestamp;
    }

    @Override
    public boolean isActivated(NetworkAction networkAction) {
        return false;
    }

    @Override
    public Set<NetworkAction> getActivatedNetworkActions() {
        return Set.of();
    }

    @Override
    public Set<RangeAction<?>> getRangeActions() {
        Set<RangeAction<?>> allRangeActions = new HashSet<>();
        remedialActionActivationResultPerTimestamp.map(RemedialActionActivationResult::getRangeActions).getDataPerTimestamp().values().forEach(allRangeActions::addAll);
        return allRangeActions;
    }

    @Override
    public Set<RangeAction<?>> getActivatedRangeActions(State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getActivatedRangeActions(state);
    }

    @Override
    public double getOptimizedSetpoint(RangeAction<?> rangeAction, State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getOptimizedSetpoint(rangeAction, state);
    }

    @Override
    public Map<RangeAction<?>, Double> getOptimizedSetpointsOnState(State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getOptimizedSetpointsOnState(state);
    }

    @Override
    public double getSetPointVariation(RangeAction<?> rangeAction, State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getSetPointVariation(rangeAction, state);
    }

    @Override
    public int getOptimizedTap(PstRangeAction pstRangeAction, State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getOptimizedTap(pstRangeAction, state);
    }

    @Override
    public Map<PstRangeAction, Integer> getOptimizedTapsOnState(State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getOptimizedTapsOnState(state);
    }

    @Override
    public int getTapVariation(PstRangeAction pstRangeAction, State state) {
        return MarmotUtils.getDataFromState(remedialActionActivationResultPerTimestamp, state).getTapVariation(pstRangeAction, state);
    }
}
