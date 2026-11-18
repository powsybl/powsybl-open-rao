/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.fillers;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.*;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
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

/**
 * @author Enrico Bettiol {@literal <enrico.bettiol at artelys.com>}
 */
public class CounterTradeAreaBalanceFiller implements ProblemFiller {

    protected final OptimizationPerimeter optimizationContext;
    protected final Set<FlowCnec> flowCnecs;
    private final Map<State, Set<CounterTradeRangeAction>> counterTradeActions;
    protected final SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension;
    protected final Unit unit;
    private final OffsetDateTime timestamp;

    public CounterTradeAreaBalanceFiller(OptimizationPerimeter optimizationContext,
                                         Map<State, Set<CounterTradeRangeAction>> counterTradeActions,
                                         SearchTreeRaoRangeActionsOptimizationParameters rangeActionParametersExtension,
                                         Unit unit,
                                         OffsetDateTime timestamp) {
        this.optimizationContext = optimizationContext;
        this.flowCnecs = new TreeSet<>(Comparator.comparing(Identifiable::getId));
        this.flowCnecs.addAll(optimizationContext.getFlowCnecs());
        this.counterTradeActions = counterTradeActions;
        this.rangeActionParametersExtension = rangeActionParametersExtension;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    @Override
    public void fill(LinearProblem linearProblem, FlowResult flowResult,
                     SensitivityResult sensitivityResult, RangeActionActivationResult rangeActionActivationResult) {

        Set<FlowCnec> validFlowCnecs = FillersUtil.getFlowCnecsComputationStatusOk(flowCnecs, sensitivityResult);
        counterTradeActions.forEach((state, counterTradeRangeActions) -> {

            Map<String, CounterTradeRangeAction> counterTradeActionsByArea = new HashMap<>();
            for (CounterTradeRangeAction ra : counterTradeRangeActions) {
                if (counterTradeActionsByArea.put(ra.getAreaId(), ra) != null) {
                    throw new OpenRaoException("Several counter-trade range actions on area " + ra.getAreaId() + " in state " + state.getId());
                }
            }
            checkConnectedAreasAreSymmetric(counterTradeActionsByArea);

            Set<String> alreadyCreatedBorders = new HashSet<>();

            for (CounterTradeRangeAction ra : counterTradeRangeActions) {
                // add the border balance constraints between CT on each area and its borders
                fillBorderBalance(linearProblem, ra, state, counterTradeActionsByArea, alreadyCreatedBorders);
                // add the generation dispatch
                fillGeneratorDispatch(linearProblem, ra, state);
            }
        });
        fillFlowImpact(linearProblem, sensitivityResult, rangeActionActivationResult, validFlowCnecs);
    }

    private void fillBorderBalance(LinearProblem linearProblem, CounterTradeRangeAction ra, State state,
                                   Map<String, CounterTradeRangeAction> counterTradeActionsByArea,
                                   Set<String> alreadyCreatedBorders) {
        String area = ra.getAreaId();
        OpenRaoMPVariable ctVar = linearProblem.getRangeActionSetpointVariable(ra, state);
        OpenRaoMPConstraint balance = linearProblem.addCounterTradeAreaBalanceConstraint(ra, state);
        balance.setCoefficient(ctVar, -1.0);

        boolean hasBorder = false;
        for (ConnectedArea connected : ra.getConnectedAreas()) {
            String neighbour = connected.getAreaId();
            if (!counterTradeActionsByArea.containsKey(neighbour)) {
                continue; // no CT on the other side: border fixed to 0, no variable
            }
            OpenRaoMPVariable borderVar = getOrCreateBorderVariable(linearProblem, area, neighbour, state, alreadyCreatedBorders, counterTradeActionsByArea);
            balance.setCoefficient(borderVar, borderSign(area, neighbour));
            hasBorder = true;
        }
        if (!hasBorder) {
            OpenRaoLoggerProvider.TECHNICAL_LOGS.warn(
                    "Counter-trade area {} has no connected area with a counter-trade action: its setpoint is forced to 0", area);
        }

    }

    // positive and negative parts, variables and constraints
    private void fillGeneratorDispatch(LinearProblem linearProblem, CounterTradeRangeAction ra, State state) {
        CounterTradeGeneratorOrdering ordering = ra.getGeneratorOrdering();
        OpenRaoMPVariable ctVar = linearProblem.getRangeActionSetpointVariable(ra, state);
        String areaId = ra.getAreaId();
        OpenRaoMPVariable delta = linearProblem.addCounterTradeDirectionBinary(areaId, state);

        // ctVar - sum(x_up) + sum(x_down) = 0
        OpenRaoMPConstraint decomposition = linearProblem.addCounterTradeSetpointDecompositionConstraint(areaId, state);
        decomposition.setCoefficient(ctVar, 1.0);

        addDirectionalDispatch(linearProblem, areaId, state, ordering.upwardOrder(), ordering.upwardLastStage(), ordering.maxUpwardMagnitude(),
                LinearProblem.VariationDirectionExtension.UPWARD, decomposition, delta);
        addDirectionalDispatch(linearProblem, areaId, state, ordering.downwardOrder(), ordering.downwardLastStage(), ordering.maxDownwardMagnitude(),
                LinearProblem.VariationDirectionExtension.DOWNWARD, decomposition, delta);
    }

    private void addDirectionalDispatch(LinearProblem linearProblem, String areaId, State state,
                                        List<GlskGeneratorAllocation> order, int lastStage, double maxMagnitude,
                                        LinearProblem.VariationDirectionExtension direction,
                                        OpenRaoMPConstraint decomposition, OpenRaoMPVariable delta) {

        if (order.isEmpty()) {
            // no generator available in the area for this direction: the CT action is pinned to 0 by the decomposition constraint
            return;
        }

        boolean up = direction == LinearProblem.VariationDirectionExtension.UPWARD;
        double totalM = 0.0; // sum of m_h over the stages that can be reached
        for (int h = 0; h <= lastStage; h++) {
            totalM += order.get(h).cumulativeAllocation();
        }
        // UP:   sum(x_up)   - M_up * delta       <= 0
        // DOWN: sum(x_down) + M_down * delta     <= M_down
        OpenRaoMPConstraint exclusivity = linearProblem.addCounterTradeExclusivityConstraint(
                up ? 0.0 : totalM, areaId, state, direction);
        exclusivity.setCoefficient(delta, up ? -totalM : totalM);

        OpenRaoMPVariable previousY = null;

        for (int h = 0; h <= lastStage; h++) {
            GlskGeneratorAllocation allocation = order.get(h);
            String generatorId = allocation.generatorId();
            double bound = Math.min(allocation.cumulativeAllocation(), maxMagnitude);

            OpenRaoMPVariable x = linearProblem.addCounterTradeGeneratorDispatchVariable(bound, generatorId, h, areaId, state, direction);

            exclusivity.setCoefficient(x, 1.0);
            decomposition.setCoefficient(x, up ? -1.0 : 1.0);

            // x - m_h y_{h-1} <=0
            if (previousY != null) {
                OpenRaoMPConstraint orderCtr = linearProblem.addCounterTradeGeneratorOrderConstraint(generatorId, h, areaId, state, direction);
                orderCtr.setCoefficient(x, 1.0);
                orderCtr.setCoefficient(previousY, -bound);
            }
            if (h < lastStage) {
                OpenRaoMPVariable y = linearProblem.addCounterTradeGeneratorSaturationBinary(generatorId, h, areaId, state, direction);
                // x - m_h * y >= 0
                OpenRaoMPConstraint link = linearProblem.addCounterTradeGeneratorSaturationLinkConstraint(generatorId, h, areaId, state, direction);
                link.setCoefficient(x, 1.0);
                link.setCoefficient(y, -bound);
                // y - y_{h-1} <=0
                if (previousY != null) {
                    OpenRaoMPConstraint monotonic = linearProblem.addCounterTradeSaturationOrderConstraint(generatorId, h, areaId, state, direction);
                    monotonic.setCoefficient(y, 1.0);
                    monotonic.setCoefficient(previousY, -1.0);
                }
                previousY = y;
            }

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
                    if (alreadyConsideredAreas.add(ra.getAreaId())) {
                        CounterTradeGeneratorOrdering ordering = ra.getGeneratorOrdering();
                        double currentSetpoint = activationResult.getOptimizedSetpoint(ra, state);
                        addDirectionImpact(linearProblem, sensitivityResult, ra, state, cnec, side, flowConstraint,
                                ordering.upwardOrder(), ordering.upwardLastStage(), LinearProblem.VariationDirectionExtension.UPWARD,
                                1.0, Math.max(currentSetpoint, 0.0));
                        addDirectionImpact(linearProblem, sensitivityResult, ra, state, cnec, side, flowConstraint,
                                ordering.downwardOrder(), ordering.downwardLastStage(), LinearProblem.VariationDirectionExtension.DOWNWARD,
                                -1.0, Math.max(-currentSetpoint, 0.0));
                    }
                }
            }
        }));
    }

    private void addDirectionImpact(LinearProblem linearProblem, SensitivityResult sensitivityResult,
                                    CounterTradeRangeAction ra, State state, FlowCnec cnec, TwoSides side,
                                    OpenRaoMPConstraint flowConstraint, List<GlskGeneratorAllocation> order, int lastStage,
                                    LinearProblem.VariationDirectionExtension direction,
                                    double directionSign, double currentMagnitude) {

        if (order.isEmpty()) {
            // no generator available in the area for this direction: no dispatch variable exists to carry a flow impact
            return;
        }

        double[] sigma;
        if (ra.getGeneratorOrdering().meritOrder()) {
            sigma = GeneratorOrderingComputer.meritOrderSensitivities(order, lastStage,
                    generatorId -> sensitivityResult.getSensitivityValue(cnec, side, generatorId, unit));
        } else {
            String tailId = "counterTrade_" + ra.getId() + "_tail_" + direction.name(); // use a method to get the correct name?
            double tailWeight = 0.0;
            for (int i = lastStage; i < order.size(); i++) {
                tailWeight += order.get(i).weight();
            }
            double tailSensitivity = tailWeight * sensitivityResult.getSensitivityValue(cnec, side, tailId, unit);

            sigma = GeneratorOrderingComputer.stageSensitivities(order, lastStage,
                    generatorId -> sensitivityResult.getSensitivityValue(cnec, side, generatorId, unit),
                    tailSensitivity);
        }
        double[] xCurrent = GeneratorOrderingComputer.layerAllocation(order, currentMagnitude);

        for (int h = 0; h <= lastStage; h++) {
            double sensitivity = directionSign * sigma[h];
            if (Math.abs(sensitivity) < getCounterTradeRaSensitivityThreshold(rangeActionParametersExtension)) {
                continue;
            }
            String generatorId = order.get(h).generatorId();
            OpenRaoMPVariable x = linearProblem.getCounterTradeGeneratorDispatchVariable(
                    generatorId, h, ra.getAreaId(), state, direction);
            double xCur = xCurrent[h];

            flowConstraint.setLb(flowConstraint.lb() - sensitivity * xCur);
            flowConstraint.setUb(flowConstraint.ub() - sensitivity * xCur);
            flowConstraint.setCoefficient(x, -sensitivity);
        }
    }

    private OpenRaoMPVariable getOrCreateBorderVariable(LinearProblem linearProblem, String areaId, String neighbourId,
                                                        State state, Set<String> alreadyCreatedBorders,
                                                        Map<String, CounterTradeRangeAction> counterTradeActionsByArea) {
        if (!alreadyCreatedBorders.add(borderKey(areaId, neighbourId))) {
            return linearProblem.getCounterTradeBorderExchangeVariable(areaId, neighbourId, state);
        }
        // the exchange cannot exceed what the receiving side is allowed to increase
        String fromArea = borderSign(areaId, neighbourId) > 0 ? areaId : neighbourId;
        String toArea = fromArea.equals(areaId) ? neighbourId : areaId;
        double ub = Math.max(0, counterTradeActionsByArea.get(fromArea).getMaxAdmissibleSetpoint(0));
        double lb = -Math.max(0, counterTradeActionsByArea.get(toArea).getMaxAdmissibleSetpoint(0));
        return linearProblem.addCounterTradeBorderExchangeVariable(lb, ub, areaId, neighbourId, state);
    }

    private static String borderKey(String areaId, String neighbourId) {
        return areaId.compareTo(neighbourId) < 0 ? areaId + "_" + neighbourId : neighbourId + "_" + areaId;
    }

    private static double borderSign(String areaId, String neighbourId) {
        return areaId.compareTo(neighbourId) < 0 ? 1.0 : -1.0;
    }

    private void checkConnectedAreasAreSymmetric(Map<String, CounterTradeRangeAction> counterTradeActions) {
        counterTradeActions.forEach((area, ra) -> {
            for (ConnectedArea connected : ra.getConnectedAreas()) {
                CounterTradeRangeAction other = counterTradeActions.get(connected.getAreaId());
                if (other != null && other.getConnectedAreas().stream().noneMatch(c -> c.getAreaId().equals(area))) {
                    throw new OpenRaoException(String.format(
                            "Area %s lists %s as connected area, but %s does not list %s", area, connected.getAreaId(), connected.getAreaId(), area));
                }
            }
        });
    }

    @Override
    public void updateBetweenMipIteration(LinearProblem linearProblem, RangeActionActivationResult rangeActionActivationResult) {
        // no-op: balance constraint coefficients are fixed, independent of range-action activation
    }
}
