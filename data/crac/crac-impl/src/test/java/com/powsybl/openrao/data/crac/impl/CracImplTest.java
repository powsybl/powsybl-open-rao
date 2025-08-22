/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Country;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.PhysicalParameter;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.ContingencyAdder;
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.cnec.AngleCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnecAdder;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.cnec.VoltageCnec;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkActionAdder;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

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

    private State state1;
    private State state2;
    private RangeAction<?> ra1;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;
    private RangeAction<?> ra4;
    private RangeAction<?> ra5;
    private RangeAction<?> ra6;
    private RangeAction<?> ra7;
    private RangeAction<?> ra8;
    private RangeAction<?> ra9;
    private RangeAction<?> ra10;

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
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
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
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();

        crac.newNetworkAction()
                .withId("na")
                .withOperator("operator")
                .newTerminalsConnectionAction().withActionType(ActionType.OPEN).withNetworkElement("ne4").add()
                .newSwitchAction().withActionType(ActionType.OPEN).withNetworkElement("ne5").add()
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

        State curative1 = crac.addState(contingency1, curativeInstant);
        State auto1 = crac.addState(contingency1, autoInstant);
        State curative2 = crac.addState(contingency2, curativeInstant);
        State auto2 = crac.addState(contingency1, outageInstant);
        crac.addState(contingency2, outageInstant);

        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("anyNetworkElement")
                .withOperator("operator")
                .withContingency("co1")
                .withInstant(CURATIVE_INSTANT_ID)
                .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();

        crac.newNetworkAction()
                .withId("ra")
                .withOperator("operator")
                .newPhaseTapChangerTapPositionAction().withNetworkElement("anyPst").withTapPosition(8).add()
                .newOnContingencyStateUsageRule()
                .withContingency("co1")
                .withInstant(AUTO_INSTANT_ID)
                .withUsageMethod(UsageMethod.AVAILABLE)
                .add()
                .newOnContingencyStateUsageRule()
                .withContingency("co2")
                .withInstant(CURATIVE_INSTANT_ID)
                .withUsageMethod(UsageMethod.FORCED)
                .add()
                .add();

        assertNotNull(crac.getRemedialAction("ra"));
        assertNotNull(crac.getNetworkAction("ra"));

        assertEquals(5, crac.getStates().size());
        crac.safeRemoveStates(Set.of(curative1.getId(), auto1.getId(), curative2.getId(), auto2.getId()));
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
        assertInstanceOf(ContingencyAdderImpl.class, contingencyAdder);
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
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
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
                .newTerminalsConnectionAction().withNetworkElement("anyNetworkElement").withActionType(ActionType.CLOSE).add()
                .newOnContingencyStateUsageRule().withInstant(CURATIVE_INSTANT_ID).withContingency("co1").withUsageMethod(UsageMethod.AVAILABLE).add()
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
        State curative1 = crac.addState(contingency1, curativeInstant);
        State outage1 = crac.addState(contingency1, outageInstant);
        State curative2 = crac.addState(contingency2, curativeInstant);
        State auto2 = crac.addState(contingency2, autoInstant);
        State outage2 = crac.addState(contingency2, outageInstant);

        assertEquals(2, crac.getStates(contingency1).size());
        assertTrue(crac.getStates(contingency1).containsAll(Set.of(curative1, outage1)));
        assertEquals(3, crac.getStates(contingency2).size());
        assertTrue(crac.getStates(contingency2).containsAll(Set.of(curative2, auto2, outage2)));
        Contingency contingency3 = new Contingency("co3", "co3", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        assertTrue(crac.getStates(contingency3).isEmpty());
    }

    @Test
    void testGetStatesFromInstant() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        Contingency contingency2 = new Contingency("co2", "co2", Collections.singletonList(Mockito.mock(ContingencyElement.class)));
        crac.addContingency(contingency1);
        crac.addContingency(contingency2);
        State curative1 = crac.addState(contingency1, curativeInstant);
        State outage1 = crac.addState(contingency1, outageInstant);
        State curative2 = crac.addState(contingency2, curativeInstant);
        State auto2 = crac.addState(contingency2, autoInstant);
        State outage2 = crac.addState(contingency2, outageInstant);

        assertEquals(2, crac.getStates(outageInstant).size());
        assertTrue(crac.getStates(curativeInstant).containsAll(Set.of(curative1, curative2)));
        assertEquals(2, crac.getStates(outageInstant).size());
        assertTrue(crac.getStates(outageInstant).containsAll(Set.of(outage1, outage2)));
        assertEquals(1, crac.getStates(autoInstant).size());
        assertTrue(crac.getStates(autoInstant).contains(auto2));
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
        State curative1 = crac.addState(contingency1, curativeInstant);
        State curative1bis = crac.addState(contingency1, curativeInstant);
        assertSame(curative1, curative1bis);
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
        assertInstanceOf(FlowCnecAdderImpl.class, flowCnecAdder);
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
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();
        FlowCnec cnec2 = crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("anyNetworkElement")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();
        FlowCnec cnec3 = crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("anyNetworkElement")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co2")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();

        State curative1 = crac.getState("co1", curativeInstant);
        State outage2 = crac.getState("co2", outageInstant);

        assertEquals(2, crac.getFlowCnecs(curative1).size());
        assertEquals(2, crac.getCnecs(curative1).size());
        assertTrue(crac.getFlowCnecs(curative1).containsAll(Set.of(cnec1, cnec2)));
        assertTrue(crac.getCnecs(curative1).containsAll(Set.of(cnec1, cnec2)));
        assertEquals(1, crac.getFlowCnecs(outage2).size());
        assertEquals(1, crac.getCnecs(outage2).size());
        assertTrue(crac.getFlowCnecs(outage2).contains(cnec3));
        assertTrue(crac.getCnecs(outage2).contains(cnec3));
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
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec2")
                .withNetworkElement("ne1")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec3")
                .withNetworkElement("ne2")
                .withInstant(CURATIVE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();
        crac.newFlowCnec()
                .withId("cnec4")
                .withNetworkElement("ne2")
                .withInstant(OUTAGE_INSTANT_ID)
                .withContingency("co1")
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
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

        ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();

        state1 = crac.getState("co1", curativeInstant);
        state2 = crac.getState("co2", curativeInstant);

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

        ra1 = crac.newHvdcRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra2 = crac.newHvdcRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra3 = crac.newHvdcRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra4 = crac.newHvdcRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();

        state1 = crac.getState("co1", curativeInstant);
        state2 = crac.getState("co2", curativeInstant);

        assertEquals(0, crac.getRangeActions(state1, UsageMethod.FORCED).size());
        assertEquals(2, crac.getRangeActions(state1, UsageMethod.AVAILABLE).size());
        assertTrue(crac.getRangeActions(state1, UsageMethod.AVAILABLE).containsAll(Set.of(ra1, ra3)));
        assertEquals(2, crac.getRangeActions(state2, UsageMethod.FORCED).size());
        assertTrue(crac.getRangeActions(state2, UsageMethod.FORCED).containsAll(Set.of(ra2, ra4)));

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

        ra1 = crac.newPstRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();
        ra4 = crac.newPstRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(Map.of(-1, -1., 0, 0., 1, 1.))
                .add();

        state1 = crac.getState("co1", curativeInstant);
        state2 = crac.getState("co2", curativeInstant);

        assertEquals(Set.of(ra1), crac.getRangeActions(state1, UsageMethod.AVAILABLE));
        assertEquals(Set.of(ra3), crac.getRangeActions(state2, UsageMethod.AVAILABLE));
        assertEquals(Set.of(ra2), crac.getRangeActions(state1, UsageMethod.FORCED));
        assertEquals(Set.of(ra4), crac.getRangeActions(state2, UsageMethod.FORCED));
        assertEquals(Set.of(ra1, ra2), crac.getRangeActions(state1, UsageMethod.AVAILABLE, UsageMethod.FORCED));
        assertEquals(Set.of(ra3, ra4), crac.getRangeActions(state2, UsageMethod.AVAILABLE, UsageMethod.FORCED));
    }

    @Test
    void testFilterHvdcRangeActionUsageRules() {
        crac.newContingency().withId("co1").withContingencyElement("neCo", getRandomTypeContingency()).add();
        crac.newContingency().withId("co2").withContingencyElement("neCo", getRandomTypeContingency()).add();

        ra1 = crac.newHvdcRangeAction()
                .withId("ra1")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra2 = crac.newHvdcRangeAction()
                .withId("ra2")
                .withNetworkElement("ne1")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra3 = crac.newHvdcRangeAction()
                .withId("ra3")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();
        ra4 = crac.newHvdcRangeAction()
                .withId("ra4")
                .withNetworkElement("ne2")
                .newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co2").withInstant(CURATIVE_INSTANT_ID).add()
                .newRange().withMin(-5).withMax(10).add()
                .add();

        state1 = crac.getState("co1", curativeInstant);
        state2 = crac.getState("co2", curativeInstant);

        assertEquals(Set.of(ra1), crac.getRangeActions(state1, UsageMethod.AVAILABLE));
        assertEquals(Set.of(ra3), crac.getRangeActions(state2, UsageMethod.AVAILABLE));
        assertEquals(Set.of(ra2), crac.getRangeActions(state1, UsageMethod.FORCED));
        assertEquals(Set.of(ra4), crac.getRangeActions(state2, UsageMethod.FORCED));
        assertEquals(Set.of(ra1, ra2), crac.getRangeActions(state1, UsageMethod.AVAILABLE, UsageMethod.FORCED));
        assertEquals(Set.of(ra3, ra4), crac.getRangeActions(state2, UsageMethod.AVAILABLE, UsageMethod.FORCED));
    }

    @Test
    void testRemoveNetworkAction() {
        crac.addNetworkElement("neCo", "neCo");
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(getRandomTypeContingencyElement("neCo")));
        crac.addContingency(contingency1);

        NetworkActionAdder na1Adder = crac.newNetworkAction().withId("na1").withName("na1").withOperator("operator").withSpeed(10);
        na1Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add();
        na1Adder.newSwitchAction().withNetworkElement("ne1", "ne1").withActionType(ActionType.OPEN).add();
        NetworkAction na1 = na1Adder.add();
        NetworkActionAdder na2Adder = crac.newNetworkAction().withId("na2").withName("na2").withOperator("operator").withSpeed(10);
        na2Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(AUTO_INSTANT_ID).add();
        na2Adder.newSwitchAction().withNetworkElement("ne1", "ne1").withActionType(ActionType.OPEN).add();
        NetworkAction na2 = na2Adder.add();
        NetworkActionAdder na3Adder = crac.newNetworkAction().withId("na3").withName("na3").withOperator("operator").withSpeed(10);
        na3Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add();
        na3Adder.newSwitchAction().withNetworkElement("ne2", "ne2").withActionType(ActionType.CLOSE).add();
        NetworkAction na3 = na3Adder.add();
        NetworkActionAdder na4Adder = crac.newNetworkAction().withId("na4").withName("na4").withOperator("operator").withSpeed(10);
        na4Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(AUTO_INSTANT_ID).add();
        na4Adder.newSwitchAction().withNetworkElement("ne2", "ne2").withActionType(ActionType.CLOSE).add();
        NetworkAction na4 = na4Adder.add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency1, autoInstant);

        assertTrue(crac.getNetworkActions(state1, UsageMethod.FORCED).isEmpty());
        assertEquals(Set.of(na1, na3), crac.getNetworkActions(state1, UsageMethod.AVAILABLE));
        assertEquals(Set.of(na2, na4), crac.getNetworkActions(state2, UsageMethod.FORCED));
        assertEquals(Set.of(na2, na4), crac.getNetworkActions(state2, UsageMethod.FORCED, UsageMethod.AVAILABLE));

        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(4, crac.getRemedialActions().size());
        crac.removeRemedialAction("doesnt exist 1");
        crac.removeNetworkAction("doesnt exist 2");
        assertEquals(4, crac.getNetworkActions().size());

        crac.removeRemedialAction("na1");
        assertNull(crac.getRemedialAction("na1"));
        assertNotNull(crac.getNetworkElement("ne1")); // still used by na2
        assertNotNull(crac.getState(contingency1, curativeInstant)); // state1, still used by na3

        crac.removeNetworkAction("na2");
        assertNull(crac.getNetworkAction("na2"));
        assertNull(crac.getNetworkElement("ne1")); // unused
        assertNotNull(crac.getState(contingency1, curativeInstant)); // state1, still used by na3
        assertNotNull(crac.getState(contingency1, autoInstant)); // state2, still used by na4

        crac.removeNetworkAction("na3");
        assertNull(crac.getNetworkAction("na3"));
        assertNotNull(crac.getNetworkElement("ne2")); // still used by na4
        assertNull(crac.getState(contingency1, curativeInstant)); // unused
        assertNotNull(crac.getState(contingency1, autoInstant)); // state2, still used by na4

        crac.removeRemedialAction("na4");
        assertEquals(0, crac.getRemedialActions().size());
        assertEquals(1, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement("neCo"));
        assertEquals(0, crac.getStates().size());
    }

    @Test
    void testFilterNetworkActionUsageRules() {
        Contingency contingency1 = new Contingency("co1", "co1", Collections.singletonList(getRandomTypeContingencyElement("neCo")));
        crac.addContingency(contingency1);

        NetworkActionAdder na1Adder = crac.newNetworkAction().withId("na1").withName("na1").withOperator("operator").withSpeed(10);
        na1Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.AVAILABLE).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add();
        na1Adder.newSwitchAction().withNetworkElement("ne1", "ne1").withActionType(ActionType.OPEN).add();
        NetworkAction na1 = na1Adder.add();
        NetworkActionAdder na2Adder = crac.newNetworkAction().withId("na2").withName("na2").withOperator("operator").withSpeed(10);
        na2Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(AUTO_INSTANT_ID).add();
        na2Adder.newSwitchAction().withNetworkElement("ne1", "ne1").withActionType(ActionType.OPEN).add();
        NetworkAction na2 = na2Adder.add();
        NetworkActionAdder na3Adder = crac.newNetworkAction().withId("na3").withName("na3").withOperator("operator").withSpeed(10);
        na3Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(CURATIVE_INSTANT_ID).add();
        na3Adder.newSwitchAction().withNetworkElement("ne2", "ne2").withActionType(ActionType.CLOSE).add();
        NetworkAction na3 = na3Adder.add();
        NetworkActionAdder na4Adder = crac.newNetworkAction().withId("na4").withName("na4").withOperator("operator").withSpeed(10);
        na4Adder.newOnContingencyStateUsageRule().withUsageMethod(UsageMethod.FORCED).withContingency("co1").withInstant(AUTO_INSTANT_ID).add();
        na4Adder.newSwitchAction().withNetworkElement("ne2", "ne2").withActionType(ActionType.CLOSE).add();
        NetworkAction na4 = na4Adder.add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency1, autoInstant);

        assertEquals(Set.of(na1), crac.getNetworkActions(state1, UsageMethod.AVAILABLE));
        assertEquals(Set.of(), crac.getNetworkActions(state2, UsageMethod.AVAILABLE));
        assertEquals(Set.of(na3), crac.getNetworkActions(state1, UsageMethod.FORCED));
        assertEquals(Set.of(na2, na4), crac.getNetworkActions(state2, UsageMethod.FORCED));
        assertEquals(Set.of(na1, na3), crac.getNetworkActions(state1, UsageMethod.AVAILABLE, UsageMethod.FORCED));
        assertEquals(Set.of(na2, na4), crac.getNetworkActions(state2, UsageMethod.AVAILABLE, UsageMethod.FORCED));
    }

    @Test
    void testPstRangeActionAdder() {
        PstRangeActionAdder pstRangeActionAdder = crac.newPstRangeAction();
        assertInstanceOf(PstRangeActionAdderImpl.class, pstRangeActionAdder);
        assertSame(crac, ((PstRangeActionAdderImpl) pstRangeActionAdder).getCrac());
    }

    @Test
    void testHvdcRangeActionAdder() {
        HvdcRangeActionAdder hvdcRangeActionAdder = crac.newHvdcRangeAction();
        assertInstanceOf(HvdcRangeActionAdderImpl.class, hvdcRangeActionAdder);
        assertSame(crac, ((HvdcRangeActionAdderImpl) hvdcRangeActionAdder).getCrac());
    }

    @Test
    void testNetworkActionAdder() {
        NetworkActionAdder networkActionAdder = crac.newNetworkAction();
        assertInstanceOf(NetworkActionAdderImpl.class, networkActionAdder);
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
    void testTwoPreventiveInstants() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.newInstant("preventive 2", InstantKind.PREVENTIVE));
        assertEquals("Only one preventive instant is allowed", exception.getMessage());
    }

    @Test
    void testTwoOutageInstants() {
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> crac.newInstant("outage 2", InstantKind.OUTAGE));
        assertEquals("Only one outage instant is allowed", exception.getMessage());
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

    private void setUpCracWithRAs() {
        Contingency contingency1 = crac.newContingency()
            .withId("contingency1")
            .withContingencyElement("contingency1-ne", ContingencyElementType.LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("contingency2")
            .withContingencyElement("contingency2-ne", ContingencyElementType.LINE)
            .add();
        crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("cnec-ne")
            .withContingency("contingency1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withNominalVoltage(220.)
            .newThreshold().withSide(TwoSides.TWO).withMax(1000.).withUnit(Unit.AMPERE).add()
            .add();
        // ra1 : preventive only
        ra1 = crac.newPstRangeAction()
            .withId("ra1")
            .withNetworkElement("ra1-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra2 : preventive and curative
        ra2 = crac.newPstRangeAction()
            .withId("ra2")
            .withNetworkElement("ra2-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNAVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra3 : preventive and curative
        ra3 = crac.newPstRangeAction()
            .withId("ra3")
            .withNetworkElement("ra3-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newTapRange().withMaxTap(100).withMinTap(-100).withRangeType(RangeType.RELATIVE_TO_PREVIOUS_INSTANT).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra4 : preventive only, but with same NetworkElement as ra5
        ra4 = crac.newPstRangeAction()
            .withId("ra4")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra5 : curative only, but with same NetworkElement as ra4
        ra5 = crac.newPstRangeAction()
            .withId("ra5")
            .withNetworkElement("ra4-ne1")
            .withNetworkElement("ra4-ne2")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra6 : preventive and curative (onFlowConstraint)
        ra6 = crac.newPstRangeAction()
            .withId("ra6")
            .withNetworkElement("ra6-ne")
            .withOperator("FR")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnConstraintUsageRule().withCnec("cnec").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra7 : auto only
        ra7 = crac.newPstRangeAction()
            .withId("ra7")
            .withNetworkElement("ra7-ne")
            .newOnContingencyStateUsageRule().withContingency("contingency2").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(1)
            .add();
        // ra8 : preventive and auto
        ra8 = crac.newPstRangeAction()
            .withId("ra8")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(AUTO_INSTANT_ID).withUsageMethod(UsageMethod.FORCED).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .withSpeed(2)
            .add();
        // ra9 : preventive only, but with same NetworkElement as ra8
        ra9 = crac.newPstRangeAction()
            .withId("ra9")
            .withNetworkElement("ra8-ne")
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
            .add();
        // ra10 : preventive only, counter trade
        ra10 = crac.newCounterTradeRangeAction()
            .withId("ra10")
            .withExportingCountry(Country.FR)
            .withImportingCountry(Country.DE)
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.UNDEFINED).add()
            .newRange().withMin(-1000).withMax(1000).add()
            .add();

        // na1 : preventive + curative
        crac.newNetworkAction()
            .withId("na1")
            .newSwitchAction().withNetworkElement("na1-ne").withActionType(ActionType.OPEN).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .newOnContingencyStateUsageRule().withContingency("contingency1").withInstant(CURATIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        state1 = crac.getState(contingency1, curativeInstant);
        state2 = crac.getState(contingency2, curativeInstant);
    }

    @Test
    void testIsRangeActionAvailableInState() {
        setUpCracWithRAs();

        // ra1 is available in preventive only
        assertTrue(crac.isRangeActionAvailableInState(ra1, crac.getPreventiveState()));
        assertFalse(crac.isRangeActionAvailableInState(ra1, state1));
        assertFalse(crac.isRangeActionAvailableInState(ra1, state2));

        // ra2 is available in state2 only
        assertFalse(crac.isRangeActionAvailableInState(ra2, crac.getPreventiveState()));
        assertFalse(crac.isRangeActionAvailableInState(ra2, state1));
        assertTrue(crac.isRangeActionAvailableInState(ra2, state2));

        // ra3 is available in preventive and in state1
        assertTrue(crac.isRangeActionAvailableInState(ra3, crac.getPreventiveState()));
        assertTrue(crac.isRangeActionAvailableInState(ra3, state1));
        assertFalse(crac.isRangeActionAvailableInState(ra3, state2));

        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(crac.isRangeActionAvailableInState(ra4, crac.getPreventiveState()));
        assertFalse(crac.isRangeActionAvailableInState(ra4, state1));
        assertFalse(crac.isRangeActionAvailableInState(ra4, state2));

        assertFalse(crac.isRangeActionAvailableInState(ra5, crac.getPreventiveState()));
        assertFalse(crac.isRangeActionAvailableInState(ra5, state1));
        assertTrue(crac.isRangeActionAvailableInState(ra5, state2));

        // ra6 is available in preventive and in state1
        assertTrue(crac.isRangeActionAvailableInState(ra6, crac.getPreventiveState()));
        assertTrue(crac.isRangeActionAvailableInState(ra6, state1));
        assertFalse(crac.isRangeActionAvailableInState(ra6, state2));

        // ra10 is available in preventive only
        assertTrue(crac.isRangeActionAvailableInState(ra10, crac.getPreventiveState()));
        assertFalse(crac.isRangeActionAvailableInState(ra10, state1));
        assertFalse(crac.isRangeActionAvailableInState(ra10, state2));
    }

    @Test
    void testIsRangeActionPreventive() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertTrue(crac.isRangeActionPreventive(ra1));
        // ra2 is available in state2 only
        assertFalse(crac.isRangeActionPreventive(ra2));
        // ra3 is available in preventive and in state1
        assertTrue(crac.isRangeActionPreventive(ra3));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertTrue(crac.isRangeActionPreventive(ra4));
        assertFalse(crac.isRangeActionPreventive(ra5));
        // ra6 is preventive and curative
        assertTrue(crac.isRangeActionPreventive(ra6));
    }

    @Test
    void testIsRangeActionCurative() {
        setUpCracWithRAs();
        // ra1 is available in preventive only
        assertFalse(crac.isRangeActionAutoOrCurative(ra1));
        // ra2 is available in state2 only
        assertTrue(crac.isRangeActionAutoOrCurative(ra2));
        // ra3 is available in preventive and in state1
        assertTrue(crac.isRangeActionAutoOrCurative(ra3));
        // ra4 is preventive, ra5 is available in state2, both have the same network element
        assertFalse(crac.isRangeActionAutoOrCurative(ra4));
        assertTrue(crac.isRangeActionAutoOrCurative(ra5));
        // ra6 is preventive and curative
        assertTrue(crac.isRangeActionAutoOrCurative(ra6));
    }

    @Test
    void testIsRangeActionAuto() {
        setUpCracWithRAs();
        // ra7 is auto
        assertTrue(crac.isRangeActionAutoOrCurative(ra7));
        // ra8 is preventive and auto
        assertTrue(crac.isRangeActionAutoOrCurative(ra8));
        // ra9 is preventive with same network element as ra8
        assertFalse(crac.isRangeActionAutoOrCurative(ra9));
    }

    @Test
    void testGetCnecsWithPhysicalParameter() {
        crac.newContingency()
            .withId("co1")
            .withContingencyElement("neCo", getRandomTypeContingency())
            .add();

        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("anyNetworkElement")
            .withOperator("operator")
            .withContingency("co1")
            .withInstant(CURATIVE_INSTANT_ID)
            .newThreshold().withMin(-1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        VoltageCnec voltageCnec = crac.newVoltageCnec()
            .withId("vc")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("co1")
            .withNetworkElement("VL1")
            .withMonitored()
            .newThreshold().withUnit(Unit.KILOVOLT).withMin(400.).withMax(450.).add()
            .add();

        AngleCnec angleCnec = crac.newAngleCnec()
            .withId("acCur1")
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("co1")
            .withImportingNetworkElement("VL1_0")
            .withExportingNetworkElement("VL2")
            .withMonitored()
            .newThreshold().withUnit(Unit.DEGREE).withMin(-8.).withMax(null).add()
            .add();

        assertEquals(Set.of(flowCnec), crac.getCnecs(PhysicalParameter.FLOW));
        assertEquals(Set.of(angleCnec), crac.getCnecs(PhysicalParameter.ANGLE));
        assertEquals(Set.of(voltageCnec), crac.getCnecs(PhysicalParameter.VOLTAGE));

        assertEquals(Collections.emptySet(), crac.getCnecs(PhysicalParameter.FLOW, crac.getPreventiveState()));
        assertEquals(Collections.emptySet(), crac.getCnecs(PhysicalParameter.ANGLE, crac.getPreventiveState()));
        assertEquals(Collections.emptySet(), crac.getCnecs(PhysicalParameter.VOLTAGE, crac.getPreventiveState()));

        assertEquals(Set.of(flowCnec), crac.getCnecs(PhysicalParameter.FLOW, crac.getState("co1", crac.getInstant(CURATIVE_INSTANT_ID))));
        assertEquals(Set.of(angleCnec), crac.getCnecs(PhysicalParameter.ANGLE, crac.getState("co1", crac.getInstant(CURATIVE_INSTANT_ID))));
        assertEquals(Set.of(voltageCnec), crac.getCnecs(PhysicalParameter.VOLTAGE, crac.getState("co1", crac.getInstant(CURATIVE_INSTANT_ID))));
    }

    @Test
    void createCracWithSeveralAutoInstants() {
        CracImpl badCrac = new CracImpl("crac");
        badCrac.newInstant("preventive", InstantKind.PREVENTIVE);
        badCrac.newInstant("outage", InstantKind.OUTAGE);
        badCrac.newInstant("auto-1", InstantKind.AUTO);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> badCrac.newInstant("auto-2", InstantKind.AUTO));
        assertEquals("Only one auto instant is allowed and it must occur between outage and curative instants", exception.getMessage());
    }

    @Test
    void createCracWithAutoInstantAfterCurative() {
        CracImpl badCrac = new CracImpl("crac");
        badCrac.newInstant("preventive", InstantKind.PREVENTIVE);
        badCrac.newInstant("outage", InstantKind.OUTAGE);
        badCrac.newInstant("curative", InstantKind.CURATIVE);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> badCrac.newInstant("auto", InstantKind.AUTO));
        assertEquals("Only one auto instant is allowed and it must occur between outage and curative instants", exception.getMessage());
    }
}
