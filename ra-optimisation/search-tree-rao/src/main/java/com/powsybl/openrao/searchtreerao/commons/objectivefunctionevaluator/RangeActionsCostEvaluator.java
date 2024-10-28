package com.powsybl.openrao.searchtreerao.commons.objectivefunctionevaluator;

import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.searchtreerao.result.api.PrePerimeterResult;
import com.powsybl.openrao.searchtreerao.result.api.RangeActionActivationResult;

public class RangeActionsCostEvaluator implements RemedialActionsCostEvaluator {
    private final RangeActionActivationResult rangeActionActivationResult;
    private final PrePerimeterResult prePerimeterResult;
    private final State state;
    private final double defaultVariationCost;

    public RangeActionsCostEvaluator(RangeActionActivationResult rangeActionActivationResult, PrePerimeterResult prePerimeterResult, State state, double defaultVariationCost) {
        this.rangeActionActivationResult = rangeActionActivationResult;
        this.prePerimeterResult = prePerimeterResult;
        this.state = state;
        this.defaultVariationCost = defaultVariationCost;

    }

    public double getTotalCost() {
        return getTotalActivationCost() + getTotalVariationCost();
    }

    private double getTotalActivationCost() {
        double totalActivationCost = 0;
        for (RangeAction<?> rangeAction : rangeActionActivationResult.getActivatedRangeActions(state)) {
            totalActivationCost += rangeAction.getActivationCost().orElse(0d);
        }
        return totalActivationCost;
    }

    private double getTotalVariationCost() {
        double totalVariationCost = 0;
        for (RangeAction<?> rangeAction : rangeActionActivationResult.getActivatedRangeActions(state)) {
            double prePerimeterSetPoint = prePerimeterResult.getSetpoint(rangeAction);
            double newSetPoint = rangeActionActivationResult.getOptimizedSetpoint(rangeAction, state);
            double absoluteVariation = Math.abs(newSetPoint - prePerimeterSetPoint);
            if (newSetPoint > prePerimeterSetPoint) {
                totalVariationCost += absoluteVariation * rangeAction.getVariationCost(RangeAction.VariationDirection.UP).orElse(defaultVariationCost);
            } else if (newSetPoint < prePerimeterSetPoint) {
                totalVariationCost += absoluteVariation * rangeAction.getVariationCost(RangeAction.VariationDirection.DOWN).orElse(defaultVariationCost);
            }
        }
        return totalVariationCost;
    }
}
