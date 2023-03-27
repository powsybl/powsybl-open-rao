/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class InjectionSetpointImplTest {

    @Test
    public void basicMethods() {
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(mockedNetworkElement, 10.);
        assertEquals(10., injectionSetpoint.getSetpoint(), 1e-3);
        assertEquals(mockedNetworkElement, injectionSetpoint.getNetworkElement());
        assertEquals(Set.of(mockedNetworkElement), injectionSetpoint.getNetworkElements());
        assertTrue(injectionSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    public void hasImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _generator"),
            100);

        assertTrue(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void hasNoImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _generator"),
            2000);

        assertFalse(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _generator"),
                100);

        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    public void hasImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _load"),
            100);

        assertTrue(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void hasNoImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _load"),
            1000);

        assertFalse(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void applyOnLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _load"),
                100);

        loadSetpoint.apply(network);
        assertEquals(100., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
    }

    @Test
    public void hasImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("DL1"),
            100);

        assertTrue(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void hasNoImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("DL1"),
            0);

        assertFalse(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    public void applyOnDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("DL1"),
                100);

        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    public void equals() {
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            10.);
        assertEquals(injectionSetpoint, injectionSetpoint);

        InjectionSetpointImpl sameInjectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            10.);
        assertEquals(injectionSetpoint, sameInjectionSetpoint);

        InjectionSetpointImpl differentInjectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            12.);
        assertNotEquals(injectionSetpoint, differentInjectionSetpoint);
    }
}
