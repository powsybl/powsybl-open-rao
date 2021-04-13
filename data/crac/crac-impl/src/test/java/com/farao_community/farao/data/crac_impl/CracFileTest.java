/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Contingency;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.data.crac_api.Instant.CURATIVE;
import static com.farao_community.farao.data.crac_api.Instant.OUTAGE;
import static org.junit.Assert.*;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracFileTest {
    private CracImpl simpleCrac;

    @Before
    public void setUp() {
        simpleCrac = new CracImpl("test-crac");
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
    public void testGetContingency() {
        assertEquals(0, simpleCrac.getContingencies().size());
    }

    @Test
    public void testAddContingency() {
        assertEquals(0, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ContingencyImpl("contingency-1", Collections.singleton(new NetworkElementImpl("ne1"))));
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-1"));
        try {
            simpleCrac.addContingency(new ContingencyImpl("contingency-1", Collections.singleton(new NetworkElementImpl("ne2"))));
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID (contingency-1) but a different network elements already exists.", e.getMessage());
        }
        try {
            simpleCrac.addContingency(new ContingencyImpl("contingency-2", Collections.singleton(new NetworkElementImpl("ne1"))));
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ContingencyImpl("contingency-3", Collections.singleton(new NetworkElementImpl("ne3"))));
        assertEquals(3, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-3"));
        assertNull(simpleCrac.getContingency("contingency-fail"));
    }

    @Test
    public void testStatesAndInstantsInitialization() {
        assertEquals(0, simpleCrac.getContingencies().size());
        assertEquals(0, simpleCrac.getStates().size());
    }

    @Test(expected = FaraoException.class)
    public void testGetStateWithNotExistingContingencyId() {
        assertNull(simpleCrac.getState("fail-contingency", CURATIVE));
    }

    @Test
    public void testGetStateWithNotExistingContingency() {
        Contingency contingency = new ContingencyImpl("co", Set.of(new NetworkElementImpl("ne")));
        assertNull(simpleCrac.getState(contingency, CURATIVE));
    }

    @Test(expected = FaraoException.class)
    public void testAddCnecFromDataFailsBecauseOfNetworkElement() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addContingency(co);
        simpleCrac.addCnec(
                "cnec-id",
                "ne",
                "operator",
                Set.of(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                co,
                OUTAGE);
    }

    @Test(expected = FaraoException.class)
    public void testAddCnecFromDataFailsBecauseOfContingency() {
        simpleCrac.addNetworkElement("ne");
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addCnec(
                "cnec-id",
                "ne",
                "operator",
                Set.of(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                co,
                OUTAGE);
    }

    @Test
    public void testAddCnecFromData() {
        simpleCrac.addNetworkElement("ne");
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addContingency(co);
        BranchCnec cnec = simpleCrac.addCnec(
                "cnec-id",
                "ne",
                "operator",
                Set.of(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                co,
                OUTAGE);

        assertNotNull(simpleCrac.getState(co, OUTAGE));
        assertNotNull(simpleCrac.getBranchCnec("cnec-id"));
        assertSame(simpleCrac.getNetworkElement("ne"), cnec.getNetworkElement());
        assertSame(simpleCrac.getState(co, OUTAGE), cnec.getState());
    }

    @Test(expected = FaraoException.class)
    public void testAddPreventiveCnecFromDataFailsBecauseOfNetworkElement() {
        simpleCrac.addPreventiveCnec(
                "cnec-id",
                "ne",
                "operator",
                Set.of(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)));
    }

    @Test
    public void testAddPreventiveCnecFromData() {
        simpleCrac.addNetworkElement("ne");
        BranchCnec cnec = simpleCrac.addPreventiveCnec(
                "cnec-id",
                "ne",
                "operator",
                Set.of(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)));

        assertNotNull(simpleCrac.getPreventiveState());
        assertNotNull(simpleCrac.getBranchCnec("cnec-id"));
        assertSame(simpleCrac.getNetworkElement("ne"), cnec.getNetworkElement());
        assertSame(simpleCrac.getPreventiveState(), cnec.getState());
    }

    @Test(expected = FaraoException.class)
    public void testAddCnecFromExistingCnecFailsBecauseOfContingency() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        State state = new PostContingencyState(co, OUTAGE);
        BranchCnec cnec = new FlowCnecImpl(
                "cnec",
                new NetworkElementImpl("ne"),
                "operator",
                state,
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        simpleCrac.addCnec(cnec);
    }

    @Test
    public void testAddCnecFromExistingCnec() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addContingency(co);
        State state = new PostContingencyState(co, OUTAGE);
        BranchCnec cnec = new FlowCnecImpl(
                "cnec-id",
                new NetworkElementImpl("ne"),
                "operator",
                state,
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        simpleCrac.addCnec(cnec);

        assertNotNull(simpleCrac.getState(co, OUTAGE));
        assertNotNull(simpleCrac.getNetworkElement("ne"));
        assertNotNull(simpleCrac.getBranchCnec("cnec-id"));
        assertSame(simpleCrac.getNetworkElement("ne"), simpleCrac.getBranchCnec("cnec-id").getNetworkElement());
        assertSame(simpleCrac.getState(co, OUTAGE), simpleCrac.getBranchCnec("cnec-id").getState());
    }

    @Test
    public void testAddPreventiveCnecFromExistingCnec() {
        State state = new PreventiveState();
        BranchCnec cnec = new FlowCnecImpl(
                "cnec-id",
                new NetworkElementImpl("ne"),
                "operator",
                state,
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        simpleCrac.addCnec(cnec);

        assertNotNull(simpleCrac.getPreventiveState());
        assertNotNull(simpleCrac.getNetworkElement("ne"));
        assertNotNull(simpleCrac.getBranchCnec("cnec-id"));
        assertSame(simpleCrac.getNetworkElement("ne"), simpleCrac.getBranchCnec("cnec-id").getNetworkElement());
        assertSame(simpleCrac.getPreventiveState(), simpleCrac.getBranchCnec("cnec-id").getState());
    }

    @Test
    public void testAddCnecWithAlreadyExistingState() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addContingency(co);
        Contingency coSame = new ContingencyImpl("co", Set.of(new NetworkElementImpl("ne-co")));
        BranchCnec cnec = new FlowCnecImpl(
                "cnec-id",
                new NetworkElementImpl("ne"),
                "operator",
                new PostContingencyState(coSame, OUTAGE),
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        simpleCrac.addCnec(cnec);

        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getState("co", OUTAGE));
        assertSame(simpleCrac.getBranchCnec("cnec-id").getState(), simpleCrac.getState("co", OUTAGE));
    }

    @Test
    public void testAddCnecWithTwoIdenticalCnecs() {
        Contingency co = new ContingencyImpl("co", Collections.singleton(new NetworkElementImpl("ne-co")));
        simpleCrac.addContingency(co);
        BranchCnec cnec1 = new FlowCnecImpl(
                "cnec-id",
                new NetworkElementImpl("ne"),
                "operator",
                new PostContingencyState(simpleCrac.getContingency("co"), OUTAGE),
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );
        BranchCnec cnec2 = new FlowCnecImpl(
                "cnec-id",
                new NetworkElementImpl("ne"),
                "operator",
                new PostContingencyState(simpleCrac.getContingency("co"), OUTAGE),
                true, false,
                Collections.singleton(new BranchThresholdImpl(Unit.AMPERE, -1000., null, BranchThresholdRule.ON_LEFT_SIDE)),
                0.
        );

        assertEquals(0, simpleCrac.getFlowCnecs().size());
        simpleCrac.addCnec(cnec1);
        assertEquals(1, simpleCrac.getFlowCnecs().size());
        simpleCrac.addCnec(cnec2);
        assertEquals(1, simpleCrac.getFlowCnecs().size());
    }

    @Test
    public void testAddRangeActionWithNoConflict() {
        RangeAction rangeAction = Mockito.mock(RangeAction.class);
        State state = Mockito.mock(State.class);
        Instant instant = Mockito.mock(Instant.class);
        Mockito.when(instant.toString()).thenReturn("preventive");
        Mockito.when(state.getInstant()).thenReturn(instant);
        Mockito.when(state.getContingency()).thenReturn(Optional.empty());

        simpleCrac.addRangeAction(rangeAction);

        assertEquals(0, simpleCrac.getFlowCnecs().size());
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
