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
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnecAdder;
import com.farao_community.farao.data.crac_api.network_action.ActionType;
import com.farao_community.farao.data.crac_api.network_action.ElementaryAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkActionAdder;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.range_action.PstRangeActionAdder;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_api.usage_rule.UsageRule;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.*;
import static org.junit.Assert.*;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracImplTest {
    private CracImpl crac;

    @Before
    public void setUp() {
        crac = new CracImpl("test-crac");
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
    public void testGetContingency() {
        assertEquals(0, crac.getContingencies().size());
    }

    @Test
    public void testAddContingency() {
        assertEquals(0, crac.getContingencies().size());
        crac.addContingency(new ContingencyImpl("contingency-1", "co-name", Collections.singleton(new NetworkElementImpl("ne1"))));
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-1"));
        crac.addContingency(new ContingencyImpl("contingency-2", "co-name", Collections.singleton(new NetworkElementImpl("ne1"))));
        assertEquals(2, crac.getContingencies().size());
        crac.addContingency(new ContingencyImpl("contingency-3", "co-name", Collections.singleton(new NetworkElementImpl("ne3"))));
        assertEquals(3, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-3"));
        assertNull(crac.getContingency("contingency-fail"));
    }

    @Test
    public void testStatesAndInstantsInitialization() {
        assertEquals(0, crac.getContingencies().size());
        assertEquals(0, crac.getStates().size());
    }

    @Test(expected = FaraoException.class)
    public void testGetStateWithNotExistingContingencyId() {
        crac.getState("fail-contingency", CURATIVE);
    }

    @Test
    public void testGetStateWithNotExistingContingency() {
        Contingency contingency = new ContingencyImpl("co", "co-name", Set.of(new NetworkElementImpl("ne")));
        assertNull(crac.getState(contingency, CURATIVE));
    }

    @Test
    public void testAddFlowCnecFromExistingCnec() {
        Contingency co = new ContingencyImpl("co", "co", Collections.singleton(new NetworkElementImpl("ne-co")));
        crac.addContingency(co);
        State state = new PostContingencyState(co, OUTAGE);
        FlowCnec cnec = new FlowCnecImpl(
                "cnec-id",
                "cnec-name",
                new NetworkElementImpl("ne"),
                "operator",
                state,
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        crac.addFlowCnec(cnec);
        assertNotNull(crac.getFlowCnec("cnec-id"));
        assertNotNull(crac.getCnec("cnec-id"));
    }

    @Test
    public void testAddPreventiveFlowCnecFromExistingCnec() {
        State state = new PreventiveState();
        FlowCnec cnec = new FlowCnecImpl(
                "cnec-id",
                "cnec-name",
                new NetworkElementImpl("ne"),
                "operator",
                state,
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        crac.addFlowCnec(cnec);
        assertNotNull(crac.getFlowCnec("cnec-id"));
        assertNotNull(crac.getCnec("cnec-id"));
    }

    @Test
    public void testAddFlowCnecWithTwoIdenticalCnecs() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        crac.addContingency(co);
        FlowCnec cnec1 = new FlowCnecImpl(
                "cnec-id",
                "cnec-name",
                new NetworkElementImpl("ne"),
                "operator",
                new PostContingencyState(crac.getContingency("co"), OUTAGE),
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        FlowCnec cnec2 = new FlowCnecImpl(
                "cnec-id",
                "cnec-name",
                new NetworkElementImpl("ne"),
                "operator",
                new PostContingencyState(crac.getContingency("co"), OUTAGE),
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );

        assertEquals(0, crac.getFlowCnecs().size());
        assertEquals(0, crac.getCnecs().size());
        crac.addFlowCnec(cnec1);
        assertEquals(1, crac.getFlowCnecs().size());
        assertEquals(1, crac.getCnecs().size());
        crac.addFlowCnec(cnec2);
        assertEquals(1, crac.getFlowCnecs().size());
        assertEquals(1, crac.getCnecs().size());
    }

    @Test
    public void testAddRangeActionWithNoConflict() {
        PstRangeAction rangeAction = Mockito.mock(PstRangeAction.class);
        Mockito.when(rangeAction.getId()).thenReturn("rangeAction");
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.toString()).thenReturn("preventive");
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        assertEquals(0, crac.getPstRangeActions().size());
        assertEquals(0, crac.getRangeActions().size());
        assertEquals(0, crac.getRemedialActions().size());
        crac.addPstRangeAction(rangeAction);
        assertEquals(1, crac.getPstRangeActions().size());
        assertEquals(1, crac.getRangeActions().size());
        assertEquals(1, crac.getRemedialActions().size());
        assertNotNull(crac.getRemedialAction("rangeAction"));
    }

    @Test
    public void testSynchronizeFlag() {
        assertFalse(crac.isSynchronized());
        Network network = Mockito.mock(Network.class);
        crac.synchronize(network);
        assertTrue(crac.isSynchronized());
    }

    @Test(expected = FaraoException.class)
    public void synchronizeFailSecondTime() {
        Network network = Mockito.mock(Network.class);
        crac.synchronize(network);
        crac.synchronize(network);
    }

    @Test(expected = Test.None.class)
    public void synchronizeThenDesynchronizeThenSynchronizeAgain() {
        Network network = Mockito.mock(Network.class);
        crac.synchronize(network);
        crac.desynchronize();
        crac.synchronize(network);
    }

    @Test
    public void testSafeRemoveNetworkElements() {
        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");
        NetworkElement ne3 = crac.addNetworkElement("ne3", "ne3");
        NetworkElement ne4 = crac.addNetworkElement("ne4", "ne4");
        NetworkElement ne5 = crac.addNetworkElement("ne5", "ne5");
        NetworkElement ne6 = crac.addNetworkElement("ne6", "ne6");
        NetworkElement ne7 = crac.addNetworkElement("ne7", "ne7");

        Contingency contingency = new ContingencyImpl("co", "co", Set.of(ne1, ne2));
        crac.addContingency(contingency);

        FlowCnec cnec = new FlowCnecImpl("cnec", "cnec", ne3, "operator", new PreventiveState(), false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec);

        ElementaryAction ea1 = new TopologicalActionImpl(ne4, ActionType.OPEN);
        ElementaryAction ea2 = new TopologicalActionImpl(ne5, ActionType.OPEN);
        NetworkAction na = new NetworkActionImpl("na", "na", "operator", Collections.emptyList(), Set.of(ea1, ea2));
        crac.addNetworkAction(na);
        assertNotNull(crac.getRemedialAction("na"));
        assertNotNull(crac.getNetworkAction("na"));

        assertEquals(7, crac.getNetworkElements().size());
        crac.safeRemoveNetworkElements(Set.of("ne1", "ne2", "ne3", "ne4", "ne5", "ne6"));
        assertEquals(6, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("ne1"));
        assertNotNull(crac.getNetworkElement("ne2"));
        assertNotNull(crac.getNetworkElement("ne3"));
        assertNotNull(crac.getNetworkElement("ne4"));
        assertNotNull(crac.getNetworkElement("ne5"));
        assertNull(crac.getNetworkElement("ne6"));
        assertNotNull(crac.getNetworkElement("ne7"));
    }

    @Test
    public void testSafeRemoveStates() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        Contingency contingency2 = new ContingencyImpl("co2", "co2", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency1, OUTAGE);
        State state3 = crac.addState(contingency2, CURATIVE);
        State state4 = crac.addState(contingency2, AUTO);
        State state5 = crac.addState(contingency2, OUTAGE);

        FlowCnec cnec = new FlowCnecImpl("cnec", "cnec", Mockito.mock(NetworkElement.class), "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec);

        UsageRule ur1 = new OnStateImpl(UsageMethod.AVAILABLE, state2);
        UsageRule ur2 = new OnStateImpl(UsageMethod.FORCED, state3);
        PstRangeAction ra = new PstRangeActionImpl("ra", "ra", "operator", List.of(ur1, ur2), Collections.emptyList(), Mockito.mock(NetworkElement.class), null);
        crac.addPstRangeAction(ra);
        assertNotNull(crac.getRemedialAction("ra"));
        assertNotNull(crac.getPstRangeAction("ra"));

        assertEquals(5, crac.getStates().size());
        crac.safeRemoveStates(Set.of(state1.getId(), state2.getId(), state3.getId(), state4.getId()));
        assertEquals(4, crac.getStates().size());
        assertNotNull(crac.getState(contingency1, CURATIVE));
        assertNotNull(crac.getState(contingency1, OUTAGE));
        assertNotNull(crac.getState(contingency2, CURATIVE));
        assertNull(crac.getState(contingency2, AUTO));
        assertNotNull(crac.getState(contingency2, OUTAGE));
    }

    @Test
    public void testContingencyAdder() {
        ContingencyAdder contingencyAdder = crac.newContingency();
        assertTrue(contingencyAdder instanceof ContingencyAdderImpl);
        assertSame(crac, ((ContingencyAdderImpl) contingencyAdder).owner);
    }

    @Test
    public void testRemoveContingency() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        Contingency contingency2 = new ContingencyImpl("co2", "co2", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);

        assertEquals(2, crac.getContingencies().size());
        crac.removeContingency("co2");
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("co1"));
        assertNull(crac.getContingency("co2"));
    }

    @Test
    public void testRemoveUsedContingencyError() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        FlowCnec cnec = new FlowCnecImpl("cnec", "cnec", Mockito.mock(NetworkElement.class), "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec);

        assertEquals(1, crac.getContingencies().size());
        try {
            crac.removeContingency("co1");
            fail();
        } catch (FaraoException e) {
            // expected behaviour
        }
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("co1"));
    }

    @Test
    public void testRemoveUsedContingencyError2() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        PstRangeAction ra = new PstRangeActionImpl("na", "na", "operator", List.of(new OnStateImpl(UsageMethod.AVAILABLE, state1)), Collections.emptyList(), Mockito.mock(NetworkElement.class), null);
        crac.addPstRangeAction(ra);

        assertEquals(1, crac.getContingencies().size());
        try {
            crac.removeContingency("co1");
            fail();
        } catch (FaraoException e) {
            // expected behaviour
        }
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("co1"));
    }

    @Test
    public void testPreventiveState() {
        assertNull(crac.getPreventiveState());
        crac.addPreventiveState();
        State state = crac.getPreventiveState();
        assertNotNull(state);
        assertEquals(PREVENTIVE, state.getInstant());
        assertTrue(state.getContingency().isEmpty());
        crac.addPreventiveState();
        assertSame(state, crac.getPreventiveState());
    }

    @Test
    public void testGetStatesFromContingency() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        Contingency contingency2 = new ContingencyImpl("co2", "co2", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency1, OUTAGE);
        State state3 = crac.addState(contingency2, CURATIVE);
        State state4 = crac.addState(contingency2, AUTO);
        State state5 = crac.addState(contingency2, OUTAGE);

        assertEquals(2, crac.getStates(contingency1).size());
        assertTrue(crac.getStates(contingency1).containsAll(Set.of(state1, state2)));
        assertEquals(3, crac.getStates(contingency2).size());
        assertTrue(crac.getStates(contingency2).containsAll(Set.of(state3, state4, state5)));
        Contingency contingency3 = new ContingencyImpl("co3", "co3", Collections.singleton(Mockito.mock(NetworkElement.class)));
        assertTrue(crac.getStates(contingency3).isEmpty());
    }

    @Test
    public void testGetStatesFromInstant() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        Contingency contingency2 = new ContingencyImpl("co2", "co2", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency1, OUTAGE);
        State state3 = crac.addState(contingency2, CURATIVE);
        State state4 = crac.addState(contingency2, AUTO);
        State state5 = crac.addState(contingency2, OUTAGE);

        assertEquals(2, crac.getStates(OUTAGE).size());
        assertTrue(crac.getStates(CURATIVE).containsAll(Set.of(state1, state3)));
        assertEquals(2, crac.getStates(OUTAGE).size());
        assertTrue(crac.getStates(OUTAGE).containsAll(Set.of(state2, state5)));
        assertEquals(1, crac.getStates(AUTO).size());
        assertTrue(crac.getStates(AUTO).contains(state4));
        assertTrue(crac.getStates(PREVENTIVE).isEmpty());
    }

    @Test(expected = FaraoException.class)
    public void testAddStateWithPreventiveError() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addState(contingency1, PREVENTIVE);
    }

    @Test
    public void testAddSameStateTwice() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency1, CURATIVE);
        assertSame(state1, state2);
    }

    @Test(expected = FaraoException.class)
    public void testAddStateBeforecontingencyError() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addState(contingency1, CURATIVE);
    }

    @Test
    public void testFlowCnecAdder() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
        assertTrue(flowCnecAdder instanceof FlowCnecAdderImpl);
        assertSame(crac, ((FlowCnecAdderImpl) flowCnecAdder).owner);
    }

    @Test
    public void testGetCnecsFromState() {
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(Mockito.mock(NetworkElement.class)));
        Contingency contingency2 = new ContingencyImpl("co2", "co2", Collections.singleton(Mockito.mock(NetworkElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency2, OUTAGE);

        FlowCnec cnec1 = new FlowCnecImpl("cnec1", "cnec", Mockito.mock(NetworkElement.class), "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec1);
        FlowCnec cnec2 = new FlowCnecImpl("cnec2", "cnec", Mockito.mock(NetworkElement.class), "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec2);
        FlowCnec cnec3 = new FlowCnecImpl("cnec3", "cnec", Mockito.mock(NetworkElement.class), "operator", state2, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec3);

        assertEquals(2, crac.getFlowCnecs(state1).size());
        assertEquals(2, crac.getCnecs(state1).size());
        assertTrue(crac.getFlowCnecs(state1).containsAll(Set.of(cnec1, cnec2)));
        assertTrue(crac.getCnecs(state1).containsAll(Set.of(cnec1, cnec2)));
        assertEquals(1, crac.getFlowCnecs(state2).size());
        assertEquals(1, crac.getCnecs(state2).size());
        assertTrue(crac.getFlowCnecs(state2).contains(cnec3));
        assertTrue(crac.getCnecs(state2).contains(cnec3));
    }

    @Test
    public void testRemoveCnec() {
        NetworkElement neCo = crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(neCo));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        State state2 = crac.addState(contingency1, OUTAGE);

        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");

        FlowCnec cnec1 = new FlowCnecImpl("cnec1", "cnec", ne1, "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec1);
        FlowCnec cnec2 = new FlowCnecImpl("cnec2", "cnec", ne1, "operator", state2, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec2);
        FlowCnec cnec3 = new FlowCnecImpl("cnec3", "cnec", ne2, "operator", state1, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec3);
        FlowCnec cnec4 = new FlowCnecImpl("cnec4", "cnec", ne2, "operator", state2, false, false, Collections.emptySet(), .0);
        crac.addFlowCnec(cnec4);

        assertEquals(4, crac.getFlowCnecs().size());
        crac.removeCnec("doesnt exist 1");
        crac.removeFlowCnec("doesnt exist 2");
        assertEquals(4, crac.getFlowCnecs().size());

        crac.removeCnec("cnec1");
        assertNull(crac.getCnec("cnec1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by cnec2
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by cnec3

        crac.removeFlowCnec("cnec2");
        assertNull(crac.getCnec("cnec2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by cnec3
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by cnec4

        crac.removeFlowCnec("cnec3");
        assertNull(crac.getCnec("cnec3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by cnec4
        assertNull(crac.getState(contingency1, CURATIVE)); // unused
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by cnec4

        crac.removeCnec("cnec4");
        assertEquals(0, crac.getFlowCnecs().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neCo"));
        assertEquals(0, crac.getStates().size());
    }

    @Test
    public void testRemoveRangeAction() {
        NetworkElement neCo = crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(neCo));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        UsageRule ur1 = new OnStateImpl(UsageMethod.AVAILABLE, state1);
        State state2 = crac.addState(contingency1, OUTAGE);
        UsageRule ur2 = new OnStateImpl(UsageMethod.FORCED, state2);

        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");

        PstRangeAction ra1 = new PstRangeActionImpl("ra1", "ra1", "operator", List.of(ur1), Collections.emptyList(), ne1, null);
        crac.addPstRangeAction(ra1);
        PstRangeAction ra2 = new PstRangeActionImpl("ra2", "ra2", "operator", List.of(ur2), Collections.emptyList(), ne1, null);
        crac.addPstRangeAction(ra2);
        PstRangeAction ra3 = new PstRangeActionImpl("ra3", "ra3", "operator", List.of(ur1), Collections.emptyList(), ne2, null);
        crac.addPstRangeAction(ra3);
        PstRangeAction ra4 = new PstRangeActionImpl("ra4", "ra4", "operator", List.of(ur2), Collections.emptyList(), ne2, null);
        crac.addPstRangeAction(ra4);

        assertEquals(0, crac.getRangeActions(state1, UsageMethod.FORCED).size());
        assertEquals(2, crac.getRangeActions(state1, UsageMethod.AVAILABLE).size());
        assertTrue(crac.getRangeActions(state1, UsageMethod.AVAILABLE).containsAll(Set.of(ra1, ra3)));
        assertEquals(2, crac.getRangeActions(state2, UsageMethod.FORCED).size());
        assertTrue(crac.getRangeActions(state2, UsageMethod.FORCED).containsAll(Set.of(ra2, ra4)));

        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(4, crac.getRangeActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removePstRangeAction("doesnt exist 2");
        assertEquals(4, crac.getPstRangeActions().size());

        crac.removeRemedialAction("ra1");
        assertNull(crac.getRemedialAction("ra1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by ra2
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by ra3

        crac.removePstRangeAction("ra2");
        assertNull(crac.getRangeAction("ra2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by ra3
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by RA4

        crac.removePstRangeAction("ra3");
        assertNull(crac.getPstRangeAction("ra3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by ra4
        assertNull(crac.getState(contingency1, CURATIVE)); // unused
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by ra4

        crac.removeRemedialAction("ra4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neCo"));
        assertEquals(0, crac.getStates().size());
    }

    @Test
    public void testRemoveNetworkAction() {
        NetworkElement neCo = crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new ContingencyImpl("co1", "co1", Collections.singleton(neCo));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, CURATIVE);
        UsageRule ur1 = new OnStateImpl(UsageMethod.AVAILABLE, state1);
        State state2 = crac.addState(contingency1, OUTAGE);
        UsageRule ur2 = new OnStateImpl(UsageMethod.FORCED, state2);

        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");

        ElementaryAction ea1 = new TopologicalActionImpl(ne1, ActionType.OPEN);
        ElementaryAction ea2 = new TopologicalActionImpl(ne2, ActionType.CLOSE);

        NetworkAction ra1 = new NetworkActionImpl("ra1", "ra1", "operator", List.of(ur1), Collections.singleton(ea1));
        crac.addNetworkAction(ra1);
        NetworkAction ra2 = new NetworkActionImpl("ra2", "ra2", "operator", List.of(ur2), Collections.singleton(ea1));
        crac.addNetworkAction(ra2);
        NetworkAction ra3 = new NetworkActionImpl("ra3", "ra3", "operator", List.of(ur1), Collections.singleton(ea2));
        crac.addNetworkAction(ra3);
        NetworkAction ra4 = new NetworkActionImpl("ra4", "ra4", "operator", List.of(ur2), Collections.singleton(ea2));
        crac.addNetworkAction(ra4);

        assertEquals(0, crac.getNetworkActions(state1, UsageMethod.FORCED).size());
        assertEquals(2, crac.getNetworkActions(state1, UsageMethod.AVAILABLE).size());
        assertTrue(crac.getNetworkActions(state1, UsageMethod.AVAILABLE).containsAll(Set.of(ra1, ra3)));
        assertEquals(2, crac.getNetworkActions(state2, UsageMethod.FORCED).size());
        assertTrue(crac.getNetworkActions(state2, UsageMethod.FORCED).containsAll(Set.of(ra2, ra4)));

        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removeNetworkAction("doesnt exist 2");
        assertEquals(4, crac.getNetworkActions().size());

        crac.removeRemedialAction("ra1");
        assertNull(crac.getRemedialAction("ra1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by ra2
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by ra3

        crac.removeNetworkAction("ra2");
        assertNull(crac.getNetworkAction("ra2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState(contingency1, CURATIVE)); // state1, still used by ra3
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by RA4

        crac.removeNetworkAction("ra3");
        assertNull(crac.getNetworkAction("ra3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by ra4
        assertNull(crac.getState(contingency1, CURATIVE)); // unused
        assertNotNull(crac.getState(contingency1, OUTAGE)); // state2, still used by ra4

        crac.removeRemedialAction("ra4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neCo"));
        assertEquals(0, crac.getStates().size());
    }

    @Test
    public void testPstRangeActionAdder() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
        assertTrue(pstRangeActionAdder instanceof PstRangeActionAdderImpl);
        assertSame(crac, ((PstRangeActionAdderImpl) pstRangeActionAdder).getCrac());
    }

    @Test
    public void testNetworkActionAdder() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction();
        assertTrue(networkActionAdder instanceof NetworkActionAdderImpl);
        assertSame(crac, ((NetworkActionAdderImpl) networkActionAdder).getCrac());
    }
}
