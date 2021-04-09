package com.farao_community.farao.rao_commons.linear_optimisation;

public class MnecParameters {
    private final double mnecAcceptableMarginDiminution;
    private final double mnecViolationCost;
    private final double mnecConstraintAdjustmentCoefficient;

    public MnecParameters(double mnecAcceptableMarginDiminution, double mnecViolationCost, double mnecConstraintAdjustmentCoefficient) {
        this.mnecAcceptableMarginDiminution = mnecAcceptableMarginDiminution;
        this.mnecViolationCost = mnecViolationCost;
        this.mnecConstraintAdjustmentCoefficient = mnecConstraintAdjustmentCoefficient;
    }

    public double getMnecAcceptableMarginDiminution() {
        return mnecAcceptableMarginDiminution;
    }

    public double getMnecViolationCost() {
        return mnecViolationCost;
    }

    public double getMnecConstraintAdjustmentCoefficient() {
        return mnecConstraintAdjustmentCoefficient;
    }
}
