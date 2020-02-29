/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.balances_adjustment.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * ThreeWindingsTransformer at the border of a NetworkArea
 *
 *  @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class BorderThreeWindingsTransformer implements  BorderDevice {

    private final ThreeWindingsTransformer threeWindingsTransformer;

    public BorderThreeWindingsTransformer(ThreeWindingsTransformer threeWindingsTransformer) {
        this.threeWindingsTransformer = threeWindingsTransformer;
    }

    @Override
    public String getId() {
        return threeWindingsTransformer.getId();
    }

    public ThreeWindingsTransformer getThreeWindingsTransformer() {
        return threeWindingsTransformer;
    }


    /**
     * @return leaving flow of threeWindingsTransformer is the sum of two flows (on two sides of the transformer).
     */
    @Override
    public double getLeavingFlow(Network network, NetworkArea networkArea) {
        List<VoltageLevel> voltageLevels = networkArea.getAreaVoltageLevels(network);
        double outsideFlow = 0;
        double insideFlow = 0;
        for (ThreeWindingsTransformer.Side side : ThreeWindingsTransformer.Side.values()) {
            outsideFlow += !voltageLevels.contains(threeWindingsTransformer.getTerminal(side).getVoltageLevel()) && threeWindingsTransformer.getTerminal(side).isConnected()
                    ?  threeWindingsTransformer.getTerminal(side).getP() : 0;
            insideFlow += voltageLevels.contains(threeWindingsTransformer.getTerminal(side).getVoltageLevel()) && threeWindingsTransformer.getTerminal(side).isConnected()
                    ?  threeWindingsTransformer.getTerminal(side).getP() : 0;
        }
        return (insideFlow - outsideFlow) / 2;
    }
}
