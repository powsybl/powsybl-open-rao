/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.*;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnecAdder;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import com.powsybl.openrao.data.cracapi.usagerule.UsageRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.powsybl.openrao.data.cracapi.usagerule.UsageMethod.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class CracImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String OUTAGE_INSTANT_ID = "outage";
    private static final String AUTO_INSTANT_ID = "auto";
    private static final String CURATIVE_INSTANT_ID = "curative";

    private CracImpl crac;
    private Instant preventiveInstant;
    private Instant outageInstant;
    private Instant autoInstant;
    private Instant curativeInstant;

    private ContingencyElementType getRandomTypeContingency() {
        return ContingencyElementType.LINE;
    }

    private ContingencyElement getRandomTypeContingencyElement(String id) {
        return new BranchContingency(id);
    }

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE)
            .newInstant(OUTAGE_INSTANT_ID, InstantKind.OUTAGE)
            .newInstant(AUTO_INSTANT_ID, InstantKind.AUTO)
            .newInstant(CURATIVE_INSTANT_ID, InstantKind.CURATIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
        outageInstant = crac.getInstant(OUTAGE_INSTANT_ID);
        autoInstant = crac.getInstant(AUTO_INSTANT_ID);
        curativeInstant = crac.getInstant(CURATIVE_INSTANT_ID);
    }

    @Test
    void testAddNetworkElementWithIdAndName() {
        NetworkElement networkElement = crac.addNetworkElement("neID", "neName");
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement, crac.getNetworkElement("neID"));
    }

    @Test
    void testAddNetworkElementWithIdAndNameFail() {
        crac.addNetworkElement("neID", "neName");
        try {
            crac.addNetworkElement("neID", "neName-fail");
            fail();
        } catch (OpenRaoException e) {
            assertEquals("A network element with the same ID (neID) but a different name already exists.", e.getMessage());
        }
    }

    @Test
    void testAddNetworkElementWithIdAndNameTwice() {
        NetworkElement networkElement1 = crac.addNetworkElement("neID", "neName");
        NetworkElement networkElement2 = crac.addNetworkElement("neID", "neName");
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neID"));
        assertSame(networkElement1, networkElement2);
        assertSame(networkElement1, crac.getNetworkElement("neID"));
    }

    @Test
    void testGetContingency() {
        assertEquals(0, crac.getContingencies().size());
    }

    @Test
    void testAddContingency() {
        assertEquals(0, crac.getContingencies().size());
        crac.addContingency(new Contingency("contingency-1", "co-name", Collections.singletonList(getRandomTypeContingencyElement("ne1"))));
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-1"));
        crac.addContingency(new Contingency("contingency-2", "co-name", Collections.singletonList(getRandomTypeContingencyElement("ne1"))));
        assertEquals(2, crac.getContingencies().size());
        crac.addContingency(new Contingency("contingency-3", "co-name", Collections.singletonList(getRandomTypeContingencyElement("ne3"))));
        assertEquals(3, crac.getContingencies().size());
        assertNotNull(crac.getContingency("contingency-3"));
        assertNull(crac.getContingency("contingency-fail"));
    }

    @Test
    void testStatesAndInstantsInitialization() {
        assertEquals(0, crac.getContingencies().size());
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testGetStateWithNotExistingContingencyId() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.getState("fail-contingency", curativeInstant));
        assertEquals("Contingency fail-contingency does not exist, as well as the related state.", exception.getMessage());
    }

    @Test
    void testGetStateWithNotExistingContingency() {
        Contingency contingency = crac.newContingency()
                .withId("co")
                .withName("co-name")
                .withContingencyElement("ne", getRandomTypeContingency())
                .add();

        assertNull(crac.getState(contingency, curativeInstant));
    }

    @Test
    void testGetCnecandGetFlowCnec() {

        crac.newContingency().withId("co").withContingencyElement("ne-co", getRandomTypeContingency()).add();
        crac.newFlowCnec()
                .withId("cnec-id")
                .withName("cnec-name")
                .withNetworkElement("ne")
                .withOperator("operator")
                .withOptimized(true)
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co")
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        assertNotNull(crac.getFlowCnec("cnec-id"));
        assertNotNull(crac.getCnec("cnec-id"));
    }

    @Test
    void testAddPstRangeActionWithNoConflict() {
        PstRangeAction rangeAction = Mockito.mock(PstRangeAction.class);
        when(rangeAction.getId()).thenReturn("rangeAction");
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());

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
    void testAddHvdcRangeActionWithNoConflict() {
        HvdcRangeAction rangeAction = Mockito.mock(HvdcRangeAction.class);
        when(rangeAction.getId()).thenReturn("rangeAction");
        State state = Mockito.mock(State.class);
        when(state.getContingency()).thenReturn(Optional.empty());

        assertEquals(0, crac.getHvdcRangeActions().size());
        assertEquals(0, crac.getRangeActions().size());
        assertEquals(0, crac.getRemedialActions().size());
        crac.addHvdcRangeAction(rangeAction);
        assertEquals(1, crac.getHvdcRangeActions().size());
        assertEquals(1, crac.getRangeActions().size());
        assertEquals(1, crac.getRemedialActions().size());
        assertNotNull(crac.getRemedialAction("rangeAction"));
    }

    @Test
    void testSafeRemoveNetworkElements() {

        crac.newContingency().withId("co").withContingencyElement("ne1", getRandomTypeContingency()).withContingencyElement("ne2", getRandomTypeContingency()).add();
        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("ne3")
                .withOperator("operator")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        crac.newNetworkAction()
                .withId("na")
                .withOperator("operator")
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("ne4").add()
                .newTopologicalAction().withActionType(ActionType.OPEN).withNetworkElement("ne5").add()
                .add();

        crac.addNetworkElement("ne6", "ne6");
        crac.addNetworkElement("ne7", "ne7");

        assertNotNull(crac.getRemedialAction("na"));
        assertNotNull(crac.getNetworkAction("na"));

        assertEquals(5, crac.getNetworkElements().size());
        crac.safeRemoveNetworkElements(Set.of("ne3", "ne4", "ne5", "ne6"));
        assertEquals(4, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("ne3"));
        assertNotNull(crac.getNetworkElement("ne4"));
        assertNotNull(crac.getNetworkElement("ne5"));
        assertNull(crac.getNetworkElement("ne6"));
        assertNotNull(crac.getNetworkElement("ne7"));
    }

    @Test
    void testSafeRemoveStates() {

        Contingency contingency1 = crac.newContingency()
                .withId("co1")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();

        Contingency contingency2 = crac.newContingency()
                .withId("co2")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();

        State state1 = crac.addState(contingency1, curativeInstant);
        State state2 = crac.addState(contingency1, autoInstant);
        State state3 = crac.addState(contingency2, curativeInstant);
        State state4 = crac.addState(contingency1, outageInstant);
        crac.addState(contingency2, outageInstant);

        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("anyNetworkElement")
                .withOperator("operator")
                .withContingency("co1")
                .withInstant(CURATIVE_INSTANT_ID)
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        crac.newNetworkAction()
                .withId("ra")
                .withOperator("operator")
                .newPstSetPoint().withNetworkElement("anyPst").withSetpoint(8).add()
                .newOnContingencyStateUsageRule()
                .withContingency("co1")
                .withInstant(AUTO_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .newOnContingencyStateUsageRule()
                .withContingency("co2")
                .withInstant(CURATIVE_INSTANT_ID)
                .withUsageMethod(FORCED)
                .add()
                .add();

        assertNotNull(crac.getRemedialAction("ra"));
        assertNotNull(crac.getNetworkAction("ra"));

        assertEquals(5, crac.getStates().size());
        crac.safeRemoveStates(Set.of(state1.getId(), state2.getId(), state3.getId(), state4.getId()));
        assertEquals(4, crac.getStates().size());
        assertNotNull(crac.getState(contingency1, curativeInstant));
        assertNotNull(crac.getState(contingency1, curativeInstant));
        assertNotNull(crac.getState(contingency2, curativeInstant));
        assertNull(crac.getState(contingency2, autoInstant));
        assertNotNull(crac.getState(contingency2, curativeInstant));
    }

    @Test
    void testContingencyAdder() {
        ContingencyAdder contingencyAdder = crac.newContingency();
        assertTrue(contingencyAdder instanceof ContingencyAdderImpl);
        assertSame(crac, ((ContingencyAdderImpl) contingencyAdder).owner);
    }

    @Test
    void testRemoveContingency() {

        crac.newContingency().withId("co1").withContingencyElement("ne1", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("ne2", getRandomTypeContingency()).add();

        assertEquals(2, crac.getContingencies().size());
        assertEquals(0, crac.getNetworkElements().size());
        crac.removeContingency("co2");
        assertEquals(1, crac.getContingencies().size());
        assertEquals(0, crac.getNetworkElements().size());
        assertNotNull(crac.getContingency("co1"));
        assertNull(crac.getContingency("co2"));
    }

    @Test
    void testRemoveUsedContingencyError() {

        crac.newContingency()
                .withId("co1")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();
        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("anyNetworkElement")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        assertEquals(1, crac.getContingencies().size());

        try {
            crac.removeContingency("co1");
            fail();
        } catch (OpenRaoException e) {
            // expected behaviour
        }
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("co1"));
    }

    @Test
    void testRemoveUsedContingencyError2() {

        crac.newContingency()
                .withId("co1")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();
        crac.newNetworkAction()
                .withId("na")
                .withOperator("operator")
                .newTopologicalAction().withNetworkElement("anyNetworkElement").withActionType(ActionType.CLOSE).add()
                .newOnContingencyStateUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("co1").withUsageMethod(AVAILABLE).add()
                .add();

        assertEquals(1, crac.getContingencies().size());
        try {
            crac.removeContingency("co1");
            fail();
        } catch (OpenRaoException e) {
            // expected behaviour
        }
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("co1"));
    }

    @Test
    void testPreventiveState() {
        assertNull(crac.getPreventiveState());
        crac.addPreventiveState();
        State state = crac.getPreventiveState();
        assertNotNull(state);
        assertEquals(preventiveInstant, state.getInstant());
        assertTrue(state.getContingency().isEmpty());
        crac.addPreventiveState();
        assertSame(state, crac.getPreventiveState());
    }

    @Test
    void testGetStatesFromContingency() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        Contingency contingency2 = new Contingency("co2", "co2", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, curativeInstant);
        State state2 = crac.addState(contingency1, outageInstant);
        State state3 = crac.addState(contingency2, curativeInstant);
        State state4 = crac.addState(contingency2, autoInstant);
        State state5 = crac.addState(contingency2, outageInstant);

        assertEquals(2, crac.getStates(contingency1).size());
        assertTrue(crac.getStates(contingency1).containsAll(Set.of(state1, state2)));
        assertEquals(3, crac.getStates(contingency2).size());
        assertTrue(crac.getStates(contingency2).containsAll(Set.of(state3, state4, state5)));
        Contingency contingency3 = new Contingency("co3", "co3", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        assertTrue(crac.getStates(contingency3).isEmpty());
    }

    @Test
    void testGetStatesFromInstant() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        Contingency contingency2 = new Contingency("co2", "co2", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State state1 = crac.addState(contingency1, curativeInstant);
        State state2 = crac.addState(contingency1, outageInstant);
        State state3 = crac.addState(contingency2, curativeInstant);
        State state4 = crac.addState(contingency2, autoInstant);
        State state5 = crac.addState(contingency2, outageInstant);

        assertEquals(2, crac.getStates(outageInstant).size());
        assertTrue(crac.getStates(curativeInstant).containsAll(Set.of(state1, state3)));
        assertEquals(2, crac.getStates(outageInstant).size());
        assertTrue(crac.getStates(outageInstant).containsAll(Set.of(state2, state5)));
        assertEquals(1, crac.getStates(autoInstant).size());
        assertTrue(crac.getStates(autoInstant).contains(state4));
        assertTrue(crac.getStates(preventiveInstant).isEmpty());
    }

    @Test
    void testAddStateWithPreventiveError() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        crac.addContingency(contingency1);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.addState(contingency1, preventiveInstant));
        assertEquals("Impossible to add a preventive state with a contingency.", exception.getMessage());
    }

    @Test
    void testAddSameStateTwice() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, curativeInstant);
        State state2 = crac.addState(contingency1, curativeInstant);
        assertSame(state1, state2);
    }

    @Test
    void testAddStateBeforecontingencyError() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.addState(contingency1, curativeInstant));
        assertEquals("Please add co1 to crac first.", exception.getMessage());
    }

    @Test
    void testFlowCnecAdder() {
        FlowCnecAdder flowCnecAdder = crac.newFlowCnec();
        assertTrue(flowCnecAdder instanceof FlowCnecAdderImpl);
        assertSame(crac, ((FlowCnecAdderImpl) flowCnecAdder).owner);
    }

    @Test
    void testGetCnecsFromState() {

        crac.newContingency()
                .withId("co1")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();
        crac.newContingency()
                .withId("co2")
                .withContingencyElement("anyNetworkElement", getRandomTypeContingency())
                .add();
        FlowCnec cnec1 = crac.newFlowCnec()
                .withId("cnec1")
                .withNetworkElement("anyNetworkElement")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();
        FlowCnec cnec2 = crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("anyNetworkElement")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();
        FlowCnec cnec3 = crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("anyNetworkElement")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co2")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        State state1 = crac.getState("co1", curativeInstant);
        State state2 = crac.getState("co2", outageInstant);

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
    void testRemoveCnec() {

        crac.newContingency()
                .withId("co1")
                .withContingencyElement("neCo", getRandomTypeContingency())
                .add();
        crac.newContingency()
                .withId("co2")
                .withContingencyElement("neCo", getRandomTypeContingency())
                .add();
        crac.newFlowCnec()
                .withId("cnec1")
                .withNetworkElement("ne1")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("ne1")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("ne2")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec4")
                .withNetworkElement("ne2")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(Side.LEFT).add()
                .add();

        assertEquals(4, crac.getFlowCnecs().size());
        crac.removeCnec("doesnt exist 1");
        crac.removeFlowCnec("doesnt exist 2");
        assertEquals(4, crac.getFlowCnecs().size());

        crac.removeCnec("cnec1");
        assertNull(crac.getCnec("cnec1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by cnec2
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by cnec3

        crac.removeFlowCnec("cnec2");
        assertNull(crac.getCnec("cnec2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by cnec3
        assertNotNull(crac.getState("co1", outageInstant)); // state2, still used by cnec4

        crac.removeFlowCnec("cnec3");
        assertNull(crac.getCnec("cnec3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by cnec4
        assertNull(crac.getState("co1", curativeInstant)); // unused
        assertNotNull(crac.getState("co1", outageInstant)); // state2, still used by cnec4

        crac.removeCnec("cnec4");
        assertEquals(0, crac.getFlowCnecs().size());
        assertEquals(0, crac.getNetworkElements().size());
        assertEquals(1, crac.getContingency("co1").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(1, crac.getContingency("co2").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testRemovePstRangeAction() {

        crac.newContingency().withId("co1").withContingencyElement("neCo", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("neCo", getRandomTypeContingency()).add();

        RemedialAction<?> ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();

        State state1 = crac.getState("co1", curativeInstant);
        State state2 = crac.getState("co2", curativeInstant);

        assertEquals(0, crac.getRangeActions(state1, FORCED).size());
        assertEquals(2, crac.getRangeActions(state1, UsageMethod.AVAILABLE).size());
        assertTrue(crac.getRangeActions(state1, UsageMethod.AVAILABLE).containsAll(Set.of(ra1, ra3)));
        assertEquals(2, crac.getRangeActions(state2, FORCED).size());
        assertTrue(crac.getRangeActions(state2, FORCED).containsAll(Set.of(ra2, ra4)));

        assertEquals(4, crac.getPstRangeActions().size());
        assertEquals(4, crac.getRangeActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removePstRangeAction("doesnt exist 2");
        assertEquals(4, crac.getPstRangeActions().size());

        crac.removeRemedialAction("ra1");
        assertNull(crac.getRemedialAction("ra1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by ra2
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by ra3

        crac.removePstRangeAction("ra2");
        assertNull(crac.getRangeAction("ra2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by ra3
        assertNotNull(crac.getState("co2", curativeInstant)); // state2, still used by RA4

        crac.removePstRangeAction("ra3");
        assertNull(crac.getPstRangeAction("ra3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by ra4
        assertNull(crac.getState("co1", curativeInstant)); // unused
        assertNotNull(crac.getState("co2", curativeInstant)); // state2, still used by ra4

        crac.removeRemedialAction("ra4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(0, crac.getNetworkElements().size());
        assertEquals(1, crac.getContingency("co1").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(1, crac.getContingency("co2").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testRemoveHvdcRangeAction() {

        crac.newContingency().withId("co1").withContingencyElement("neCo", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("neCo", getRandomTypeContingency()).add();

        RemedialAction<?> ra1 = crac.newHvdcRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra2 = crac.newHvdcRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra3 = crac.newHvdcRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra4 = crac.newHvdcRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();

        State state1 = crac.getState("co1", curativeInstant);
        State state2 = crac.getState("co2", curativeInstant);

        assertEquals(0, crac.getRangeActions(state1, FORCED).size());
        assertEquals(2, crac.getRangeActions(state1, UsageMethod.AVAILABLE).size());
        assertTrue(crac.getRangeActions(state1, UsageMethod.AVAILABLE).containsAll(Set.of(ra1, ra3)));
        assertEquals(2, crac.getRangeActions(state2, FORCED).size());
        assertTrue(crac.getRangeActions(state2, FORCED).containsAll(Set.of(ra2, ra4)));

        assertEquals(4, crac.getHvdcRangeActions().size());
        assertEquals(4, crac.getRangeActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removeHvdcRangeAction("doesnt exist 2");
        assertEquals(4, crac.getHvdcRangeActions().size());

        crac.removeRemedialAction("ra1");
        assertNull(crac.getRemedialAction("ra1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by ra2
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by ra3

        crac.removeHvdcRangeAction("ra2");
        assertNull(crac.getRangeAction("ra2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState("co1", curativeInstant)); // state1, still used by ra3
        assertNotNull(crac.getState("co2", curativeInstant)); // state2, still used by RA4

        crac.removeHvdcRangeAction("ra3");
        assertNull(crac.getHvdcRangeAction("ra3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by ra4
        assertNull(crac.getState("co1", curativeInstant)); // unused
        assertNotNull(crac.getState("co2", curativeInstant)); // state2, still used by ra4

        crac.removeRemedialAction("ra4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(0, crac.getNetworkElements().size());
        assertEquals(1, crac.getContingency("co1").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(1, crac.getContingency("co1").getElements().stream().filter(e -> Objects.equals(e.getId(), "neCo")).count());
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testFilterPstRangeActionUsageRules() {
        crac.newContingency().withId("co1").withContingencyElement("neCo", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("neCo", getRandomTypeContingency()).add();

        RemedialAction<?> ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        RemedialAction<?> ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();

        State state1 = crac.getState("co1", curativeInstant);
        State state2 = crac.getState("co2", curativeInstant);

        assertEquals(Set.of(ra1), crac.getRangeActions(state1, AVAILABLE));
        assertEquals(Set.of(ra3), crac.getRangeActions(state2, AVAILABLE));
        assertEquals(Set.of(ra2), crac.getRangeActions(state1, FORCED));
        assertEquals(Set.of(ra4), crac.getRangeActions(state2, FORCED));
        assertEquals(Set.of(ra1, ra2), crac.getRangeActions(state1, AVAILABLE, FORCED));
        assertEquals(Set.of(ra3, ra4), crac.getRangeActions(state2, AVAILABLE, FORCED));
    }

    @Test
    void testFilterHvdcRangeActionUsageRules() {
        crac.newContingency().withId("co1").withContingencyElement("neCo", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("neCo", getRandomTypeContingency()).add();

        RemedialAction<?> ra1 = crac.newHvdcRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra2 = crac.newHvdcRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra3 = crac.newHvdcRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(AVAILABLE).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        RemedialAction<?> ra4 = crac.newHvdcRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();

        State state1 = crac.getState("co1", curativeInstant);
        State state2 = crac.getState("co2", curativeInstant);

        assertEquals(Set.of(ra1), crac.getRangeActions(state1, AVAILABLE));
        assertEquals(Set.of(ra3), crac.getRangeActions(state2, AVAILABLE));
        assertEquals(Set.of(ra2), crac.getRangeActions(state1, FORCED));
        assertEquals(Set.of(ra4), crac.getRangeActions(state2, FORCED));
        assertEquals(Set.of(ra1, ra2), crac.getRangeActions(state1, AVAILABLE, FORCED));
        assertEquals(Set.of(ra3, ra4), crac.getRangeActions(state2, AVAILABLE, FORCED));
    }

    @Test
    void testRemoveNetworkAction() {
        NetworkElement neCo = crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(getRandomTypeContingencyElement("neCo")));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, curativeInstant);
        UsageRule ur1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, state1);
        State state2 = crac.addState(contingency1, outageInstant);
        UsageRule ur2 = new OnContingencyStateImpl(FORCED, state2);

        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");

        ElementaryAction ea1 = new TopologicalActionImpl(ne1, ActionType.OPEN);
        ElementaryAction ea2 = new TopologicalActionImpl(ne2, ActionType.CLOSE);

        NetworkAction ra1 = new NetworkActionImpl("ra1", "ra1", "operator", Set.of(ur1), Collections.singleton(ea1), 10, Collections.singleton(ne1));
        crac.addNetworkAction(ra1);
        NetworkAction ra2 = new NetworkActionImpl("ra2", "ra2", "operator", Set.of(ur2), Collections.singleton(ea1), 10, Collections.singleton(ne1));
        crac.addNetworkAction(ra2);
        NetworkAction ra3 = new NetworkActionImpl("ra3", "ra3", "operator", Set.of(ur1), Collections.singleton(ea2), 10, Collections.singleton(ne2));
        crac.addNetworkAction(ra3);
        NetworkAction ra4 = new NetworkActionImpl("ra4", "ra4", "operator", Set.of(ur2), Collections.singleton(ea2), 10, Collections.singleton(ne2));
        crac.addNetworkAction(ra4);

        assertTrue(crac.getNetworkActions(state1, FORCED).isEmpty());
        assertEquals(Set.of(ra1, ra3), crac.getNetworkActions(state1, AVAILABLE));
        assertEquals(Set.of(ra2, ra4), crac.getNetworkActions(state2, FORCED));
        assertEquals(Set.of(ra2, ra4), crac.getNetworkActions(state2, FORCED, AVAILABLE));

        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removeNetworkAction("doesnt exist 2");
        assertEquals(4, crac.getNetworkActions().size());

        crac.removeRemedialAction("ra1");
        assertNull(crac.getRemedialAction("ra1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by ra2
        assertNotNull(crac.getState(contingency1, curativeInstant)); // state1, still used by ra3

        crac.removeNetworkAction("ra2");
        assertNull(crac.getNetworkAction("ra2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState(contingency1, curativeInstant)); // state1, still used by ra3
        assertNotNull(crac.getState(contingency1, outageInstant)); // state2, still used by RA4

        crac.removeNetworkAction("ra3");
        assertNull(crac.getNetworkAction("ra3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by ra4
        assertNull(crac.getState(contingency1, curativeInstant)); // unused
        assertNotNull(crac.getState(contingency1, outageInstant)); // state2, still used by ra4

        crac.removeRemedialAction("ra4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neCo"));
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testFilterNetworkActionUsageRules() {
        NetworkElement neCo = crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(getRandomTypeContingencyElement("neCo")));
        crac.addContingency(contingency1);
        State state1 = crac.addState(contingency1, curativeInstant);
        UsageRule ur1 = new OnContingencyStateImpl(UsageMethod.AVAILABLE, state1);
        State state2 = crac.addState(contingency1, outageInstant);
        UsageRule ur2 = new OnContingencyStateImpl(FORCED, state2);
        UsageRule ur3 = new OnContingencyStateImpl(FORCED, state1);

        NetworkElement ne1 = crac.addNetworkElement("ne1", "ne1");
        NetworkElement ne2 = crac.addNetworkElement("ne2", "ne2");

        ElementaryAction ea1 = new TopologicalActionImpl(ne1, ActionType.OPEN);
        ElementaryAction ea2 = new TopologicalActionImpl(ne2, ActionType.CLOSE);

        NetworkAction ra1 = new NetworkActionImpl("ra1", "ra1", "operator", Set.of(ur1), Collections.singleton(ea1), 10, Collections.singleton(ne1));
        crac.addNetworkAction(ra1);
        NetworkAction ra2 = new NetworkActionImpl("ra2", "ra2", "operator", Set.of(ur2), Collections.singleton(ea1), 10, Collections.singleton(ne1));
        crac.addNetworkAction(ra2);
        NetworkAction ra3 = new NetworkActionImpl("ra3", "ra3", "operator", Set.of(ur3), Collections.singleton(ea2), 10, Collections.singleton(ne2));
        crac.addNetworkAction(ra3);
        NetworkAction ra4 = new NetworkActionImpl("ra4", "ra4", "operator", Set.of(ur2), Collections.singleton(ea2), 10, Collections.singleton(ne2));
        crac.addNetworkAction(ra4);

        assertEquals(Set.of(ra1), crac.getNetworkActions(state1, AVAILABLE));
        assertEquals(Set.of(), crac.getNetworkActions(state2, AVAILABLE));
        assertEquals(Set.of(ra3), crac.getNetworkActions(state1, FORCED));
        assertEquals(Set.of(ra2, ra4), crac.getNetworkActions(state2, FORCED));
        assertEquals(Set.of(ra1, ra3), crac.getNetworkActions(state1, AVAILABLE, FORCED));
        assertEquals(Set.of(ra2, ra4), crac.getNetworkActions(state2, AVAILABLE, FORCED));
    }

    @Test
    void testPstRangeActionAdder() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
        assertTrue(pstRangeActionAdder instanceof PstRangeActionAdderImpl);
        assertSame(crac, ((PstRangeActionAdderImpl) pstRangeActionAdder).getCrac());
    }

    @Test
    void testHvdcRangeActionAdder() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
        assertTrue(hvdcRangeActionAdder instanceof HvdcRangeActionAdderImpl);
        assertSame(crac, ((HvdcRangeActionAdderImpl) hvdcRangeActionAdder).getCrac());
    }

    @Test
    void testNetworkActionAdder() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction();
        assertTrue(networkActionAdder instanceof NetworkActionAdderImpl);
        assertSame(crac, ((NetworkActionAdderImpl) networkActionAdder).getCrac());
    }

    @Test
    void testNewInstantAlreadyDefined() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.newInstant(OUTAGE_INSTANT_ID, InstantKind.PREVENTIVE));
        assertEquals("Instant 'outage' is already defined", exception.getMessage());
    }

    @Test
    void testGetInstantNeverDefined() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.getInstant("never defined"));
        assertEquals("Instant 'never defined' has not been defined", exception.getMessage());
    }

    @Test
    void testGetInstantByKindWithOneInstantPerInstantKind() {
        assertEquals(preventiveInstant, crac.getPreventiveInstant());
        assertEquals(outageInstant, crac.getOutageInstant());
        assertEquals(autoInstant, crac.getInstant(InstantKind.AUTO));
        assertEquals(curativeInstant, crac.getInstant(InstantKind.CURATIVE));
    }

    @Test
    void testGetInstants() {
        assertEquals(List.of(preventiveInstant, outageInstant, autoInstant, curativeInstant), crac.getSortedInstants());
    }

    @Test
    void testGetLastInstant() {
        assertEquals(curativeInstant, crac.getLastInstant());
    }

    @Test
    void testGetLastInstantWithMultipleInstantsPerInstantKind() {
        crac.newInstant("curative 2", InstantKind.CURATIVE)
            .newInstant("curative 3", InstantKind.CURATIVE);
        Instant curativeInstant3 = crac.getInstant("curative 3");
        assertEquals(curativeInstant3, crac.getLastInstant());
    }

    @Test
    void testGetInstantsByKindWithTwoInstantsPerInstantKind() {
        crac.newInstant("preventive 2", InstantKind.PREVENTIVE)
            .newInstant("outage 2", InstantKind.OUTAGE)
            .newInstant("auto 2", InstantKind.AUTO)
            .newInstant("curative 2", InstantKind.CURATIVE);
        Instant preventiveInstant2 = crac.getInstant("preventive 2");
        Instant outageInstant2 = crac.getInstant("outage 2");
        Instant autoInstant2 = crac.getInstant("auto 2");
        Instant curativeInstant2 = crac.getInstant("curative 2");

        assertEquals(Set.of(preventiveInstant, preventiveInstant2), crac.getInstants(InstantKind.PREVENTIVE));
        assertEquals(Set.of(outageInstant, outageInstant2), crac.getInstants(InstantKind.OUTAGE));
        assertEquals(Set.of(autoInstant, autoInstant2), crac.getInstants(InstantKind.AUTO));
        assertEquals(Set.of(curativeInstant, curativeInstant2), crac.getInstants(InstantKind.CURATIVE));
    }

    @Test
    void testGetInstantByKindDoesNotWorkWithTwoInstantsPerInstantKind() {
        crac.newInstant("curative 2", InstantKind.CURATIVE);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.getInstant(InstantKind.CURATIVE));
        assertEquals("Crac does not contain exactly one instant of kind 'CURATIVE'. It contains 2 instants of kind 'CURATIVE'", exception.getMessage());
    }

    @Test
    void testGetPreviousInstant() {
        assertNull(crac.getInstantBefore(preventiveInstant));
        assertEquals(preventiveInstant, crac.getInstantBefore(outageInstant));
        assertEquals(outageInstant, crac.getInstantBefore(autoInstant));
        assertEquals(autoInstant, crac.getInstantBefore(curativeInstant));
    }

    @Test
    void testGetPreviousInstantWorksWithOtherInstance() {
        Instant anotherInstanceOfInstantDefinedInTheCrac = new InstantImpl(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE, null);
        assertNull(crac.getInstantBefore(anotherInstanceOfInstantDefinedInTheCrac));

        anotherInstanceOfInstantDefinedInTheCrac = new InstantImpl(AUTO_INSTANT_ID, InstantKind.AUTO, outageInstant);
        assertEquals(outageInstant, crac.getInstantBefore(anotherInstanceOfInstantDefinedInTheCrac));
    }

    @Test
    void testGetPreviousInstantFromInvalidInstants() {
        assertThrows(NullPointerException.class, () -> crac.getInstantBefore(null));

        Instant instantNotDefinedInTheCrac = new InstantImpl("instantNotDefinedInTheCrac", InstantKind.PREVENTIVE, null);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.getInstantBefore(instantNotDefinedInTheCrac));
        assertEquals("Provided instant 'instantNotDefinedInTheCrac' is not defined in the CRAC", exception.getMessage());

        Instant anotherInstantNotDefinedInTheCrac = new InstantImpl(OUTAGE_INSTANT_ID, InstantKind.PREVENTIVE, preventiveInstant);
        exception = assertThrows(OpenRaoException.class, () -> crac.getInstantBefore(anotherInstantNotDefinedInTheCrac));
        assertEquals("Provided instant {id:'outage', kind:'PREVENTIVE', order:1} is not the same {id: 'outage', kind:'OUTAGE', order:1} in the CRAC", exception.getMessage());
    }

    @Test
    void testFirstInstantHasToBePreventive() {
        CracImpl cracThatFails = new CracImpl("test-crac");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> cracThatFails.newInstant("instant", InstantKind.AUTO));
        assertEquals("The first instant in the CRAC must be preventive", exception.getMessage());

        cracThatFails.newInstant("titi", InstantKind.PREVENTIVE);
        exception = assertThrows(OpenRaoException.class, () -> cracThatFails.newInstant("instant", InstantKind.CURATIVE));
        assertEquals("The second instant in the CRAC must be an outage", exception.getMessage());
    }

    @Test
    void testGetUniqueInstants() {
        assertEquals(preventiveInstant, crac.getPreventiveInstant());
        assertEquals(outageInstant, crac.getOutageInstant());
    }

    @Test
    void testCracHasAutoInstant() {
        assertTrue(crac.hasAutoInstant());
        assertFalse(new CracImpl("test-crac").hasAutoInstant());
    }

    @Test
    void testRaUsageLimits() {
        assertTrue(crac.getRaUsageLimitsPerInstant().isEmpty());
        RaUsageLimits raUsageLimits1 = new RaUsageLimits();
        raUsageLimits1.setMaxRa(3);
        Instant preventiveInstant = crac.getInstant("preventive");
        Map<Instant, RaUsageLimits> firstMap = Map.of(preventiveInstant, raUsageLimits1);
        crac.newRaUsageLimits("preventive")
            .withMaxRa(raUsageLimits1.getMaxRa())
            .add();
        assertEquals(firstMap.get(preventiveInstant), crac.getRaUsageLimitsPerInstant().get(preventiveInstant));
        assertEquals(firstMap.get(preventiveInstant), crac.getRaUsageLimits(preventiveInstant));
        assertEquals(firstMap, crac.getRaUsageLimitsPerInstant());
        Instant fakeInstant = Mockito.mock(Instant.class);
        when(fakeInstant.getId()).thenReturn("fake_instant");
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.newRaUsageLimits("fake_instant"));
        assertEquals("The instant fake_instant does not exist in the crac.", exception.getMessage());
        assertFalse(crac.getRaUsageLimitsPerInstant().containsKey(fakeInstant));
        assertEquals(new RaUsageLimits(), crac.getRaUsageLimits(fakeInstant));
    }
}
