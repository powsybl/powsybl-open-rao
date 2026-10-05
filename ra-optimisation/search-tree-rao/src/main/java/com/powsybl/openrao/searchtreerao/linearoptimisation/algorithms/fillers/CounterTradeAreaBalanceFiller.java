package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.openrao.commons.AreaGraph;
import com.powsybl.openrao.commons.AreaPairBoundary;
import com.powsybl.openrao.commons.CounterTradeGeneratorOrdering;
import com.powsybl.openrao.commons.GlskGeneratorAllocation;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.util.HashSet;
import java.util.List;
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
                // add the border balance constraints between CT on each area and its borders
                fillBorderBalance(linearProblem, ra, state, alreadyCreatedBorders);
                // add the generation dispatch
                fillGeneratorDispatch(linearProblem, ra, state);
            }
        });
    }

    private void fillBorderBalance(LinearProblem linearProblem, CounterTradeRangeAction ra, State state,
                                   Set<AreaPairBoundary> alreadyCreatedBorders) {
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


    // positive and negative parts, variables and constraints
    private void fillGeneratorDispatch(LinearProblem linearProblem, CounterTradeRangeAction ra, State state) {
        CounterTradeGeneratorOrdering ordering = ra.getGeneratorOrdering();
        OpenRaoMPVariable ctVar = linearProblem.getRangeActionSetpointVariable(ra, state);
        String areaId = ra.getArea();
        OpenRaoMPVariable delta = linearProblem.addCounterTradeDirectionBinary(areaId, state);

        // ctVar - sum(x_up) + sum(x_down) = 0
        OpenRaoMPConstraint decomposition = linearProblem.addCounterTradeSetpointDecompositionConstraint(areaId, state);
        decomposition.setCoefficient(ctVar, 1.0);

        addDirectionalDispatch(linearProblem, areaId, state, ordering.upwardOrder(),
                LinearProblem.VariationDirectionExtension.UPWARD, decomposition, delta);
        addDirectionalDispatch(linearProblem, areaId, state, ordering.downwardOrder(),
                LinearProblem.VariationDirectionExtension.DOWNWARD, decomposition, delta);
    }

    private void addDirectionalDispatch(LinearProblem linearProblem, String areaId, State state,
                                        List<GlskGeneratorAllocation> allocations,
                                        LinearProblem.VariationDirectionExtension direction,
                                        OpenRaoMPConstraint decomposition, OpenRaoMPVariable delta) {

        boolean up = direction == LinearProblem.VariationDirectionExtension.UPWARD;
        double totalM = allocations.stream().mapToDouble(GlskGeneratorAllocation::cumulativeAllocation).sum();

        // UP:   sum(x_up)   - M_up * delta       <= 0
        // DOWN: sum(x_down) + M_down * delta     <= M_down
        OpenRaoMPConstraint exclusivity = linearProblem.addCounterTradeExclusivityConstraint(
                up ? 0.0 : totalM, areaId, state, direction);
        exclusivity.setCoefficient(delta, up ? -totalM : totalM);

        OpenRaoMPVariable previousY = null;

        for (GlskGeneratorAllocation allocation : allocations) {
            String generatorId = allocation.generatorId();
            double mh = allocation.cumulativeAllocation();

            OpenRaoMPVariable x = linearProblem.addCounterTradeGeneratorDispatchVariable(mh, generatorId, areaId, state, direction);
            OpenRaoMPVariable y = linearProblem.addCounterTradeGeneratorSaturationBinary(generatorId, areaId, state, direction);

            // x - m_h * y >= 0
            OpenRaoMPConstraint link = linearProblem.addCounterTradeGeneratorSaturationLinkConstraint(generatorId, areaId, state, direction);

            link.setCoefficient(x, 1.0);
            link.setCoefficient(y, -mh);

            // x - m_h y_{h-1} <=0
            if (previousY != null) {
                OpenRaoMPConstraint order = linearProblem.addCounterTradeGeneratorOrderConstraint(generatorId, areaId, state, direction);
                order.setCoefficient(x, 1.0);
                order.setCoefficient(previousY, -mh);
            }
            previousY = y;

            exclusivity.setCoefficient(x, 1.0);
            decomposition.setCoefficient(x, up ? -1.0 : 1.0);
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // no-op: balance constraint coefficients are fixed, independent of range-action activation
    }
}
