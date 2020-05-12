/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.range_domain.Range;
import com.farao_community.farao.data.crac_impl.range_domain.RangeType;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.RelativeFlowThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_api.Direction.OPPOSITE;
import static com.farao_community.farao.data.crac_api.Side.LEFT;
import static org.junit.Assert.*;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracFileTest {
    private SimpleCrac crac;
    private static final Logger LOGGER = LoggerFactory.getLogger(CracFileTest.class);

    @Before
    public void setUp() {
        crac = new SimpleCrac("test-crac");
    }

    @Test
    public void testAddNetworkElementWithIdAndName() {
        NetworkElement networkElement = crac.addNetworkElement("neID", "neName");
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement, crac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithIdAndNameFail() {
        crac.addNetworkElement("neID", "neName");
        try {
            crac.addNetworkElement("neID", "neName-fail");
            fail();
        } catch (FaraoException e) {
            assertEquals("A network element with the same ID (neID) but a different name already exists.", e.getMessage());
        }
    }

    @Test
    public void testAddNetworkElementWithIdAndNameTwice() {
        NetworkElement networkElement1 = crac.addNetworkElement("neID", "neName");
        NetworkElement networkElement2 = crac.addNetworkElement("neID", "neName");
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement1, networkElement2);
        assertSame(networkElement1, crac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithId() {
        NetworkElement networkElement = crac.addNetworkElement("neID");
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement, crac.getNetworkElement("neID"));
    }

    @Test
    public void testAddNetworkElementWithNetworkElement() {
        NetworkElement networkElement = crac.addNetworkElement(new NetworkElement("neID"));
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement, crac.getNetworkElement("neID"));
    }

    @Test
    public void testGetInstant() {
        assertEquals(0, crac.getInstants().size());
    }

    @Test
    public void testAddInstantWithId() {
        crac.addInstant("initial-instant", 0);
        assertEquals(1, crac.getInstants().size());
        assertNotNull(crac.getInstant("initial-instant"));
        assertEquals(0, crac.getInstant("initial-instant").getSeconds());
    }

    @Test
    public void testAddInstantWithInstant() {
        assertEquals(0, crac.getInstants().size());
        crac.addInstant(new Instant("initial-instant", 0));
        assertEquals(1, crac.getInstants().size());
        assertNotNull(crac.getInstant("initial-instant"));
        try {
            crac.addInstant(new Instant("initial-instant", 12));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same ID but different seconds already exists.", e.getMessage());
        }
        try {
            crac.addInstant(new Instant("fail-initial", 0));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same seconds but different ID already exists.", e.getMessage());
        }
        assertEquals(1, crac.getInstants().size());
        crac.addInstant(new Instant("curative", 60));
        assertEquals(2, crac.getInstants().size());
        assertNotNull(crac.getInstant("curative"));
    }

    @Test
    public void testGetContingency() {
        assertEquals(0, crac.getContingencies().size());
    }

    @Test
    public void testAddContingencyWithElements() {
        crac.addContingency("contingency-1", "ne1", "ne2");
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-1"));
        try {
            crac.addContingency("contingency-1", "ne2");
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID (contingency-1) but a different network elements already exists.", e.getMessage());
        }
        try {
            crac.addContingency("contingency-2", "ne1");
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, crac.getContingencies().size());
        crac.addContingency("contingency-3", "ne3");
        assertEquals(3, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-3"));
        assertNull(crac.getContingency("contingency-fail"));
    }

    @Test
    public void testAddContingency() {
        assertEquals(0, crac.getContingencies().size());
        crac.addContingency(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne1"))));
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-1"));
        try {
            crac.addContingency(new ComplexContingency("contingency-1", Collections.singleton(new NetworkElement("ne2"))));
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID (contingency-1) but a different network elements already exists.", e.getMessage());
        }
        try {
            crac.addContingency(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("ne1"))));
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, crac.getContingencies().size());
        crac.addContingency(new ComplexContingency("contingency-3", Collections.singleton(new NetworkElement("ne3"))));
        assertEquals(3, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-3"));
        assertNull(crac.getContingency("contingency-fail"));
    }

    @Test
    public void addStatesWithIdFail() {
        try {
            crac.addState("contingency", "instant");
            fail();
        } catch (FaraoException e) {
            // must throw
        }
    }

    @Test
    public void addStatesWithId() {
        crac.addContingency("contingency", "neID");
        crac.addInstant("instant", 5);
        crac.addState("contingency", "instant");
        assertNotNull(crac.getState("contingency-instant"));
    }

    @Test
    public void addStatesWithIdPreventive() {
        crac.addInstant("instant", 0);
        crac.addState(null, "instant");
        assertNotNull(crac.getState("none-instant"));
    }

    @Test
    public void addStatesWithIdPreventiveFail() {
        try {
            crac.addState(null, "instant");
            fail();
        } catch (FaraoException e) {
            // must throw
        }
    }

    @Test
    public void addStatesWithObjectFail() {
        try {
            crac.addState(new ComplexContingency(
                "contingency", Collections.singleton(crac.addNetworkElement("neID"))
            ), new Instant("instant", 5));
            fail();
        } catch (FaraoException e) {
            // must throw
        }
    }

    @Test
    public void addStatesWithObject() {
        Contingency contingency = crac.addContingency("contingency", "neID");
        Instant instant = crac.addInstant("instant", 5);
        crac.addState(contingency, instant);
        assertNotNull(crac.getState("contingency-instant"));
    }

    @Test
    public void addStatesWithObjectPreventive() {
        Instant instant = crac.addInstant("instant", 0);
        crac.addState(null, instant);
        assertNotNull(crac.getState("none-instant"));
    }

    @Test
    public void addStatesWithObjectPreventiveFail() {
        try {
            crac.addState(null, new Instant("instant", 5));
            fail();
        } catch (FaraoException e) {
            // must throw
        }
    }

    @Test
    public void testStates() {
        assertNull(crac.getPreventiveState());
        assertEquals(0, crac.getContingencies().size());
        assertEquals(0, crac.getInstants().size());

        crac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 0)));
        assertNotNull(crac.getPreventiveState());
        assertEquals("initial-instant", crac.getPreventiveState().getInstant().getId());

        assertEquals(crac.getInstant("initial-instant"), crac.getPreventiveState().getInstant());

        crac.addState(new SimpleState(
                Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
                new Instant("after-contingency", 60))
        );

        try {
            crac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant-fail", 0)));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same seconds but different ID already exists.", e.getMessage());
        }

        try {
            crac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 12)));
            fail();
        } catch (FaraoException e) {
            assertEquals("An instant with the same ID but different seconds already exists.", e.getMessage());
        }

        assertEquals(2, crac.getInstants().size());
        assertEquals(2, crac.getStates().size());
        assertEquals(1, crac.getContingencies().size());

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        assertEquals(2, crac.getStatesFromInstant("after-contingency").size());

        // Different states pointing at the same instant object
        Instant instant = crac.getInstant("after-contingency");
        crac.getStates(instant).forEach(state -> assertSame(instant, state.getInstant()));

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        // Different states pointing at the same contingency object
        Contingency contingency = crac.getContingency("contingency-2");
        assertEquals(2, crac.getStates(contingency).size());
        crac.getStates(contingency).forEach(state -> {
            assertTrue(state.getContingency().isPresent());
            assertSame(contingency, state.getContingency().get());
        }
        );

        State testState = crac.getState(contingency, instant);
        assertTrue(testState.getContingency().isPresent());
        assertSame(testState.getContingency().get(), contingency);
        assertSame(testState.getInstant(), instant);
    }

    @Test
    public void testGetStatesWithPreventiveInstantId() {
        assertEquals(0, crac.getStatesFromInstant("initial-instant").size());

        crac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 0)));
        assertNotNull(crac.getStatesFromInstant("initial-instant"));
        assertEquals(1, crac.getStatesFromInstant("initial-instant").size());
        assertSame(crac.getStatesFromInstant("initial-instant").iterator().next(), crac.getPreventiveState());
    }

    @Test
    public void testGetStatesWithInstantIds() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        assertEquals(2, crac.getStatesFromInstant("after-contingency").size());
        assertEquals(1, crac.getStatesFromInstant("after-contingency-bis").size());
    }

    @Test
    public void testGetStatesWithContingencyIds() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        assertEquals(1, crac.getStatesFromContingency("contingency").size());
        assertEquals(2, crac.getStatesFromContingency("contingency-2").size());
    }

    @Test
    public void testGetStateWithIds() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNotNull(crac.getState("contingency", "after-contingency"));
    }

    @Test
    public void testGetStateWithNotExistingContingencyId() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNull(crac.getState("fail-contingency", "after-contingency"));
    }

    @Test
    public void testGetStateWithNotExistingInstantId() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency", Collections.singleton(new NetworkElement("network-element")))),
            new Instant("after-contingency", 60))
        );

        assertNull(crac.getState("contingency", "fail-after-contingency"));
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

        crac.addCnec(cnec);

        assertEquals(1, crac.getCnecs("co", "after-co").size());
        Cnec getCnec = crac.getCnecs("co", "after-co").iterator().next();
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

        crac.addState(state3);
        crac.addState(state1);
        crac.addState(state2);

        Iterator<State> states = crac.getStatesFromContingency("contingency-1").iterator();
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

        crac.addState(state4);

        states = crac.getStatesFromContingency("contingency-1").iterator();
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

        crac.addCnec(cnec1);
        assertEquals(0, crac.getContingencies().size());
        assertNotNull(crac.getInstant("initial-instant"));
        assertNotNull(crac.getPreventiveState());
        assertEquals(1, crac.getCnecs(crac.getPreventiveState()).size());
        assertSame(crac.getCnecs(crac.getPreventiveState()).iterator().next().getState(), crac.getPreventiveState());

        Cnec cnec2 = new SimpleCnec(
                "cnec2",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        crac.addCnec(cnec2);
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getInstant("after-co"));
        assertNotNull(crac.getState("co", "after-co"));
        assertSame(crac.getCnecs(crac.getState("co", "after-co")).iterator().next().getState(), crac.getState(crac.getContingency("co"), crac.getInstant("after-co")));
    }

    @Test
    public void testAddCnecWithAlreadyExistingState() {
        crac.addState(new SimpleState(
            Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
            new Instant("after-co", 60)
        ));

        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getInstant("after-co"));
        assertNotNull(crac.getState(crac.getContingency("co"), crac.getInstant("after-co")));

        Cnec cnec = new SimpleCnec(
                "cnec2",
                new NetworkElement("network-element-1"),
            Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, OPPOSITE, 1000.)),
                new SimpleState(
                    Optional.of(new ComplexContingency("co", Collections.singleton(new NetworkElement("network-element-2")))),
                    new Instant("after-co", 60)
                )
        );

        crac.addCnec(cnec);
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getInstant("after-co"));
        assertNotNull(crac.getState(crac.getContingency("co"), crac.getInstant("after-co")));
        assertSame(
                crac.getCnecs(crac.getState(crac.getContingency("co"), crac.getInstant("after-co"))).iterator().next().getState(),
                crac.getState(crac.getContingency("co"), crac.getInstant("after-co")));
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

        assertEquals(0, crac.getCnecs().size());
        crac.addCnec(cnec1);
        assertEquals(1, crac.getCnecs().size());
        crac.addCnec(cnec2);
        assertEquals(1, crac.getCnecs().size());
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

        crac.addRangeAction(rangeAction);

        assertNotNull(crac.getPreventiveState());
        assertEquals(0, crac.getCnecs().size());
    }

    @Test
    public void synchronizeFailSecondTime() {
        Network network = Mockito.mock(Network.class);
        crac.synchronize(network);
        try {
            crac.synchronize(network);
            fail();
        } catch (FaraoException e) {
            // should throw
        }
    }

    @Test
    public void synchronizeThenDesynchronizeThenSynchronizeAgain() {
        Network network = Mockito.mock(Network.class);
        crac.synchronize(network);
        crac.desynchronize();
        try {
            crac.synchronize(network);
        } catch (FaraoException e) {
            fail();
        }
    }

    @Test
    public void generateValidityReport() {
        Network network = NetworkImportsUtil.import12NodesNetwork();

        SimpleCrac simpleCrac = new SimpleCrac("cracId");

        Contingency contingency = simpleCrac.addContingency("contingencyId", "FFR1AA1  FFR2AA1  1");
        simpleCrac.addContingency("contingency2Id", "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE3AA1  1");
        simpleCrac.addContingency("contThatShouldBeRemoved", "element that does not exist");

        Instant initialInstant = simpleCrac.addInstant("N", 0);
        Instant outageInstant = simpleCrac.addInstant("postContingencyId", 5);

        State preventiveState = simpleCrac.addState(null, initialInstant);
        State postContingencyState = simpleCrac.addState(contingency, outageInstant);
        State stateThatShouldBeRemoved = simpleCrac.addState("contThatShouldBeRemoved", "postContingencyId");

        simpleCrac.addNetworkElement("neId1");
        simpleCrac.addNetworkElement("neId2");
        simpleCrac.addNetworkElement(new NetworkElement("pst"));

        simpleCrac.addCnec("cnec1prev", "FFR1AA1  FFR2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), preventiveState.getId());
        simpleCrac.addCnec("cnec2prev", "neId2", Collections.singleton(new RelativeFlowThreshold(Side.LEFT, Direction.OPPOSITE, 30)), preventiveState.getId());
        simpleCrac.addCnec("cnec1cur", "neId1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 800)), postContingencyState.getId());
        simpleCrac.addCnec("cnec3cur", "BBE1AA1  BBE2AA1  1", Collections.singleton(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.OPPOSITE, 500)), stateThatShouldBeRemoved.getId());

        Topology topology1 = new Topology(
                "topologyId1",
                "topologyName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("neId1"),
                ActionType.CLOSE
        );
        Topology topology2 = new Topology(
                "topologyId2",
                "topologyName",
                "RTE",
                new ArrayList<>(),
                simpleCrac.getNetworkElement("FFR1AA1  FFR2AA1  1"),
                ActionType.CLOSE
        );
        PstWithRange pstWithRange = new PstWithRange(
                "pstRangeId",
                "pstRangeName",
                "RTE",
                Collections.singletonList(new FreeToUse(UsageMethod.AVAILABLE, preventiveState)),
                Arrays.asList(new Range(0, 16, RangeType.ABSOLUTE_FIXED, RangeDefinition.STARTS_AT_ONE)),
                simpleCrac.getNetworkElement("pst")
        );

        simpleCrac.addNetworkAction(topology1);
        simpleCrac.addNetworkAction(topology2);
        simpleCrac.addRangeAction(pstWithRange);

        assertEquals(4, simpleCrac.getCnecs().size());
        assertEquals(2, simpleCrac.getNetworkActions().size());
        assertEquals(1, simpleCrac.getRangeActions().size());
        assertEquals(3, simpleCrac.getContingencies().size());
        assertEquals(3, simpleCrac.getStates().size());

        simpleCrac.generateValidityReport(network);

        assertEquals(1, simpleCrac.getCnecs().size());
        assertEquals(1, simpleCrac.getNetworkActions().size());
        assertEquals(0, simpleCrac.getRangeActions().size());
        assertEquals(2, simpleCrac.getContingencies().size());
        assertEquals(2, simpleCrac.getStates().size());
    }
}
