/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.commons.parameters;

import com.farao_community.farao.rao_api.parameters.RaoParameters;

import java.util.Objects;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
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
        return new RangeActionParameters(raoParameters.getPstOptimizationApproximation(),
            raoParameters.getPstSensitivityThreshold(),
            raoParameters.getHvdcSensitivityThreshold(),
            raoParameters.getInjectionRaSensitivityThreshold(),
            raoParameters.getPstPenaltyCost(),
            raoParameters.getHvdcPenaltyCost(),
            raoParameters.getInjectionRaPenaltyCost());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RangeActionParameters that = (RangeActionParameters) o;
        return Double.compare(that.pstSensitivityThreshold, pstSensitivityThreshold) == 0 && Double.compare(that.hvdcSensitivityThreshold, hvdcSensitivityThreshold) == 0 && Double.compare(that.injectionSensitivityThreshold, injectionSensitivityThreshold) == 0 && Double.compare(that.pstPenaltyCost, pstPenaltyCost) == 0 && Double.compare(that.hvdcPenaltyCost, hvdcPenaltyCost) == 0 && Double.compare(that.injectionPenaltyCost, injectionPenaltyCost) == 0 && pstOptimizationApproximation == that.pstOptimizationApproximation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pstOptimizationApproximation, pstSensitivityThreshold, hvdcSensitivityThreshold, injectionSensitivityThreshold, pstPenaltyCost, hvdcPenaltyCost, injectionPenaltyCost);
    }
}
