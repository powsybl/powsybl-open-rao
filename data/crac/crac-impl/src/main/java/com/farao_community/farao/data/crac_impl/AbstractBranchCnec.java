/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThreshold;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.powsybl.iidm.network.Network;

import java.util.HashSet;
import java.util.Set;

/**
 * Critical network element and contingency.
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public abstract class AbstractBranchCnec<T extends BranchCnec<T>> extends AbstractCnec<T> implements BranchCnec<T> {

    protected Set<BranchThreshold> thresholds;
    protected final Double[] nominalVoltages = new Double[2];
    protected BranchBoundsCache bounds = new BranchBoundsCache();

    AbstractBranchCnec(String id,
                        String name,
                        NetworkElement networkElement,
                        String operator,
                        State state,
                        boolean optimized,
                        boolean monitored,
                        Set<BranchThreshold> thresholds,
                        double frm,
                        Double nominalVLeft,
                        Double nominalVRight) {
        super(id, name, networkElement, operator, state, optimized, monitored, frm);
        this.thresholds = thresholds;
        this.nominalVoltages[0] = nominalVLeft;
        this.nominalVoltages[1] = nominalVRight;
    }

    @Deprecated
    // todo : delete method
    protected AbstractBranchCnec(String id, String name, NetworkElement networkElement, String operator, State state, boolean optimized,
                              boolean monitored, Set<BranchThreshold> thresholds) {
        super(id, name, networkElement, operator, state, optimized, monitored, 0);
        setThresholds(thresholds);
    }

    @Deprecated
    // todo : delete method
    protected AbstractBranchCnec(String id, NetworkElement networkElement, String operator, State state, boolean optimized,
                              boolean monitored, Set<BranchThreshold> thresholds) {
        super(id, networkElement, operator, state, optimized, monitored);
        setThresholds(thresholds);
    }

    @Override
    public double computeMargin(double actualValue, Side side, Unit unit) {
        double marginOnLowerBound = actualValue - getLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - actualValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public final Set<BranchThreshold> getThresholds() {
        return thresholds;
    }

    @Override
    @Deprecated
    //todo : delete
    public void addThreshold(BranchThreshold branchThreshold) {
        bounds.resetBounds();
        thresholds.add(branchThreshold);
    }

    @Deprecated
    //todo : delete
    public void setThresholds(Set<BranchThreshold> thresholds) {
        bounds.resetBounds();
        this.thresholds = new HashSet<>(thresholds);
    }

    @Override
    public void synchronize(Network network) {
    }

    @Override
    public Double getNominalVoltage(Side side) {
        //checkSynchronized(format("access voltage levels of branch cnec %s", getId()));
        return nominalVoltages[side.equals(Side.LEFT) ? 0 : 1];
    }

    @Override
    public void desynchronize() {
        isSynchronized = false;
    }

    @Override
    public boolean isSynchronized() {
        return isSynchronized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractBranchCnec cnec = (AbstractBranchCnec) o;
        return super.equals(cnec) && thresholds.equals(cnec.thresholds);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
