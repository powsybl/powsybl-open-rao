/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * BorderBranch is a Branch (Line or TwoWindingsTransformer) at the border of a NetworkArea
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BorderBranch implements BorderDevice {

    private final Branch branch;

    public BorderBranch(Branch branch) {
        this.branch = branch;
    }

    public Branch getBranch() {
        return branch;
    }

    @Override
    public String getId() {
        return branch.getId();
    }



    /**
     * @return the mean value of the flow on the area’s side and the flow on the other side
     */
    @Override
    public double getLeavingFlow(Network network, NetworkArea networkArea) {
        List<VoltageLevel> voltageLevels = networkArea.getAreaVoltageLevels(network);
        double leavingFlow = 0;
        for (Branch.Side side : Branch.Side.values()) {
            double flow = voltageLevels.contains(branch.getTerminal(side).getVoltageLevel()) ?  branch.getTerminal(side).getP() : -branch.getTerminal(side).getP();
            leavingFlow += branch.getTerminal(side).isConnected() ? flow : 0;
        }
        return leavingFlow / 2;
    }
}
