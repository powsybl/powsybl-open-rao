/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class RelativeFlowThreshold extends AbstractFlowThreshold {

    private double percentageOfMax;

    public RelativeFlowThreshold(Unit unit, Side side, Direction direction, double percentageOfMax) {
        super(unit, side, direction);
        this.percentageOfMax = percentageOfMax;
    }

    @Override
    public boolean isMinThresholdOvercome(Network network, Cnec cnec) {
        return false;
    }

    @Override
    public boolean isMaxThresholdOvercome(Network network, Cnec cnec) throws SynchronizationException {
        if (Double.isNaN(maxValue)) {
            throw new SynchronizationException("Relative flow threshold have not been synchronized with network");
        }
        switch (unit) {
            case AMPERE:
                return (maxValue * percentageOfMax / 100) < getTerminal(network, cnec).getI();
            case MEGAWATT:
                return (maxValue * percentageOfMax / 100) < getTerminal(network, cnec).getP();
            case DEGREE:
            case KILOVOLT:
            default:
                throw new FaraoException("Incompatible type of unit between FlowThreshold and degree or kV");
        }
    }

    @Override
    public void synchronize(Network network, Cnec cnec) {
        // TODO: manage matching between LEFT/RIGHT and ONE/TWO
        switch (side) {
            case LEFT:
                maxValue = network.getBranch(cnec.getCriticalNetworkElement().getId()).getCurrentLimits(Branch.Side.ONE).getPermanentLimit();
                break;
            case RIGHT:
                maxValue = network.getBranch(cnec.getCriticalNetworkElement().getId()).getCurrentLimits(Branch.Side.TWO).getPermanentLimit();
                break;
            default:
                throw new FaraoException("Side is not defined");
        }

    }

    @Override
    public void desynchronize() {
        maxValue = Double.NaN;
    }
}
