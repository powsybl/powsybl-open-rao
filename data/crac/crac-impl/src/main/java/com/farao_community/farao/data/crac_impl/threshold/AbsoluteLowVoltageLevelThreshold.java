/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl.threshold;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Direction;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.Side;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@JsonTypeName("absolute-low-voltage-threshold")
public class AbsoluteLowVoltageLevelThreshold extends AbsoluteFlowThreshold {

    @JsonCreator
    public AbsoluteLowVoltageLevelThreshold(@JsonProperty("unit") Unit unit,
                                            @JsonProperty("direction") Direction direction,
                                            @JsonProperty("maxValue") double maxValue) {
        super(unit, null, direction, maxValue);
    }

    public AbsoluteLowVoltageLevelThreshold(Unit unit, NetworkElement networkElement, Direction direction, double maxValue, double frmInMw) {
        super(unit, networkElement, null, direction, maxValue, frmInMw);
    }

    public AbsoluteLowVoltageLevelThreshold(Unit unit, NetworkElement networkElement, Side side, Direction direction, double maxValue, double frmInMw) {
        super(unit, networkElement, side, direction, maxValue, frmInMw);
    }

    @Override
    public void synchronize(Network network) {
        Branch branch = checkAndGetValidBranch(network, getNetworkElement().getId());
        if (branch.getTerminal1().getVoltageLevel().getNominalV() <= branch.getTerminal2().getVoltageLevel().getNominalV()) {
            side = Side.LEFT;
        } else {
            side = Side.RIGHT;
        }
        super.synchronize(network);
    }

    @Override
    public AbstractFlowThreshold copy() {
        AbsoluteLowVoltageLevelThreshold copiedAbsoluteFlowThreshold = new AbsoluteLowVoltageLevelThreshold(unit, networkElement, side, direction, maxValue, frmInMW);
        if (isSynchronized()) {
            copiedAbsoluteFlowThreshold.isSynchronized = isSynchronized;
            copiedAbsoluteFlowThreshold.voltageLevel = voltageLevel;
        }
        return copiedAbsoluteFlowThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbsoluteLowVoltageLevelThreshold threshold = (AbsoluteLowVoltageLevelThreshold) o;
        return super.equals(threshold);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
