/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_computation;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CracContingenciesProviderTest {

    @Test
    public void cracPstWithRange() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addHvdcLine(network);

        network.getSubstation("BBE1AA").newVoltageLevel().setId("BBE1AA2").setNominalV(225).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        network.getVoltageLevel("BBE1AA2").getNodeBreakerView().newBusbarSection().setId("BB1").setNode(1).add();

        Crac crac = CommonCracCreation.createWithPstRange();
        ComplexContingency generatorContingency = new ComplexContingency("contingency-generator");
        generatorContingency.addNetworkElement(new NetworkElement("BBE1AA1 _generator"));
        ComplexContingency hvdcContingency = new ComplexContingency("contingency-hvdc");
        hvdcContingency.addNetworkElement(new NetworkElement("HVDC1"));
        ComplexContingency busbarSectionContingency = new ComplexContingency("contingency-busbar-section");
        hvdcContingency.addNetworkElement(new NetworkElement("BB1"));
        crac.addContingency(generatorContingency);
        crac.addContingency(hvdcContingency);
        crac.addContingency(busbarSectionContingency);

        CracContingenciesProvider provider = new CracContingenciesProvider(crac);

        // Common Crac contains 6 CNEC and 1 range action
        List<Contingency> contingencyList = provider.getContingencies(network);
        assertEquals(5, contingencyList.size());
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR3")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR2")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-generator")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-hvdc")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-busbar-section")));
    }

    @Test(expected = FaraoException.class)
    public void testFailureOnContingency() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPstRange();

        ComplexContingency busBreakerContingency = new ComplexContingency("contingency-fail");
        busBreakerContingency.addNetworkElement(new NetworkElement("FFR3AA1"));
        crac.addContingency(busBreakerContingency);

        CracContingenciesProvider provider = new CracContingenciesProvider(crac);

        List<Contingency> contingencyList = provider.getContingencies(network);
    }
}
