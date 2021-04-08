/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;
import com.farao_community.farao.data.crac_impl.threshold.BranchThresholdImpl;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracFileTest {
    private SimpleCrac simpleCrac;
    private static final Logger LOGGER = LoggerFactory.getLogger(CracFileTest.class);

    @Before
    public void setUp() {
        simpleCrac = new SimpleCrac("test-crac");
    }

    @Test
    public void testAddNetworkElementWithIdAndName() {
        NetworkElement networkElement = simpleCrac.addNetworkElement("neID", "neName");
        assertEquals(1, simpleCrac.getNetworkElements().size());
        assertNotNull(simpleCrac.getNetworkElement("neID"));
        assertSame(networkElement, simpleCrac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithIdAndNameFail() {
        simpleCrac.addNetworkElement("neID", "neName");
        try {
            simpleCrac.addNetworkElement("neID", "neName-fail");
            fail();
        } catch (FaraoException e) {
            assertEquals("A network element with the same ID (neID) but a different name already exists.", e.getMessage());
        }
    }

    @Test
    public void testAddNetworkElementWithIdAndNameTwice() {
        NetworkElement networkElement1 = simpleCrac.addNetworkElement("neID", "neName");
        NetworkElement networkElement2 = simpleCrac.addNetworkElement("neID", "neName");
        assertEquals(1, simpleCrac.getNetworkElements().size());
        assertNotNull(simpleCrac.getNetworkElement("neID"));
        assertSame(networkElement1, networkElement2);
        assertSame(networkElement1, simpleCrac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithId() {
        NetworkElement networkElement = simpleCrac.addNetworkElement("neID");
        assertEquals(1, simpleCrac.getNetworkElements().size());
        assertNotNull(simpleCrac.getNetworkElement("neID"));
        assertSame(networkElement, simpleCrac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithNetworkElement() {
        NetworkElement networkElement = simpleCrac.addNetworkElement("neID");
        assertEquals(1, simpleCrac.getNetworkElements().size());
        assertNotNull(simpleCrac.getNetworkElement("neID"));
        assertSame(networkElement, simpleCrac.getNetworkElement("neID"));
    }

    @Test
    public void testGetInstant() {
        assertEquals(0, simpleCrac.getInstants().size());
    }

    @Test
    public void testAddInstantWithId() {
        simpleCrac.addInstant("initial-instant", 0);
        assertEquals(1, simpleCrac.getInstants().size());
        assertNotNull(simpleCrac.getInstant("initial-instant"));
        assertEquals(0, simpleCrac.getInstant("initial-instant").getSeconds());
    }

    @Test
    public void testAddInstantWithInstant() {
        assertEquals(0, simpleCrac.getInstants().size());
        simpleCrac.addInstant(new Instant("initial-instant", 0));
        assertEquals(1, simpleCrac.getInstants().size());
        assertNotNull(simpleCrac.getInstant("initial-instant"));
        try {
            simpleCrac.addInstant(new Instant("initial-instant", 12));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same ID but different seconds already exists.", e.getMessage());
        }
        try {
            simpleCrac.addInstant(new Instant("fail-initial", 0));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same seconds but different ID already exists.", e.getMessage());
        }
        assertEquals(1, simpleCrac.getInstants().size());
        simpleCrac.addInstant(new Instant("curative", 60));
        assertEquals(2, simpleCrac.getInstants().size());
        assertNotNull(simpleCrac.getInstant("curative"));
    }

    @Test
    public void testGetContingency() {
        assertEquals(0, simpleCrac.getContingencies().size());
    }

    @Test
    public void testAddContingencyWithElements() {
        simpleCrac.addContingency("contingency-1", "ne1", "ne2");
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-1"));
        try {
            simpleCrac.addContingency("contingency-1", "ne2");
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID (contingency-1) but a different network elements already exists.", e.getMessage());
        }
        try {
            simpleCrac.addContingency("contingency-2", "ne1");
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, simpleCrac.getContingencies().size());
        simpleCrac.addContingency("contingency-3", "ne3");
        assertEquals(3, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-3"));
        assertNull(simpleCrac.getContingency("contingency-fail"));
    }

    @Test
    public void testAddContingency() {
        assertEquals(0, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1"))));
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-1"));
        try {
            simpleCrac.addContingency(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne2"))));
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID (contingency-1) but a different network elements already exists.", e.getMessage());
        }
        try {
            simpleCrac.addContingency(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("ne1"))));
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ComplexContingency("contingency-3", Collections.singleton(new NetworkElement("ne3"))));
        assertEquals(3, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-3"));
        assertNull(simpleCrac.getContingency("contingency-fail"));
    }

    @Test
    public void testAddXnodeContingency() {
        assertEquals(0, simpleCrac.getContingencies().size());
        assertEquals(0, simpleCrac.getNetworkElements().size());
        simpleCrac.newContingency().withId("xnode-contingency").addXnode("xnode").add();
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("xnode-contingency"));
        assertEquals(0, simpleCrac.getNetworkElements().size());
    }

    @Test
    public void testSyncXnodeContingency() {
        Network network = Importers.loadNetwork("TestCase12NodesHvdc.uct", getClass().getResourceAsStream("/TestCase12NodesHvdc.uct"));
        XnodeContingency xnodeContingency = (XnodeContingency) simpleCrac.newContingency().withId("xnode-cont").addXnode("XLI_OB1A").addXnode("XLI_OB1B").add();
        simpleCrac.synchronize(network);
        assertTrue(xnodeContingency.isSynchronized());
        assertEquals(2, xnodeContingency.getNetworkElements().size());
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(xnodeContingency.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
        assertTrue(simpleCrac.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("DDE3AA1  XLI_OB1A 1")));
        assertTrue(simpleCrac.getNetworkElements().stream().anyMatch(ne -> ne.getId().equals("BBE2AA1  XLI_OB1B 1")));
    }

    @Test(expected = FaraoException.class)
    public void addStatesWithIdFail() {
        simpleCrac.addState("contingency", "instant");
    }

    @Test
    public void addStatesWithId() {
        simpleCrac.addContingency("contingency", "neID");
        simpleCrac.addInstant("instant", 5);
        simpleCrac.addState("contingency", "instant");
        assertNotNull(simpleCrac.getState("contingency-instant"));
    }

    @Test
    public void addStatesWithIdPreventive() {
        simpleCrac.addInstant("instant", 0);
        simpleCrac.addState(null, "instant");
        assertNotNull(simpleCrac.getState("none-instant"));
    }

    @Test(expected = FaraoException.class)
    public void addStatesWithIdPreventiveFail() {
        simpleCrac.addState(null, "instant");
    }

    @Test(expected = FaraoException.class)
    public void addStatesWithObjectFail() {
        simpleCrac.addState(new ComplexContingency(
                "contingency", Collections.singleton(simpleCrac.addNetworkElement("neID"))
        ), new Instant("instant", 5));
    }

    @Test
    public void addStatesWithObject() {
        Contingency contingency = simpleCrac.addContingency("contingency", "neID");
        Instant instant = simpleCrac.addInstant("instant", 5);
        simpleCrac.addState(contingency, instant);
        assertNotNull(simpleCrac.getState("contingency-instant"));
    }

    @Test
    public void addStatesWithObjectPreventive() {
        Instant instant = simpleCrac.addInstant("instant", 0);
        simpleCrac.addState(null, instant);
        assertNotNull(simpleCrac.getState("none-instant"));
    }

    @Test(expected = FaraoException.class)
    public void addStatesWithObjectPreventiveFail() {
        simpleCrac.addState(null, new Instant("instant", 5));
    }

    @Test
    public void testStates() {
        assertNull(simpleCrac.getPreventiveState());
        assertEquals(0, simpleCrac.getContingencies().size());
        assertEquals(0, simpleCrac.getInstants().size());

        simpleCrac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 0)));
        assertNotNull(simpleCrac.getPreventiveState());
        assertEquals("initial-instant", simpleCrac.getPreventiveState().getInstant().getId());

        assertEquals(simpleCrac.getInstant("initial-instant"), simpleCrac.getPreventiveState().getInstant());

        simpleCrac.addState(new SimpleState(
                Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
                new Instant("after-contingency", 60))
        );

        try {
            simpleCrac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant-fail", 0)));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same seconds but different ID already exists.", e.getMessage());
        }

        try {
            simpleCrac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 12)));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same ID but different seconds already exists.", e.getMessage());
        }

        assertEquals(2, simpleCrac.getInstants().size());
        assertEquals(2, simpleCrac.getStates().size());
        assertEquals(1, simpleCrac.getContingencies().size());

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        assertEquals(2, simpleCrac.getStatesFromInstant("after-contingency").size());

        // Different states pointing at the same instant object
        Instant instant = simpleCrac.getInstant("after-contingency");
        simpleCrac.getStates(instant).forEach(state -> assertSame(instant, state.getInstant()));

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        // Different states pointing at the same contingency object
        Contingency contingency = simpleCrac.getContingency("contingency-2");
        assertEquals(2, simpleCrac.getStates(contingency).size());
        simpleCrac.getStates(contingency).forEach(state -> {
            assertTrue(state.getContingency().isPresent());
            assertSame(contingency, state.getContingency().get());
        }
        );

        State testState = simpleCrac.getState(contingency, instant);
        assertTrue(testState.getContingency().isPresent());
        assertSame(testState.getContingency().get(), contingency);
        assertSame(testState.getInstant(), instant);
    }

    @Test
    public void testGetStatesWithPreventiveInstantId() {
        assertEquals(0, simpleCrac.getStatesFromInstant("initial-instant").size());

        simpleCrac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 0)));
        assertNotNull(simpleCrac.getStatesFromInstant("initial-instant"));
        assertEquals(1, simpleCrac.getStatesFromInstant("initial-instant").size());
        assertSame(simpleCrac.getStatesFromInstant("initial-instant").iterator().next(), simpleCrac.getPreventiveState());
    }

    @Test
    public void testGetStatesWithInstantIds() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        assertEquals(2, simpleCrac.getStatesFromInstant("after-contingency").size());
        assertEquals(1, simpleCrac.getStatesFromInstant("after-contingency-bis").size());
    }

    @Test
    public void testGetStatesWithContingencyIds() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        assertEquals(1, simpleCrac.getStatesFromContingency("contingency").size());
        assertEquals(2, simpleCrac.getStatesFromContingency("contingency-2").size());
    }

    @Test
    public void testGetStateWithIds() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNotNull(simpleCrac.getState("contingency", "after-contingency"));
    }

    @Test
    public void testGetStateWithNotExistingContingencyId() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNull(simpleCrac.getState("fail-contingency", "after-contingency"));
    }

    @Test
    public void testGetStateWithNotExistingInstantId() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNull(simpleCrac.getState("contingency", "fail-after-contingency"));
    }

    @Test
    public void testGetCnecWithIds() {
        BranchCnec cnec = new FlowCnecImpl(
            "cnec",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(
                Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                new Instant("after-co", 60)
            ),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );

        simpleCrac.addCnec(cnec);

        assertEquals(1, simpleCrac.getBranchCnecs("co", "after-co").size());
        BranchCnec getCnec = simpleCrac.getBranchCnecs("co", "after-co").iterator().next();
        assertEquals("cnec", getCnec.getId());
        assertEquals("network-element-1", getCnec.getNetworkElement().getId());
    }

    @Test
    public void testOrderedStates() {
        State state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1")))),
            new Instant("auto", 60)
        );

        State state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1")))),
            new Instant("auto-later", 70)
        );

        State state3 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1")))),
            new Instant("curative", 120)
        );

        simpleCrac.addState(state3);
        simpleCrac.addState(state1);
        simpleCrac.addState(state2);

        Iterator<State> states = simpleCrac.getStatesFromContingency("contingency-1").iterator();
        assertEquals(
                60,
                states.next().getInstant().getSeconds()
        );
        assertEquals(
                70,
                states.next().getInstant().getSeconds()
        );
        assertEquals(
                120,
                states.next().getInstant().getSeconds()
        );

        State state4 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1")))),
            new Instant("intermediate", 100)
        );

        simpleCrac.addState(state4);

        states = simpleCrac.getStatesFromContingency("contingency-1").iterator();
        assertEquals(
                60,
                states.next().getInstant().getSeconds()
        );
        assertEquals(
                70,
                states.next().getInstant().getSeconds()
        );
        assertEquals(
                100,
                states.next().getInstant().getSeconds()
        );
        assertEquals(
                120,
                states.next().getInstant().getSeconds()
        );
    }

    @Test
    public void testAddCnecWithNoConflicts() {
        BranchCnec cnec1 = new FlowCnecImpl(
            "cnec1",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(Optional.empty(), new Instant("initial-instant", 0)),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );

        simpleCrac.addCnec(cnec1);
        assertEquals(0, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("initial-instant"));
        assertNotNull(simpleCrac.getPreventiveState());
        assertEquals(1, simpleCrac.getBranchCnecs(simpleCrac.getPreventiveState()).size());
        assertSame(simpleCrac.getBranchCnecs(simpleCrac.getPreventiveState()).iterator().next().getState(), simpleCrac.getPreventiveState());

        BranchCnec cnec2 = new FlowCnecImpl(
            "cnec2",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(
                Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                new Instant("after-co", 60)
            ),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );
        simpleCrac.addCnec(cnec2);
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("after-co"));
        assertNotNull(simpleCrac.getState("co", "after-co"));
        assertSame(simpleCrac.getBranchCnecs(simpleCrac.getState("co", "after-co")).iterator().next().getState(), simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
    }

    @Test
    public void testAddCnecWithAlreadyExistingState() {
        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-co", 60)
        ));

        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("after-co"));
        assertNotNull(simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));

        BranchCnec cnec = new FlowCnecImpl(
            "cnec2",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(
                Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                new Instant("after-co", 60)
            ),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );
        simpleCrac.addCnec(cnec);
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("after-co"));
        assertNotNull(simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
        assertSame(
                simpleCrac.getBranchCnecs(simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co"))).iterator().next().getState(),
                simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
    }

    @Test
    public void testAddCnecWithTwoIdenticalCnecs() {
        BranchCnec cnec1 = new FlowCnecImpl(
            "cnec2",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(
                Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                new Instant("after-co", 60)
            ),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );
        BranchCnec cnec2 = new FlowCnecImpl(
            "cnec2",
            new NetworkElement("network-element-1"),
            "operator",
            new SimpleState(
                Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                new Instant("after-co", 60)
            ),
            true, false,
            Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
            0.
        );

        assertEquals(0, simpleCrac.getBranchCnecs().size());
        simpleCrac.addCnec(cnec1);
        assertEquals(1, simpleCrac.getBranchCnecs().size());
        simpleCrac.addCnec(cnec2);
        assertEquals(1, simpleCrac.getBranchCnecs().size());
    }

    @Test
    public void testAddRangeActionWithNoConflict() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.getId()).thenReturn("instantid");
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        simpleCrac.addRangeAction(rangeAction);

        assertEquals(0, simpleCrac.getBranchCnecs().size());
    }

    @Test(expected = FaraoException.class)
    public void synchronizeFailSecondTime() {
        Network network = Mockito.mock(Network.class);
        simpleCrac.synchronize(network);
        simpleCrac.synchronize(network);
    }

    @Test(expected = Test.None.class)
    public void synchronizeThenDesynchronizeThenSynchronizeAgain() {
        Network network = Mockito.mock(Network.class);
        simpleCrac.synchronize(network);
        simpleCrac.desynchronize();
        simpleCrac.synchronize(network);
    }
}
