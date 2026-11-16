/*
 * Copyright (c) 2026, Artelys (http://www.artelys.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.commons;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Rodolphe Griset {@literal <rodolphe.griset@artelys.com>}
 */
class AreaGraphTest {

    private static final String AREA_TYPE = "ControlArea";

    @Test
    void areasLinkedThroughXnodeAreNeighbours() {
        Network network = Network.create("test", "test");
        VoltageLevel es = addVoltageLevel(network, "ES", Country.ES);
        VoltageLevel pt = addVoltageLevel(network, "PT", Country.PT);
        VoltageLevel xnode = addVoltageLevel(network, "XNODE", null);
        addLine(network, "ES-XNODE", es, xnode);
        addLine(network, "XNODE-PT", xnode, pt);
        network.newArea().setId("10YES").setAreaType(AREA_TYPE).addVoltageLevel(es).add();
        network.newArea().setId("10YPT").setAreaType(AREA_TYPE).addVoltageLevel(pt).add();

        AreaGraph areaGraph = new AreaGraph(network, AREA_TYPE);

        assertEquals(Set.of("10YPT"), areaGraph.getNeighbours("10YES"));
        assertEquals(Set.of("10YES"), areaGraph.getNeighbours("10YPT"));
    }

    private static VoltageLevel addVoltageLevel(Network network, String id, Country country) {
        Substation substation = network.newSubstation().setId("S_" + id).setCountry(country).add();
        VoltageLevel voltageLevel = substation.newVoltageLevel().setId(id).setNominalV(400.)
                .setTopologyKind(TopologyKind.BUS_BREAKER).add();
        voltageLevel.getBusBreakerView().newBus().setId("B_" + id).add();
        return voltageLevel;
    }

    private static void addLine(Network network, String id, VoltageLevel voltageLevel1, VoltageLevel voltageLevel2) {
        network.newLine().setId(id)
                .setVoltageLevel1(voltageLevel1.getId()).setBus1("B_" + voltageLevel1.getId())
                .setVoltageLevel2(voltageLevel2.getId()).setBus2("B_" + voltageLevel2.getId())
                .setR(1.).setX(10.).setG1(0.).setB1(0.).setG2(0.).setB2(0.)
                .add();
    }
}
