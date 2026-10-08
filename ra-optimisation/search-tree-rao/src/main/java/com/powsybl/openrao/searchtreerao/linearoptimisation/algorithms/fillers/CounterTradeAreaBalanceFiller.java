package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.*;
import com.powsybl.openrao.data.crac.api.Identifiable;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.commons.optimizationperimeters.OptimizationPerimeter;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.LinearProblem;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPConstraint;
import com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem.OpenRaoMPVariable;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;
import com.powsybl.openrao.searchtreerao.result.api.SensitivityResult;

import java.time.OffsetDateTime;
import java.util.*;

import static com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters.getCounterTradeRaSensitivityThreshold;

public class CounterTradeAreaBalanceFiller implements ProblemFiller {

    protected final OptimizationPerimeter optimizationContext;
    protected final Set<FlowCnec> flowCnecs;
    private final Map<State, Set<CounterTradeRangeAction>> counterTradeActions;
    private final AreaGraph areaGraph;
    protected final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    protected final Unit unit;
    private final OffsetDateTime timestamp;

    public CounterTradeAreaBalanceFiller(OptimizationPerimeter optimizationContext,
                                         Map<State, Set<CounterTradeRangeAction>> counterTradeActions,
                                         AreaGraph areaGraph,
                                         SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                         Unit unit,
                                         OffsetDateTime timestamp) {
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(optimizationContext.getFlowCnecs());
        this.counterTradeActions = counterTradeActions;
        this.areaGraph = areaGraph;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult,
                     SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {

        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(flowCnecs, sensitivityResult);
        counterTradeActions.forEach((state, counterTradeRangeActions) -> {
            Set<AreaPairBoundary> alreadyCreatedBorders = new HashSet<>();
            for (CounterTradeRangeAction ra : counterTradeRangeActions) {
                // add the border balance constraints between CT on each area and its borders
                fillBorderBalance(linearProblem, ra, state, alreadyCreatedBorders);
                // add the generation dispatch
                fillGeneratorDispatch(linearProblem, ra, state);
            }
        });
        fillFlowImpact(linearProblem, sensitivityResult, rangeActionActivationResult, validFlowCnecs);
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

    private void fillFlowImpact(LinearProblem linearProblem, SensitivityResult sensitivityResult,
                                RangeActionActivationResult activationResult, Set<FlowCnec> validFlowCnecs) {
        validFlowCnecs.forEach(cnec -> cnec.getMonitoredSides().forEach(side -> {
            OpenRaoMPConstraint flowConstraint = linearProblem.getFlowConstraint(cnec, side, Optional.ofNullable(timestamp));
            List<State> statesBeforeCnec = FillersUtil.getPreviousStates(cnec.getState(), optimizationContext).stream()
                    .sorted((s1, s2) -> Integer.compare(s2.getInstant().getOrder(), s1.getInstant().getOrder())).toList();
            Set<String> alreadyConsideredAreas = new HashSet<>();
            for (State state : statesBeforeCnec) {
                for (CounterTradeRangeAction ra : counterTradeActions.getOrDefault(state, Set.of())) {
                    if (alreadyConsideredAreas.add(ra.getArea())) {
                        CounterTradeGeneratorOrdering ordering = ra.getGeneratorOrdering();
                        double currentSetpoint = activationResult.getOptimizedSetpoint(ra, state);
                        addDirectionImpact(linearProblem, sensitivityResult, ra, state, cnec, side, flowConstraint,
                                ordering.upwardOrder(), LinearProblem.VariationDirectionExtension.UPWARD,
                                1.0, Math.max(currentSetpoint, 0.0));
                        addDirectionImpact(linearProblem, sensitivityResult, ra, state, cnec, side, flowConstraint,
                                ordering.downwardOrder(), LinearProblem.VariationDirectionExtension.DOWNWARD,
                                -1.0, Math.max(-currentSetpoint, 0.0));
                    }
                }
            }
        }));
    }

    private void addDirectionImpact(LinearProblem linearProblem, SensitivityResult sensitivityResult,
                                    CounterTradeRangeAction ra, State state, FlowCnec cnec, TwoSides side,
                                    OpenRaoMPConstraint flowConstraint, List<GlskGeneratorAllocation> order,
                                    LinearProblem.VariationDirectionExtension direction,
                                    double directionSign, double currentMagnitude) {

        double[] sigma = GeneratorOrderingComputer.stageSensitivities(order,
                generatorId -> sensitivityResult.getSensitivityValue(cnec, side, generatorId, unit));
        Map<String, Double> xCurrent = GeneratorOrderingComputer.stageAllocation(order, currentMagnitude);

        for (int h = 0; h < order.size(); h++) {
            double sensitivity = directionSign * sigma[h];
            if (Math.abs(sensitivity) < getCounterTradeRaSensitivityThreshold(rangeActionParametersExtension)) {
                continue;
            }
            String generatorId = order.get(h).generatorId();
            OpenRaoMPVariable x = linearProblem.getCounterTradeGeneratorDispatchVariable(
                    generatorId, ra.getArea(), state, direction);
            double xCur = xCurrent.get(generatorId);

            flowConstraint.setLb(flowConstraint.lb() - sensitivity * xCur);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * xCur);
            flowConstraint.setCoefficient(x, -sensitivity);
        }
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // no-op: balance constraint coefficients are fixed, independent of range-action activation
    }
}
