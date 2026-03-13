/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.threshold.BranchThreshold;

/**
 * Limits of a flow (in MEGAWATT or AMPERE) through a branch.
 *
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class BranchThresholdImpl extends ThresholdImpl implements BranchThreshold {

    /**
     * Side of the network element which is monitored
     */
    protected TwoSides side;

    /**
     * Direction in which the flow is monitored
     */

    BranchThresholdImpl(TwoSides side, Unit unit, Double min, Double max) {
        super(unit, min, max);
        this.side = side;
    }

    @Override
    public TwoSides getSide() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BranchThresholdImpl otherT = (BranchThresholdImpl) o;

        return super.equals(otherT)
                && side.equals(otherT.getSide());
    }

    @Override
    public int hashCode() {
        return super.hashCode()
                + 31 * side.hashCode();
    }
}
