package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

public class RangeActionParameters {

    private final RaoParameters.PstOptimizationApproximation pstOptimizationApproximation;
    private final double pstSensitivityThreshold;
    private final double hvdcSensitivityThreshold;
    private final double injectionSensitivityThreshold;
    private final double pstPenaltyCost;
    private final double hvdcPenaltyCost;
    private final double injectionPenaltyCost;

    public RangeActionParameters(RaoParameters.PstOptimizationApproximation pstOptimizationApproximation,
                                 double pstSensitivityThreshold,
                                 double hvdcSensitivityThreshold,
                                 double injectionSensitivityThreshold,
                                 double pstPenaltyCost,
                                 double hvdcPenaltyCost,
                                 double injectionPenaltyCost) {
        this.pstOptimizationApproximation = pstOptimizationApproximation;
        this.pstSensitivityThreshold = pstSensitivityThreshold;
        this.hvdcSensitivityThreshold = hvdcSensitivityThreshold;
        this.injectionSensitivityThreshold = injectionSensitivityThreshold;
        this.pstPenaltyCost = pstPenaltyCost;
        this.hvdcPenaltyCost = hvdcPenaltyCost;
        this.injectionPenaltyCost = injectionPenaltyCost;
    }

    public RaoParameters.PstOptimizationApproximation getPstOptimizationApproximation() {
        return pstOptimizationApproximation;
    }

    public double getPstSensitivityThreshold() {
        return pstSensitivityThreshold;
    }

    public double getHvdcSensitivityThreshold() {
        return hvdcSensitivityThreshold;
    }

    public double getInjectionSensitivityThreshold() {
        return injectionSensitivityThreshold;
    }

    public double getPstPenaltyCost() {
        return pstPenaltyCost;
    }

    public double getHvdcPenaltyCost() {
        return hvdcPenaltyCost;
    }

    public double getInjectionPenaltyCost() {
        return injectionPenaltyCost;
    }

    public static RangeActionParameters buildFromRaoParameters(RaoParameters raoParameters) {

        /*
        for now, values of RangeActionParameters are constant over all the SearchTreeRao
        they can therefore be instantiated directly from a RaoParameters
         */

        return new RangeActionParameters(raoParameters.getPstOptimizationApproximation(),
            raoParameters.getPstSensitivityThreshold(),
            raoParameters.getHvdcSensitivityThreshold(),
            raoParameters.getInjectionRaSensitivityThreshold(),
            raoParameters.getPstPenaltyCost(),
            raoParameters.getHvdcPenaltyCost(),
            raoParameters.getInjectionRaPenaltyCost());
    }
}
