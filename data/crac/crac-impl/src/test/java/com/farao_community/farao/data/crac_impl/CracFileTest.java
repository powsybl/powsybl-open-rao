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
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_api.threshold.Direction.OPPOSITE;
import static com.farao_community.farao.data.crac_api.threshold.Side.LEFT;
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
        NetworkElement networkElement = simpleCrac.addNetworkElement(new NetworkElement("neID"));
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
    public void addStatesWithIdFail() {
        try {
            simpleCrac.addState("contingency", "instant");
            fail();
        } catch (FaraoException e) {
            // must throw
        }
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

    @Test
    public void addStatesWithIdPreventiveFail() {
        try {
            simpleCrac.addState(null, "instant");
            fail();
        } catch (FaraoException e) {
            // must throw
        }
    }

    @Test
    public void addStatesWithObjectFail() {
        try {
            simpleCrac.addState(new ComplexContingency(
                "contingency", Collections.singleton(simpleCrac.addNetworkElement("neID"))
            ), new Instant("instant", 5));
            fail();
        } catch (FaraoException e) {
            // must throw
        }
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

    @Test
    public void addStatesWithObjectPreventiveFail() {
        try {
            simpleCrac.addState(null, new Instant("instant", 5));
            fail();
        } catch (FaraoException e) {
            // must throw
        }
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
        Cnec cnec = new SimpleCnec(
                "cnec",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        simpleCrac.addCnec(cnec);

        assertEquals(1, simpleCrac.getCnecs("co", "after-co").size());
        Cnec getCnec = simpleCrac.getCnecs("co", "after-co").iterator().next();
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
        Cnec cnec1 = new SimpleCnec(
                "cnec1",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(Optional.empty(), new Instant("initial-instant", 0))
        );

        simpleCrac.addCnec(cnec1);
        assertEquals(0, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("initial-instant"));
        assertNotNull(simpleCrac.getPreventiveState());
        assertEquals(1, simpleCrac.getCnecs(simpleCrac.getPreventiveState()).size());
        assertSame(simpleCrac.getCnecs(simpleCrac.getPreventiveState()).iterator().next().getState(), simpleCrac.getPreventiveState());

        Cnec cnec2 = new SimpleCnec(
                "cnec2",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        simpleCrac.addCnec(cnec2);
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("after-co"));
        assertNotNull(simpleCrac.getState("co", "after-co"));
        assertSame(simpleCrac.getCnecs(simpleCrac.getState("co", "after-co")).iterator().next().getState(), simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
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

        Cnec cnec = new SimpleCnec(
                "cnec2",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        simpleCrac.addCnec(cnec);
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getInstant("after-co"));
        assertNotNull(simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
        assertSame(
                simpleCrac.getCnecs(simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co"))).iterator().next().getState(),
                simpleCrac.getState(simpleCrac.getContingency("co"), simpleCrac.getInstant("after-co")));
    }

    @Test
    public void testAddCnecWithTwoIdenticalCnecs() {
        Cnec cnec1 = new SimpleCnec(
                "cnec1",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        Cnec cnec2 = new SimpleCnec(
                "cnec1",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        assertEquals(0, simpleCrac.getCnecs().size());
        simpleCrac.addCnec(cnec1);
        assertEquals(1, simpleCrac.getCnecs().size());
        simpleCrac.addCnec(cnec2);
        assertEquals(1, simpleCrac.getCnecs().size());
    }

    @Test
    public void testAddRangeActionWithNoConflict() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.getId()).thenReturn("instantid");
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());
        Mockito.when(rangeAction.getUsageRules()).thenReturn(Collections.singletonList(new FreeToUse(UsageMethod.AVAILABLE, state)));

        simpleCrac.addRangeAction(rangeAction);

        assertNotNull(simpleCrac.getPreventiveState());
        assertEquals(0, simpleCrac.getCnecs().size());
    }

    @Test
    public void synchronizeFailSecondTime() {
        Network network = Mockito.mock(Network.class);
        simpleCrac.synchronize(network);
        try {
            simpleCrac.synchronize(network);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void synchronizeThenDesynchronizeThenSynchronizeAgain() {
        Network network = Mockito.mock(Network.class);
        simpleCrac.synchronize(network);
        simpleCrac.desynchronize();
        try {
            simpleCrac.synchronize(network);
        } catch (FaraoException e) {
            fail();
        }
    }
}
