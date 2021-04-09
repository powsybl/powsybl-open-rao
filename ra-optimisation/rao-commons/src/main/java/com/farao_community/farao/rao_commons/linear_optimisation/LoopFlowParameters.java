package com.farao_community.farao.rao_commons.linear_optimisation;

import com.farao_community.farao.rao_api.RaoParameters.LoopFlowApproximationLevel;

public class LoopFlowParameters {
    private final boolean raoWithLoopFlowLimitation;
    private final LoopFlowApproximationLevel loopFlowApproximationLevel;
    private final double loopFlowAcceptableAugmentation;
    private final double loopFlowViolationCost;
    private final double loopFlowConstraintAdjustmentCoefficient;

    public LoopFlowParameters(boolean raoWithLoopFlowLimitation,
                              LoopFlowApproximationLevel loopFlowApproximationLevel,
                              double loopFlowAcceptableAugmentation,
                              double loopFlowViolationCost,
                              double loopFlowConstraintAdjustmentCoefficient) {
        this.raoWithLoopFlowLimitation = raoWithLoopFlowLimitation;
        this.loopFlowApproximationLevel = loopFlowApproximationLevel;
        this.loopFlowAcceptableAugmentation = loopFlowAcceptableAugmentation;
        this.loopFlowViolationCost = loopFlowViolationCost;
        this.loopFlowConstraintAdjustmentCoefficient = loopFlowConstraintAdjustmentCoefficient;
    }

    public boolean isRaoWithLoopFlowLimitation() {
        return raoWithLoopFlowLimitation;
    }

    public LoopFlowApproximationLevel getLoopFlowApproximationLevel() {
        return loopFlowApproximationLevel;
    }

    public double getLoopFlowAcceptableAugmentation() {
        return loopFlowAcceptableAugmentation;
    }

    public double getLoopFlowViolationCost() {
        return loopFlowViolationCost;
    }

    public double getLoopFlowConstraintAdjustmentCoefficient() {
        return loopFlowConstraintAdjustmentCoefficient;
    }
}
