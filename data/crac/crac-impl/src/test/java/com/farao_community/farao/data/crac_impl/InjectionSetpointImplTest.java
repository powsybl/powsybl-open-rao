/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class InjectionSetpointImplTest {

    @Test
    void basicMethods() {
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(mockedNetworkElement, 10., Unit.MEGAWATT);
        assertEquals(10., injectionSetpoint.getSetpoint(), 1e-3);
        assertEquals(mockedNetworkElement, injectionSetpoint.getNetworkElement());
        assertEquals(Set.of(mockedNetworkElement), injectionSetpoint.getNetworkElements());
        assertTrue(injectionSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _generator"),
            100, Unit.MEGAWATT);

        assertTrue(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _generator"),
            2000, Unit.MEGAWATT);

        assertFalse(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _generator"),
                100, Unit.MEGAWATT);

        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _load"),
            100, Unit.MEGAWATT);

        assertTrue(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("FFR1AA1 _load"),
            1000, Unit.MEGAWATT);

        assertFalse(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _load"),
                100, Unit.MEGAWATT);

        loadSetpoint.apply(network);
        assertEquals(100., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("DL1"),
            100, Unit.MEGAWATT);

        assertTrue(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("DL1"),
            0, Unit.MEGAWATT);

        assertFalse(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("DL1"),
                100, Unit.MEGAWATT);

        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    void applyOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    void getUnit() {
        InjectionSetpointImpl dummy = new InjectionSetpointImpl(
                new NetworkElementImpl("wrong_name"),
                100, Unit.MEGAWATT);
        assertEquals(Unit.MEGAWATT, dummy.getUnit());
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl dummy = new InjectionSetpointImpl(
                new NetworkElementImpl("wrong_name"),
                100, Unit.MEGAWATT);
        assertThrows(NotImplementedException.class, () -> dummy.hasImpactOnNetwork(network));
    }

    @Test
    void equals() {
        NetworkElement mockedNetworkElement = Mockito.mock(NetworkElement.class);
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            10., Unit.MEGAWATT);
        assertEquals(injectionSetpoint, injectionSetpoint);

        InjectionSetpointImpl sameInjectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            10., Unit.MEGAWATT);
        assertEquals(injectionSetpoint, sameInjectionSetpoint);

        InjectionSetpointImpl differentInjectionSetpoint = new InjectionSetpointImpl(
            mockedNetworkElement,
            12., Unit.MEGAWATT);
        assertNotEquals(injectionSetpoint, differentInjectionSetpoint);
    }
}
