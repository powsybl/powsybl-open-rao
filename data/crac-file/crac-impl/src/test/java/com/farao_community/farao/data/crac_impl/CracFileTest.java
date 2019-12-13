/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.*;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.*;
import com.farao_community.farao.data.crac_impl.range_domain.AbsoluteFixedRange;
import com.farao_community.farao.data.crac_impl.range_domain.RelativeDynamicRange;
import com.farao_community.farao.data.crac_impl.range_domain.RelativeFixedRange;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.VoltageThreshold;
import com.farao_community.farao.data.crac_impl.usage_rule.FreeToUse;
import com.farao_community.farao.data.crac_impl.usage_rule.OnConstraint;
import com.farao_community.farao.data.crac_impl.usage_rule.OnContingency;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.farao_community.farao.data.crac_api.ActionType.*;
import static com.farao_community.farao.data.crac_api.Direction.*;
import static com.farao_community.farao.data.crac_api.Side.*;
import static org.junit.Assert.*;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracFileTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracFileTest.class);

    private static Crac create() {
        NetworkElement networkElement1 = new NetworkElement("idNE1", "My Element 1");

        // Redispatching
        NetworkElement generator = new NetworkElement("idGenerator", "My Generator");
        Redispatching rd = new Redispatching(10, 20, 18, 1000, 12, generator);
        rd.setMinimumPower(rd.getMinimumPower() + 1);
        rd.setMaximumPower(rd.getMaximumPower() + 1);
        rd.setTargetPower(rd.getTargetPower() + 1);
        rd.setStartupCost(rd.getStartupCost() + 1);
        rd.setMarginalCost(rd.getMarginalCost() + 1);

        // Range domain
        RelativeFixedRange relativeFixedRange = new RelativeFixedRange(0, 1);
        relativeFixedRange.setMin(1);
        relativeFixedRange.setMax(10);
        RelativeDynamicRange relativeDynamicRange = new RelativeDynamicRange(0, 1);
        relativeDynamicRange.setMin(100);
        relativeDynamicRange.setMax(1000);
        AbsoluteFixedRange absoluteFixedRange = new AbsoluteFixedRange(0, 1);
        absoluteFixedRange.setMin(10);
        absoluteFixedRange.setMax(1000);

        // PstRange
        NetworkElement pst1 = new NetworkElement("idPst1", "My Pst 1");
        PstRange pstRange1 = new PstRange(null);
        pstRange1.setNetworkElement(pst1);

        // HvdcRange
        NetworkElement hvdc1 = new NetworkElement("idHvdc1", "My Hvdc 1");
        HvdcRange hvdcRange1 = new HvdcRange(null);
        hvdcRange1.setNetworkElement(hvdc1);

        // GeneratorRange
        NetworkElement generator1 = new NetworkElement("idGen1", "My Generator 1");
        InjectionRange injectionRange1 = new InjectionRange(null);
        injectionRange1.setNetworkElement(generator1);

        // Countertrading
        Countertrading countertrading = new Countertrading();

        // Topology
        NetworkElement line1 = new NetworkElement("idLine1", "My Line 1");
        Topology topology1 = new Topology(line1, OPEN);
        NetworkElement switch1 = new NetworkElement("idSwitch1", "My Switch 1");
        Topology topology2 = new Topology(null, null);
        topology2.setNetworkElement(switch1);
        topology2.setActionType(CLOSE);

        // Hvdc setpoint
        HvdcSetpoint hvdcSetpoint = new HvdcSetpoint(switch1, 0);
        hvdcSetpoint.setNetworkElement(line1);
        hvdcSetpoint.setSetpoint(1000);

        // Pst setpoint
        PstSetpoint pstSetpoint = new PstSetpoint(switch1, 0);
        pstSetpoint.setNetworkElement(pst1);
        pstSetpoint.setSetpoint(5);

        // Injection setpoint
        InjectionSetpoint injectionSetpoint = new InjectionSetpoint(switch1, 0);
        injectionSetpoint.setNetworkElement(generator1);
        injectionSetpoint.setSetpoint(100);

        NetworkElement line2 = new NetworkElement("idLine2", "My Line 2");
        NetworkElement line3 = new NetworkElement("idLine3", "My Line 3");

        List<NetworkElement> elementsList = new ArrayList<>(Arrays.asList(line2, line3));
        ComplexContingency contingency = new ComplexContingency("idContingency", "My contingency", null);
        contingency.setNetworkElements(elementsList);
        contingency.addNetworkElement(networkElement1);

        // Instant
        Instant basecase = new Instant("initial", 0);
        Instant curative = new Instant("curative", -1);
        curative.setSeconds(200);

        // State
        State stateBasecase = new SimpleState(Optional.empty(), basecase);
        State stateCurative = new SimpleState(Optional.empty(), null);
        stateCurative.setContingency(Optional.of(contingency));
        stateCurative.setInstant(curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        // Thresholds
        AbsoluteFlowThreshold threshold1 = new AbsoluteFlowThreshold(Unit.AMPERE, LEFT, IN, 1000);
        threshold1.setSide(RIGHT);
        threshold1.setDirection(OUT);
        threshold1.setMaxValue(999);
        VoltageThreshold threshold2 = new VoltageThreshold(280, 300);
        threshold2.setMinValue(275);
        threshold2.setMaxValue(305);

        // CNECs
        SimpleCnec cnec1 = new SimpleCnec("idCnec", "Cnec", null, threshold1, stateCurative);
        cnec1.setCriticalNetworkElement(monitoredElement);
        SimpleCnec cnec2 = new SimpleCnec("idCnec2", "Cnec 2", monitoredElement, null, null);
        cnec2.setState(stateBasecase);
        cnec2.setThreshold(threshold2);

        // Usage rules
        FreeToUse freeToUse = new FreeToUse(null, null);
        freeToUse.setUsageMethod(UsageMethod.AVAILABLE);
        freeToUse.setState(stateBasecase);
        OnContingency onContingency = new OnContingency(UsageMethod.FORCED, stateCurative, null);
        onContingency.setContingency(contingency);
        OnConstraint onConstraint = new OnConstraint(UsageMethod.FORCED, stateCurative, null);
        onConstraint.setCnec(cnec1);

        // NetworkAction
        ComplexNetworkAction networkAction1 = new ComplexNetworkAction("id1", "name1", "operator1", new ArrayList<>(Arrays.asList(freeToUse)), new ArrayList<>(Arrays.asList(hvdcSetpoint)));
        networkAction1.addApplicableNetworkAction(topology2);
        ComplexNetworkAction networkAction2 = new ComplexNetworkAction("id2", "name2", "operator1", new ArrayList<>(Arrays.asList(freeToUse)), new ArrayList<>(Arrays.asList(pstSetpoint)));

        // RangeAction
        ComplexRangeAction rangeAction1 = new ComplexRangeAction("idRangeAction", "myRangeAction", "operator1", null, null, null);
        List<Range> ranges = new ArrayList<>(Arrays.asList(absoluteFixedRange, relativeDynamicRange));
        rangeAction1.setRanges(ranges);
        rangeAction1.addRange(relativeFixedRange);
        List<ApplicableRangeAction> elementaryRangeActions = new ArrayList<>(Arrays.asList(pstRange1));
        rangeAction1.setApplicableRangeActions(elementaryRangeActions);
        rangeAction1.addApplicableRangeAction(hvdcRange1);
        List<UsageRule> usageRules =  new ArrayList<>(Arrays.asList(freeToUse, onConstraint));
        rangeAction1.setUsageRules(usageRules);
        rangeAction1.addUsageRule(onContingency);

        ComplexRangeAction rangeAction2 = new ComplexRangeAction("idRangeAction2", "myRangeAction2", "operator1", usageRules, ranges, new ArrayList<>(Arrays.asList(pstRange1)));

        Crac crac = new SimpleCrac("idCrac", "name");

        crac.addCnec(cnec1);
        crac.addCnec(cnec2);
        crac.addNetworkAction(networkAction1);
        crac.addNetworkAction(networkAction2);
        crac.addRangeAction(rangeAction1);
        crac.addRangeAction(rangeAction2);

        return crac;
    }

    @Test
    public void testCrac() {

        Crac crac = create();

        crac.getCnecs().forEach(
            cnec -> {
                cnec.getState().getInstant();
                cnec.getState().getContingency();
            });

        crac.getRangeActions().forEach(
            abstractRemedialAction -> abstractRemedialAction.getUsageRules().forEach(
                    UsageRule::getUsageMethod));

        assertEquals("idCrac", crac.getId());

        Set<RangeAction> rangeActions = crac.getRangeActions();
        for (RangeAction rangeAction : rangeActions) {
            List<NetworkElement> networkElements = rangeAction.getNetworkElements();
            for (NetworkElement networkElement : networkElements) {
                assertNotNull(networkElement.getId());
            }
        }

        ComplexRangeAction rangeAction1 = (ComplexRangeAction) rangeActions.iterator().next();
        assertNotEquals(0, rangeAction1.getNetworkElements().size());

        Countertrading countertrading = new Countertrading();
        assertEquals(0, countertrading.getNetworkElements().size());

        NetworkElement generator = new NetworkElement("idGenerator", "My Generator");
        Redispatching rd = new Redispatching(10, 20, 18, 1000, 12, generator);
        assertEquals(0, rd.getNetworkElements().size());

        NetworkElement generator1 = new NetworkElement("idGen1", "My Generator 1");
        InjectionRange injectionRange1 = new InjectionRange(null);
        injectionRange1.setNetworkElement(generator1);
        assertEquals(1, injectionRange1.getNetworkElements().size());
    }

    @Test
    public void testGetInstant() {
        SimpleCrac simpleCrac = new SimpleCrac("test-crac");
        assertEquals(0, simpleCrac.getInstants().size());
    }

    @Test
    public void testAddInstant() {
        SimpleCrac simpleCrac = new SimpleCrac("test-crac");
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
        Crac simpleCrac = new SimpleCrac("test-crac");
        assertEquals(0, simpleCrac.getContingencies().size());
    }

    @Test
    public void testAddContingency() {
        Crac simpleCrac = new SimpleCrac("test-crac");
        assertEquals(0, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne1"))));
        assertEquals(1, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-1"));
        try {
            simpleCrac.addContingency(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne2"))));
            fail();
        } catch (FaraoException e) {
            assertEquals("A contingency with the same ID and different network elements already exists.", e.getMessage());
        }
        try {
            simpleCrac.addContingency(new ComplexContingency("contingency-2", Collections.singletonList(new NetworkElement("ne1"))));
        } catch (FaraoException e) {
            fail();
        }
        assertEquals(2, simpleCrac.getContingencies().size());
        simpleCrac.addContingency(new ComplexContingency("contingency-3", Collections.singletonList(new NetworkElement("ne3"))));
        assertEquals(3, simpleCrac.getContingencies().size());
        assertNotNull(simpleCrac.getContingency("contingency-3"));
        assertNull(simpleCrac.getContingency("contingency-fail"));
    }

    @Test
    public void testStates() {
        SimpleCrac simpleCrac = new SimpleCrac("test-crac");
        assertNull(simpleCrac.getPreventiveState());
        assertEquals(0, simpleCrac.getContingencies().size());
        assertEquals(0, simpleCrac.getInstants().size());

        simpleCrac.addState(new SimpleState(Optional.empty(), new Instant("initial-instant", 0)));
        assertNotNull(simpleCrac.getPreventiveState());
        assertEquals("initial-instant", simpleCrac.getPreventiveState().getInstant().getId());

        assertEquals(simpleCrac.getInstant("initial-instant"), simpleCrac.getPreventiveState().getInstant());

        simpleCrac.addState(new SimpleState(
                Optional.of(new ComplexContingency("contingency", Collections.singletonList(new NetworkElement("network-element")))),
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
            Optional.of(new ComplexContingency("contingency-2", Collections.singletonList(new NetworkElement("network-element-2")))),
            new Instant("after-contingency", 60))
        );

        assertEquals(2, simpleCrac.getStates(simpleCrac.getInstant("after-contingency")).size());

        // Different states pointing at the same instant object
        Instant instant = simpleCrac.getInstant("after-contingency");
        simpleCrac.getStates(instant).forEach(
            state -> assertSame(instant, state.getInstant())
        );

        simpleCrac.addState(new SimpleState(
            Optional.of(new ComplexContingency("contingency-2", Collections.singletonList(new NetworkElement("network-element-2")))),
            new Instant("after-contingency-bis", 70))
        );

        // Different states pointing at the same contingency object
        Contingency contingency = simpleCrac.getContingency("contingency-2");
        simpleCrac.getStates(contingency).forEach(
            state ->  {
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
    public void testOrderedStates() {
        Crac simpleCrac = new SimpleCrac("simple-crac");
        State state1 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne1")))),
            new Instant("auto", 60)
        );

        State state2 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne1")))),
            new Instant("auto-later", 70)
        );

        State state3 = new SimpleState(
            Optional.of(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne1")))),
            new Instant("curative", 120)
        );

        simpleCrac.addState(state3);
        simpleCrac.addState(state1);
        simpleCrac.addState(state2);

        Iterator<State> states = simpleCrac.getStates(simpleCrac.getContingency("contingency-1")).iterator();
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
            Optional.of(new ComplexContingency("contingency-1", Collections.singletonList(new NetworkElement("ne1")))),
            new Instant("intermediate", 100)
        );

        simpleCrac.addState(state4);

        states = simpleCrac.getStates(simpleCrac.getContingency("contingency-1")).iterator();
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
}
