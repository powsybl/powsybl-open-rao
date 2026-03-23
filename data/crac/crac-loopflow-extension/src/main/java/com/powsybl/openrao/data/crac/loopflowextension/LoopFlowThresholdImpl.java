/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.loopflowextension;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;

/**
 * Cnec extension for loop flow
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class LoopFlowThresholdImpl extends AbstractExtension<FlowCnec> implements LoopFlowThreshold {

    /*
     - if the unit is PERCENT_IMAX, the input flow threshold should be between 0 and 1
     - in the loop-flow threshold, PERCENT_IMAX is considered as a percentage of the Cnec
       threshold (retrieved from the Crac, without considering the frm), and NOT as a percentage
        of the branch current limit (retrieved from the Network)
     */
    private double inputThreshold;
    private Unit inputThresholdUnit;

    LoopFlowThresholdImpl(double value, Unit unit) {
        this.inputThreshold = value;
        this.inputThresholdUnit = unit;
    }

    @Override
    public double getValue() {
        return inputThreshold;
    }

    @Override
    public Unit getUnit() {
        return inputThresholdUnit;
    }

    @Override
    public double getThresholdWithReliabilityMargin(Unit requestedUnit) {
        switch (requestedUnit) {
            case MEGAWATT:
                return getThreshold(requestedUnit) - this.getExtendable().getReliabilityMargin();
            case AMPERE:
                return getThreshold(requestedUnit) - convertMWToA(this.getExtendable().getReliabilityMargin());
            case PERCENT_IMAX:
                return getThreshold(requestedUnit) - convertAToPercentImax(convertMWToA(this.getExtendable().getReliabilityMargin()));
            default:
                throw new OpenRaoException("Loopflow thresholds can only be returned in AMPERE, MEGAWATT or PERCENT_IMAX");
        }
    }

    @Override
    public double getThreshold(Unit requestedUnit) {

        if (requestedUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new OpenRaoException("Loopflow thresholds can only be returned in AMPERE, MEGAWATT or PERCENT_IMAX");
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

        throw new OpenRaoException(String.format("Cannot convert %s into %s", inputThresholdUnit, requestedUnit));
    }

    private double convertMWToA(double valueInMW) {
        return valueInMW * 1000 / (getExtendable().getNominalVoltage(TwoSides.ONE) * Math.sqrt(3));
    }

    private double convertAToMW(double valueInA) {
        return valueInA * getExtendable().getNominalVoltage(TwoSides.ONE) * Math.sqrt(3) / 1000;
    }

    private double convertAToPercentImax(double valueInA) {
        return valueInA / getCnecFmaxWithoutFrmInA();
    }

    private double convertPercentImaxToA(double valueInPercent) {
        return valueInPercent * getCnecFmaxWithoutFrmInA();
    }

    private double getCnecFmaxWithoutFrmInA() {
        double minUpperBound = Math.min(
            getExtendable().getUpperBound(TwoSides.ONE, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY),
            getExtendable().getUpperBound(TwoSides.TWO, Unit.AMPERE).orElse(Double.POSITIVE_INFINITY)
        );
        double maxLowerBound = Math.max(
            getExtendable().getLowerBound(TwoSides.ONE, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY),
            getExtendable().getLowerBound(TwoSides.TWO, Unit.AMPERE).orElse(Double.NEGATIVE_INFINITY)
        );
        return Math.min(minUpperBound, -maxLowerBound) + convertMWToA(getExtendable().getReliabilityMargin());
    }
}
