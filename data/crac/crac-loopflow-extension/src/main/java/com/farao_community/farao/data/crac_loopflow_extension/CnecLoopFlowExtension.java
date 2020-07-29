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
import com.farao_community.farao.data.crac_api.Cnec;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

/**
 * Cnec extension for loop flow
 *
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class CnecLoopFlowExtension extends AbstractExtension<Cnec> {

    // INPUTS
    // if the unit is PERCENT_IMAX, the input flow threshold should be between 0 and 100.
    private double inputThreshold;
    private Unit inputThresholdUnit;

    // ATTRIBUTES USED BY THE RAO to temporarily store some data about the loop-flows
    private double loopFlowConstraintInMW; // loop-flow upper bound, usually = max (Abs(inputThreshold), Abs(initial loopflow))
    private double loopflowShift; // sum (ptdf * net position)
    private boolean hasLoopflowShift; //has previous calculated loopflow shift value

    public CnecLoopFlowExtension(double inputThreshold, Unit inputThresholdUnit) {
        if (inputThresholdUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new FaraoException("Loop thresholds can only be defined in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        this.inputThreshold = inputThreshold;
        this.inputThresholdUnit = inputThresholdUnit;

        this.loopFlowConstraintInMW = Double.NaN;
        this.loopflowShift = Double.NaN;
        this.hasLoopflowShift = false;
    }

    /**
     * set loop flow constraint used during optimization.
     * The value is equal to MAX value of initial loop flow calculated from network and
     * loop flow threshold which is a input parameter from TSO
     * @param loopFlowConstraint = Max(init_Loop_flow, input loop flow)
     */
    public void setLoopFlowConstraintInMW(double loopFlowConstraint) {
        this.loopFlowConstraintInMW = loopFlowConstraint;
    }

    /**
     * return loop flow constraint used in linear optimization
     */
    public double getLoopFlowConstraintInMW() {
        return loopFlowConstraintInMW;
    }

    public double getLoopflowShift() {
        return loopflowShift;
    }

    public void setLoopflowShift(double loopflowShift) {
        this.loopflowShift = loopflowShift;
        setHasLoopflowShift(true);
    }

    public boolean hasLoopflowShift() {
        return hasLoopflowShift;
    }

    public void setHasLoopflowShift(boolean hasLoopflowShift) {
        this.hasLoopflowShift = hasLoopflowShift;
    }

    public double getInputThreshold(Unit requestedUnit, Network network) {

        if (requestedUnit.getPhysicalParameter() != PhysicalParameter.FLOW) {
            throw new FaraoException("Loop thresholds can only be returned in AMPERE, MEGAWATT or PERCENT_IMAX");
        }

        if (requestedUnit == inputThresholdUnit) {
            return inputThreshold;
        }

        if (inputThresholdUnit == Unit.PERCENT_IMAX && requestedUnit == Unit.AMPERE) {
            return convertPercentImaxToA(network, inputThreshold);
        }

        if (inputThresholdUnit == Unit.PERCENT_IMAX && requestedUnit == Unit.MEGAWATT) {
            return convertAToMW(network, convertPercentImaxToA(network, inputThreshold));
        }

        if (inputThresholdUnit == Unit.AMPERE && requestedUnit == Unit.PERCENT_IMAX) {
            return convertAToPercentImax(network, inputThreshold);
        }

        if (inputThresholdUnit == Unit.AMPERE && requestedUnit == Unit.MEGAWATT) {
            return convertAToMW(network, inputThreshold);
        }

        if (inputThresholdUnit == Unit.MEGAWATT && requestedUnit == Unit.AMPERE) {
            return convertMWToA(network, inputThreshold);
        }

        if (inputThresholdUnit == Unit.MEGAWATT && requestedUnit == Unit.PERCENT_IMAX) {
            return convertAToPercentImax(network, convertMWToA(network, inputThreshold));
        }

        throw new FaraoException(String.format("Cannot convert %s into %s", inputThresholdUnit, requestedUnit));

    }

    private double convertMWToA(Network network, double valueInMW) {
        return valueInMW * 1000 / (getNominalVoltage(network) * Math.sqrt(3));
    }

    private double convertAToMW(Network network, double valueInA) {
        return valueInA * getNominalVoltage(network) * Math.sqrt(3) / 1000;
    }

    private double convertAToPercentImax(Network network, double valueInA) {
        return valueInA * 100 / getImax(network);
    }

    private double convertPercentImaxToA(Network network, double valueInPercent) {
        return valueInPercent * getImax(network) / 100;
    }

    private double getNominalVoltage(Network network) {
        Branch branch = network.getBranch(this.getExtendable().getNetworkElement().getId());
        if (branch == null) {
            throw new FaraoException(String.format("Cnec with id %s was not found in the network", this.getExtendable().getId()));
        }
        return branch.getTerminal1().getVoltageLevel().getNominalV();
    }

    private double getImax(Network network) {
        Branch branch = network.getBranch(this.getExtendable().getNetworkElement().getId());
        if (branch == null) {
            throw new FaraoException(String.format("Cnec with id %s was not found in the network", this.getExtendable().getId()));
        }
        return branch.getCurrentLimits1().getPermanentLimit();
    }

    @Override
    public String getName() {
        return "CnecLoopFlowExtension";
    }
}
