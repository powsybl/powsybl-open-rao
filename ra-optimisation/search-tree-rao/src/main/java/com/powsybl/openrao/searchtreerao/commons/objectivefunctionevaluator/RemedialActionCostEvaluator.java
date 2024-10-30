package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.searchtreerao.result.api.FlowResult;
import com.powsybl.openrao.searchtreerao.result.api.RemedialActionActivationResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemedialActionCostEvaluator implements CostEvaluator {
    private final State state;
    private final Set<FlowCnec> flowCnecs;
    private final Unit unit;
    private final MarginEvaluator marginEvaluator;
    private final RangeActionsOptimizationParameters rangeActionsOptimizationParameters;
    private static final double OVERLOAD_PENALTY = 10000d; // TODO : set this in RAO parameters

    public RemedialActionCostEvaluator(State state, Set<FlowCnec> flowCnecs, Unit unit, MarginEvaluator marginEvaluator, RangeActionsOptimizationParameters rangeActionsOptimizationParameters) {
        this.state = state;
        this.flowCnecs = flowCnecs;
        this.unit = unit;
        this.marginEvaluator = marginEvaluator;
        this.rangeActionsOptimizationParameters = rangeActionsOptimizationParameters;
    }

    @Override
    public String getName() {
        return "remedial-action-cost-evaluator";
    }

    @Override
    public Pair<Double, List<FlowCnec>> computeCostAndLimitingElements(FlowResult flowResult, RemedialActionActivationResult remedialActionActivationResult, Set<String> contingenciesToExclude) {
        double totalRemedialActionsCost = getTotalNetworkActionsCost(remedialActionActivationResult) + getTotalRangeActionsCost(remedialActionActivationResult);

        List<FlowCnec> limitingElements = getCostlyElements(flowResult, contingenciesToExclude);
        FlowCnec limitingElement = limitingElements.isEmpty() ? null : limitingElements.get(0);
        double margin = marginEvaluator.getMargin(flowResult, limitingElement, unit);

        double cost = margin >= 0 ? totalRemedialActionsCost : totalRemedialActionsCost - OVERLOAD_PENALTY * margin;
        return Pair.of(cost, limitingElements);
    }

    private List<FlowCnec> getCostlyElements(FlowResult flowResult, Set<String> contingenciesToExclude) {
        Map<FlowCnec, Double> margins = new HashMap<>();

        flowCnecs.stream()
            .filter(cnec -> cnec.getState().getContingency().isEmpty() || !contingenciesToExclude.contains(cnec.getState().getContingency().get().getId()))
            .filter(Cnec::isOptimized)
            .forEach(flowCnec -> margins.put(flowCnec, marginEvaluator.getMargin(flowResult, flowCnec, unit)));

        return margins.keySet().stream()
            .filter(Cnec::isOptimized)
            .sorted(Comparator.comparing(margins::get))
            .toList();
    }

    private double getTotalNetworkActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        double totalNetworkActionsCost = 0d;
        for (NetworkAction networkAction : remedialActionActivationResult.getActivatedNetworkActions()) {
            totalNetworkActionsCost += networkAction.getActivationCost().orElse(0d);
        }
        return totalNetworkActionsCost;
    }

    private double getTotalRangeActionsCost(RemedialActionActivationResult remedialActionActivationResult) {
        double totalRangeActionsCost = 0d;
        for (RangeAction<?> rangeAction : remedialActionActivationResult.getActivatedRangeActions(state)) {
            totalRangeActionsCost += rangeAction.getActivationCost().orElse(0d);
            if (rangeAction instanceof PstRangeAction) {
                totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getPstPenaltyCost(), remedialActionActivationResult);
            } else if (rangeAction instanceof InjectionRangeAction) {
                totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getInjectionRaPenaltyCost(), remedialActionActivationResult);
            } else if (rangeAction instanceof HvdcRangeAction) {
                totalRangeActionsCost += computeVariationCost(rangeAction, rangeActionsOptimizationParameters.getHvdcPenaltyCost(), remedialActionActivationResult);
            } else {
                // TODO: add penalty for CT
                totalRangeActionsCost += computeVariationCost(rangeAction, 0d, remedialActionActivationResult);
            }
        }
        return totalRangeActionsCost;
    }

    private double computeVariationCost(RangeAction<?> rangeAction, double defaultCost, RemedialActionActivationResult remedialActionActivationResult) {
        double variation = rangeAction instanceof PstRangeAction pstRangeAction ? (double) remedialActionActivationResult.getTapVariation(pstRangeAction, state) : remedialActionActivationResult.getSetPointVariation(rangeAction, state);
        RangeAction.VariationDirection variationDirection = variation > 0 ? RangeAction.VariationDirection.UP : RangeAction.VariationDirection.DOWN;
        return Math.abs(variation) * rangeAction.getVariationCost(variationDirection).orElse(defaultCost);
    }

    @Override
    public Unit getUnit() {
        return unit;
    }

    @Override
    public Set<FlowCnec> getFlowCnecs() {
        return flowCnecs;
    }
}
