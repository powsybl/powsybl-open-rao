/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_loopflow_extension;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Side;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.powsybl.commons.extensions.AbstractExtension;

/**
 * Cnec extension for loop flow
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class CnecLoopFlowExtension extends AbstractExtension<BranchCnec> {

    /*
     - if the unit is PERCENT_IMAX, the input flow threshold should be between 0 and 100
     - in the loop-flow threshold, PERCENT_IMAX is considered as a percentage of the Cnec
       threshold (retrieved from the Crac, without considering the frm), and NOT as a percentage
        of the branch current limit (retrieved from the Network)
     */
    private double inputThreshold;
    private Unit inputThresholdUnit;

    public CnecLoopFlowExtension(double inputThreshold, Unit inputThresholdUnit) {
        if (inputThresholdUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new FaraoException("Loopflow thresholds can only be defined in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        this.inputThreshold = inputThreshold;
        this.inputThresholdUnit = inputThresholdUnit;
    }

    public double getInputThreshold() {
        return inputThreshold;
    }

    public Unit getInputThresholdUnit() {
        return inputThresholdUnit;
    }

    public double getThresholdWithReliabilityMargin(Unit requestedUnit) {
        switch (requestedUnit) {
            case MEGAWATT:
                return getInputThreshold(requestedUnit) - this.getExtendable().getReliabilityMargin();
            case AMPERE:
                return getInputThreshold(requestedUnit) - convertMWToA(this.getExtendable().getReliabilityMargin());
            case PERCENT_IMAX:
                return getInputThreshold(requestedUnit) - convertAToPercentImax(convertMWToA(this.getExtendable().getReliabilityMargin()));
            default:
                throw new FaraoException("Loopflow thresholds can only be returned in AMPERE, MEGAWATT or PERCENT_IMAX");
        }
    }

    public double getInputThreshold(Unit requestedUnit) {

        if (requestedUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new FaraoException("Loopflow thresholds can only be returned in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        if (requestedUnit == inputThresholdUnit) {
            return inputThreshold;
        }

        if (inputThresholdUnit == Unit.PERCENT_IMAX && requestedUnit == Unit.AMPERE) {
            return convertPercentImaxToA(inputThreshold);
        }

        if (inputThresholdUnit == Unit.PERCENT_IMAX && requestedUnit == Unit.MEGAWATT) {
            return convertAToMW(convertPercentImaxToA(inputThreshold));
        }

        if (inputThresholdUnit == Unit.AMPERE && requestedUnit == Unit.PERCENT_IMAX) {
            return convertAToPercentImax(inputThreshold);
        }

        if (inputThresholdUnit == Unit.AMPERE && requestedUnit == Unit.MEGAWATT) {
            return convertAToMW(inputThreshold);
        }

        if (inputThresholdUnit == Unit.MEGAWATT && requestedUnit == Unit.AMPERE) {
            return convertMWToA(inputThreshold);
        }

        if (inputThresholdUnit == Unit.MEGAWATT && requestedUnit == Unit.PERCENT_IMAX) {
            return convertAToPercentImax(convertMWToA(inputThreshold));
        }

        throw new FaraoException(String.format("Cannot convert %s into %s", inputThresholdUnit, requestedUnit));
    }

    @Override
    public String getName() {
        return "CnecLoopFlowExtension";
    }

    private double convertMWToA(double valueInMW) {
        return valueInMW * 1000 / (getExtendable().getNominalVoltage(Side.LEFT) * Math.sqrt(3));
    }

    private double convertAToMW(double valueInA) {
        return valueInA * getExtendable().getNominalVoltage(Side.LEFT) * Math.sqrt(3) / 1000;
    }

    private double convertAToPercentImax(double valueInA) {
        return valueInA * 100 / getCnecFmaxWithoutFrmInA();
    }

    private double convertPercentImaxToA(double valueInPercent) {
        return valueInPercent * getCnecFmaxWithoutFrmInA() / 100;
    }

    private double getCnecFmaxWithoutFrmInA() {
        return Math.min(
                getExtendable().getUpperBound(Side.LEFT, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY),
                -getExtendable().getLowerBound(Side.LEFT, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY))
                + convertMWToA(getExtendable().getReliabilityMargin());
    }
}
