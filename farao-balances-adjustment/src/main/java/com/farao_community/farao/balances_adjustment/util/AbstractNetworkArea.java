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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public abstract class AbstractNetworkArea implements NetworkArea {

    @Override
    public List<BorderDevice> getBorderDevices(Network network) {
        List<BorderDevice> borderDevices = new ArrayList<>();
        List<VoltageLevel> areaVoltageLevels = getAreaVoltageLevels(network);
        borderDevices.addAll(this.getBorderBranch(network, areaVoltageLevels));
        borderDevices.addAll(this.getBorderThreeWindingTransformer(network, areaVoltageLevels));
        borderDevices.addAll(this.getBorderHvdcLine(network, areaVoltageLevels));
        return  borderDevices;
    }

    public List<BorderDevice> getBorderBranch(Network network, List<VoltageLevel> areaVoltageLevels) {

        return network.getBranchStream().filter(branch ->
                areaVoltageLevels.contains(branch.getTerminal1().getVoltageLevel()) && !areaVoltageLevels.contains(branch.getTerminal2().getVoltageLevel()) ||
                        areaVoltageLevels.contains(branch.getTerminal2().getVoltageLevel()) && !areaVoltageLevels.contains(branch.getTerminal1().getVoltageLevel()))
                .map(BorderBranch::new)
                .collect(Collectors.toList());

    }

    private int getVoltageLevelsFoundCount(ThreeWindingsTransformer threeWindingsTransformer, List<VoltageLevel> areaVoltageLevels) {
        int count = 0;
        for (ThreeWindingsTransformer.Side side : ThreeWindingsTransformer.Side.values()) {
            if (areaVoltageLevels.contains(threeWindingsTransformer.getTerminal(side).getVoltageLevel())) {
                count += 1;
            }
        }
        return count;
    }

    public List<BorderDevice> getBorderThreeWindingTransformer(Network network, List<VoltageLevel> areaVoltageLevels) {

        return network.getThreeWindingsTransformerStream().filter(t -> {
            int n = getVoltageLevelsFoundCount(t, areaVoltageLevels);
            return n > 0 && n < 3;
        })
                .map(BorderThreeWindingsTransformer::new)
                .collect(Collectors.toList());
    }

    public List<BorderDevice> getBorderHvdcLine(Network network, List<VoltageLevel> areaVoltageLevels) {

        return network.getHvdcLineStream().filter(h -> areaVoltageLevels.contains(h.getConverterStation1().getTerminal().getVoltageLevel()) &&
                !areaVoltageLevels.contains(h.getConverterStation2().getTerminal().getVoltageLevel()) ||
                areaVoltageLevels.contains(h.getConverterStation2().getTerminal().getVoltageLevel()) &&
                        !areaVoltageLevels.contains(h.getConverterStation1().getTerminal().getVoltageLevel()))
                .map(BorderHvdcLine::new)
                .collect(Collectors.toList());
    }

    @Override
    public double getNetPosition(Network network) {
        double netPosition = 0;
        for (BorderDevice b : this.getBorderDevices(network)) {
            double flow = b.getLeavingFlow(network, this);
            if (!Double.isNaN(flow)) {
                netPosition += flow;
            }
        }
        return  netPosition;
    }
}
