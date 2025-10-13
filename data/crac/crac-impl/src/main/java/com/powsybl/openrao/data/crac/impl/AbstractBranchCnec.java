/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.BranchCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;

import java.util.Collections;
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
                        String border,
                        State state,
                        boolean optimized,
                        boolean monitored,
                        Set<BranchThreshold> thresholds,
                        double frm,
                        Double nominalVLeft,
                        Double nominalVRight) {
        super(id, name, Collections.singleton(networkElement), operator, border, state, optimized, monitored, frm);
        this.thresholds = thresholds;
        this.nominalVoltages[0] = nominalVLeft;
        this.nominalVoltages[1] = nominalVRight;
    }

    @Override
    public NetworkElement getNetworkElement() {
        return getNetworkElements().iterator().next();
    }

    @Override
    public double computeMargin(double actualValue, TwoSides side, Unit unit) {
        double marginOnLowerBound = actualValue - getLowerBound(side, unit).orElse(Double.NEGATIVE_INFINITY);
        double marginOnUpperBound = getUpperBound(side, unit).orElse(Double.POSITIVE_INFINITY) - actualValue;
        return Math.min(marginOnLowerBound, marginOnUpperBound);
    }

    @Override
    public final Set<BranchThreshold> getThresholds() {
        return thresholds;
    }

    @Override
    public Double getNominalVoltage(TwoSides side) {
        return nominalVoltages[side.equals(TwoSides.ONE) ? 0 : 1];
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
