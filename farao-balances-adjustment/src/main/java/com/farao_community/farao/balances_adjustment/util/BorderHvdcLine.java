/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BorderHvdcLine implements  BorderDevice {

    private final HvdcLine hvdcLine;

    public BorderHvdcLine(HvdcLine hvdcLine) {
        this.hvdcLine = hvdcLine;
    }

    public HvdcLine getHvdcLine() {
        return hvdcLine;
    }

    @Override
    public String getId() {
        return hvdcLine.getId();
    }

    /**
     * @return the mean value of the flow on the area’s side and the flow on the other side
     */
    @Override
    public double getLeavingFlow(Network network, NetworkArea networkArea) {
        List<VoltageLevel> voltageLevels = networkArea.getAreaVoltageLevels(network);
        double leavingFlow = 0;

        double flow = voltageLevels.contains(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel()) ?
                hvdcLine.getConverterStation1().getTerminal().getP() : -hvdcLine.getConverterStation1().getTerminal().getP();
        leavingFlow += hvdcLine.getConverterStation1().getTerminal().isConnected() ? flow : 0;

        flow = voltageLevels.contains(hvdcLine.getConverterStation2().getTerminal().getVoltageLevel()) ?
                hvdcLine.getConverterStation2().getTerminal().getP() : -hvdcLine.getConverterStation2().getTerminal().getP();
        leavingFlow += hvdcLine.getConverterStation2().getTerminal().isConnected() ? flow : 0;

        return leavingFlow / 2;
    }

}
