package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.AreaGraph;
import com.powsybl.openrao.commons.AreaPairBoundary;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CounterTradeAreaBalanceFiller implements ProblemFiller {

    private final Map<State, Set<CounterTradeRangeAction>> counterTradeActions;
    private final AreaGraph areaGraph;

    public CounterTradeAreaBalanceFiller(Map<State, Set<CounterTradeRangeAction>> counterTradeActions, AreaGraph areaGraph) {
        this.counterTradeActions = counterTradeActions;
        this.areaGraph = areaGraph;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult,
                     SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {

        counterTradeActions.forEach((state, counterTradeRangeActions) -> {
            Set<AreaPairBoundary> alreadyCreatedBorders = new HashSet<>();

            for (CounterTradeRangeAction ra : counterTradeRangeActions) {
                String area = ra.getArea();
                OpenRaoMPVariable ctVar = linearProblem.getRangeActionSetpointVariable(ra, state);
                OpenRaoMPConstraint balance = linearProblem.addCounterTradeAreaBalanceConstraint(ra, state);
                balance.setCoefficient(ctVar, -1.0);
                for (String neighbour : areaGraph.getNeighbours(area)) {
                    AreaPairBoundary pairKey = AreaPairBoundary.of(area, neighbour);
                    OpenRaoMPVariable borderVar = alreadyCreatedBorders.add(pairKey)
                            ? linearProblem.addCounterTradeBorderExchangeVariable(
                            -linearProblem.infinity(), linearProblem.infinity(), area, neighbour, state)
                            : linearProblem.getCounterTradeBorderExchangeVariable(area, neighbour, state);
                    double sign = pairKey.isFromSide(area) ? 1.0 : -1.0;
                    balance.setCoefficient(borderVar, sign);
                }
            }
        });
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // no-op: balance constraint coefficients are fixed, independent of range-action activation
    }
}
