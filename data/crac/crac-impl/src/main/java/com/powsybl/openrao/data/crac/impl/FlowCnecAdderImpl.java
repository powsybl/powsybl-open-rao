/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;
import com.powsybl.openrao.data.crac.api.threshold.BranchThresholdAdder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public FlowCnecAdder withNetworkElement(String networkElementId, String networkElementName) {
        if (!this.networkElementsIdAndName.entrySet().isEmpty()) {
            throw new OpenRaoException("Cannot add multiple network elements for a flow cnec.");
        }
        super.withNetworkElement(networkElementId, networkElementName);
        return this;
    }

    @Override
    public FlowCnecAdder withIMax(double iMaxInAmpere) {
        this.iMaxLeft = iMaxInAmpere;
        this.iMaxRight = iMaxInAmpere;
        return this;
    }

    @Override
    public FlowCnecAdder withIMax(double iMaxInAmpere, TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            this.iMaxLeft = iMaxInAmpere;
        } else if (side.equals(TwoSides.TWO)) {
            this.iMaxRight = iMaxInAmpere;
        }
        return this;
    }

    @Override
    public FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt) {
        this.nominalVLeft = nominalVoltageInKiloVolt;
        this.nominalVRight = nominalVoltageInKiloVolt;
        return this;
    }

    @Override
    public FlowCnecAdder withNominalVoltage(double nominalVoltageInKiloVolt, TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            this.nominalVLeft = nominalVoltageInKiloVolt;
        } else if (side.equals(TwoSides.TWO)) {
            this.nominalVRight = nominalVoltageInKiloVolt;
        }
        return this;
    }

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
        checkAndInitThresholds();
        State state = getState();

        FlowCnec cnec = new FlowCnecImpl(id, name, owner.getNetworkElement(networkElementsIdAndName.keySet().iterator().next()), operator, border, state, optimized, monitored,
            thresholds.stream().map(BranchThreshold.class::cast).collect(Collectors.toSet()),
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
            throw new OpenRaoException("Cannot add a cnec without a threshold. Please use newThreshold");
        }

        if (this.thresholds.stream().anyMatch(th -> !th.getUnit().getPhysicalParameter().equals(PhysicalParameter.FLOW))) {
            throw new OpenRaoException("FlowCnec threshold must be in a flow unit (Unit.AMPERE, Unit.MEGAWATT or Unit.PERCENT_IMAX)");
        }

        if ((this.thresholds.stream().anyMatch(th -> th.getUnit().equals(Unit.AMPERE) || th.getUnit().equals(Unit.PERCENT_IMAX)))
                && (Objects.isNull(nominalVLeft) || Objects.isNull(nominalVRight) || Double.isNaN(nominalVLeft) || Double.isNaN(nominalVRight))) {
            /*
            In the SearchTreeRao, nominal voltages are required in order to handle AMPERE threshold, as:
                - in the sensi in DC, conversion must be made as AMPERE are not returned by the sensi engine
                - in the LP, all values are given in MW, and conversion should somehow be made

            I do not think that nominalVoltage are absolutely required with thresholds in MEGAWATT, but I'm not 100% sure.
             */
            throw new OpenRaoException(String.format("nominal voltages on both side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX or AMPERE. Please use withNominalVoltage()", id));
        }

        for (BranchThresholdImpl branchThreshold : thresholds) {
            checkImax(branchThreshold);
        }
    }

    private void checkImax(BranchThresholdImpl branchThreshold) {

        if (branchThreshold.getUnit().equals(Unit.PERCENT_IMAX)
            && branchThreshold.getSide().equals(TwoSides.ONE)
            && (iMaxLeft == null || Double.isNaN(iMaxLeft))) {
            throw new OpenRaoException(String.format("iMax on left side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX on the left side. Please use withIMax()", id));
        }

        if (branchThreshold.getUnit().equals(Unit.PERCENT_IMAX)
            && branchThreshold.getSide().equals(TwoSides.TWO)
            && (iMaxRight == null || Double.isNaN(iMaxRight))) {
            throw new OpenRaoException(String.format("iMax on right side of FlowCnec %s must be defined, as one of its threshold is on PERCENT_IMAX on the right side. Please use withIMax()", id));
        }
    }
}
