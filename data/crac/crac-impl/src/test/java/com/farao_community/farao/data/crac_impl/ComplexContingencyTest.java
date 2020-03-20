/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class ComplexContingencyTest {

    @Test
    public void testDifferentWithDifferentIds() {
        ComplexContingency complexContingency1 = new ComplexContingency(
            "contingency-1",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-2")).collect(Collectors.toSet())
        );

        ComplexContingency complexContingency2 = new ComplexContingency(
            "contingency-2",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-2")).collect(Collectors.toSet())
        );

        assertNotEquals(complexContingency1, complexContingency2);
    }

    @Test
    public void testDifferentWithDifferentObjects() {
        ComplexContingency complexContingency1 = new ComplexContingency(
            "contingency-1",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-2")).collect(Collectors.toSet())
        );

        ComplexContingency complexContingency2 = new ComplexContingency(
            "contingency-1",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-5")).collect(Collectors.toSet())
        );

        assertNotEquals(complexContingency1, complexContingency2);
    }

    @Test
    public void testEqual() {
        ComplexContingency complexContingency1 = new ComplexContingency(
            "contingency-1",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-2")).collect(Collectors.toSet())
        );

        ComplexContingency complexContingency2 = new ComplexContingency(
            "contingency-1",
            Stream.of(new NetworkElement("network-element-1"), new NetworkElement("network-element-2")).collect(Collectors.toSet())
        );

        assertEquals(complexContingency1, complexContingency2);
    }

    @Test(expected = FaraoException.class)
    public void testApply() {
        ComplexContingency complexContingency = new ComplexContingency("cnec1");
        complexContingency.addNetworkElement(new NetworkElement("FRANCE_BELGIUM_1"));
        assertEquals(1, complexContingency.getNetworkElements().size());
        ComputationManager computationManager = LocalComputationManager.getDefault();
        Network network = Importers.loadNetwork("TestCase2Nodes.xiidm", getClass().getResourceAsStream("/TestCase2Nodes.xiidm"));
        assertFalse(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
        complexContingency.apply(network, computationManager);
        assertTrue(network.getBranch("FRANCE_BELGIUM_1").getTerminal1().connect());
        complexContingency.addNetworkElement(new NetworkElement("None"));
        complexContingency.apply(network, computationManager);
    }

}
