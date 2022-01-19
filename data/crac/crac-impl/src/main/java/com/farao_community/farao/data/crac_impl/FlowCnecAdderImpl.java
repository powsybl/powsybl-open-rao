/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.PhysicalParameter;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdAdder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule.*;
import static java.lang.String.format;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class FlowCnecAdderImpl extends AbstractCnecAdderImpl<FlowCnecAdder> implements FlowCnecAdder {

    private final Set<BranchThresholdImpl> thresholds = new HashSet<>();
    private Double iMaxLeft = Double.NaN;
    private Double iMaxRight = Double.NaN;
    private Double nominalVLeft = Double.NaN;
    private Double nominalVRight = Double.NaN;

    FlowCnecAdderImpl(CracImpl owner) {
        super(owner);
    }

    @Override
    public FlowCnecAdder withIMax(double iMaxInAmpere) {
        this.iMaxLeft = iMaxInAmpere;
        this.iMaxRight = iMaxInAmpere;
        return this;
    }

    @Override
    public FlowCnecAdder withIMax(double iMaxInAmpere, Side side) {
        if (side.equals(Side.LEFT)) {
            this.iMaxLeft = iMaxInAmpere;
        } else if (side.equals(Side.RIGHT)) {
            this.iMaxRight = iMaxInAmpere;
        }
        return this;
    }

    @Override
    public FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt) {
        this.nominalVLeft = nominalVoltageInKiloVolt;
        this.nominalVRight = nominalVoltageInKiloVolt;
        return this;    }

    @Override
    public FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt, Side side) {
        if (side.equals(Side.LEFT)) {
            this.nominalVLeft = nominalVoltageInKiloVolt;
        } else if (side.equals(Side.RIGHT)) {
            this.nominalVRight = nominalVoltageInKiloVolt;
        }
        return this;    }

    @Override
    public BranchThresholdAdder newThreshold() {
        return new BranchThresholdAdderImpl(this);
    }

    void addThreshold(BranchThresholdImpl threshold) {
        thresholds.add(threshold);
    }

    @Override
    protected String getTypeDescription() {
        return "FlowCnec";
    }

    @Override
    public FlowCnec add() {
        checkCnec();

        if (owner.getFlowCnec(id) != null) {
            throw new FaraoException(format("Cannot add a cnec with an already existing ID - %s.", id));
        }

        checkAndInitThresholds();

        State state;
        if (instant != Instant.PREVENTIVE) {
            state = owner.addState(owner.getContingency(contingencyId), instant);
        } else {
            state = owner.addPreventiveState();
        }

        FlowCnec cnec = new FlowCnecImpl(id, name, owner.getNetworkElement(networkElementId), operator, state, optimized, monitored,
            thresholds.stream().map(th -> (BranchThreshold) th).collect(Collectors.toSet()),
            reliabilityMargin, nominalVLeft, nominalVRight, iMaxLeft, iMaxRight);

        owner.addFlowCnec(cnec);
        return cnec;
    }

    /**
     * This method aims to define the side of thresholds when it does not require network so that margins and min/max
     * values can be computed without synchronization.
     * We check first if the threshold is compatible with the cnec physical parameter and then we set the side
     * if it is possible.
     */
    private void checkAndInitThresholds() {

        /*
         This should be done here, and not in BranchThreshold Adder, as some information of the FlowCnec are required
         to perform those checks
         */

        if (this.thresholds.isEmpty()) {
            throw new FaraoException("Cannot add a cnec without a threshold. Please use newThreshold");
        }

        if (this.thresholds.stream().anyMatch(th -> !th.getUnit().getPhysicalParameter().equals(PhysicalParameter.FLOW))) {
            throw new FaraoException("FlowCnec threshold must be in a flow unit (Unit.AMPERE, Unit.MEGAWATT or Unit.PERCENT_IMAX)");
        }

        if (this.thresholds.stream().anyMatch(th -> th.getUnit().equals(Unit.AMPERE) || th.getUnit().equals(Unit.PERCENT_IMAX))) {
            /*
            In the SearchTreeRao, nominal voltages are required in order to handle AMPERE threshold, as:
                - in the sensi in DC, conversion must be made as AMPERE are not returned by the sensi engine
                - in the LP, all values are given in MW, and conversion should somehow be made

            I do not think that nominalVoltage are absolutely required with thresholds in MEGAWATT, but I'm not 100% sure.
             */

            if (Objects.isNull(nominalVLeft) || Objects.isNull(nominalVRight) || Double.isNaN(nominalVLeft) || Double.isNaN(nominalVRight)) {
                throw new FaraoException(String.format("nominal voltages on both side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX or AMPERE. Please use withNominalVoltage()", id));
            }
        }

        for (BranchThresholdImpl branchThreshold : thresholds) {
            checkThresholdRule(branchThreshold);
            checkImax(branchThreshold);
        }
    }

    private void checkThresholdRule(BranchThresholdImpl branchThreshold) {
        switch (branchThreshold.getRule()) {
            case ON_LEFT_SIDE:
            case ON_REGULATED_SIDE:
                // TODO: This is verified only when the network is in UCTE format.
                //  Make it cleaner when we will have to work with other network format and the ON_REGULATED_SIDE rule
                branchThreshold.setSide(Side.LEFT);
                break;
            case ON_RIGHT_SIDE:
            case ON_NON_REGULATED_SIDE:
                // TODO: This is verified only when the network is in UCTE format.
                //  Make it cleaner when we will have to work with other network format and the ON_NON_REGULATED_SIDE rule
                branchThreshold.setSide(Side.RIGHT);
                break;
            case ON_LOW_VOLTAGE_LEVEL:
                if (Objects.isNull(nominalVLeft) || Objects.isNull(nominalVRight) || Double.isNaN(nominalVLeft) || Double.isNaN(nominalVRight)) {
                    throw new FaraoException("ON_LOW_VOLTAGE_LEVEL thresholds can only be defined on FlowCnec whose nominalVoltages have been set on both sides");
                }
                if (nominalVLeft <= nominalVRight) {
                    branchThreshold.setSide(Side.LEFT);
                } else {
                    branchThreshold.setSide(Side.RIGHT);
                }
                break;
            case ON_HIGH_VOLTAGE_LEVEL:
                if (Objects.isNull(nominalVLeft) || Objects.isNull(nominalVRight) || Double.isNaN(nominalVLeft) || Double.isNaN(nominalVRight)) {
                    throw new FaraoException("ON_HIGH_VOLTAGE_LEVEL thresholds can only be defined on FlowCnec whose nominalVoltages have been set on both sides");
                }
                if (nominalVLeft < nominalVRight) {
                    branchThreshold.setSide(Side.RIGHT);
                } else {
                    branchThreshold.setSide(Side.LEFT);
                }
                break;
            default:
                throw new FaraoException(String.format("Rule %s is not yet handled for thresholds on FlowCnec", branchThreshold.getRule()));
        }
    }

    private void checkImax(BranchThresholdImpl branchThreshold) {

        if (branchThreshold.getUnit().equals(Unit.PERCENT_IMAX)
            && branchThreshold.getSide().equals(Side.LEFT)
            && (iMaxLeft == null || Double.isNaN(iMaxLeft))) {
            throw new FaraoException(String.format("iMax on left side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX on the left side. Please use withIMax()", id));
        }

        if (branchThreshold.getUnit().equals(Unit.PERCENT_IMAX)
            && branchThreshold.getSide().equals(Side.RIGHT)
            && (iMaxRight == null || Double.isNaN(iMaxRight))) {
            throw new FaraoException(String.format("iMax on right side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX on the right side. Please use withIMax()", id));
        }
    }
}
