/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_impl;

/**
 * General test file
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
import com.farao_community.farao.data.crac_impl.remedial_action.threshold.FlowThreshold;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.remedial_action.threshold.VoltageThreshold;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.Redispatching;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_api.ActionType.*;
import static com.farao_community.farao.data.crac_api.Direction.*;
import static com.farao_community.farao.data.crac_api.Side.*;
import static com.farao_community.farao.data.crac_api.Unit.*;
import static org.junit.Assert.*;

public class CracFileTest {

    @Test
    public void test1() {
        int i = 1;
    }

    @Test
    public void testCrac() {

        NetworkElement networkElement1 = new NetworkElement("idNE1", "My Element 1");
        NetworkElement networkElement2 = new NetworkElement("idNE2", "My Element 2");
        NetworkElement networkElement3 = new NetworkElement("idNE3", "My Element 3");
        NetworkElement networkElement4 = new NetworkElement("idNE4", "My Element 4");

        // Redispatching
        NetworkElement generator = new NetworkElement("idGenerator", "My Generator");
        Redispatching rd = new Redispatching(10, 20, 18, 1000, 12, generator);
        rd.setMinimumPower(rd.getMinimumPower() + 1);
        rd.setMaximumPower(rd.getMaximumPower() + 1);
        rd.setTargetPower(rd.getTargetPower() + 1);
        rd.setStartupCost(rd.getStartupCost() + 1);
        rd.setMarginalCost(rd.getMarginalCost() + 1);

        // PstLever
        NetworkElement pst1 = new NetworkElement("idPst1", "My Pst 1");
        /*PstRange pstRange1 = PstRange.withRelativeRange(pst1, 2, 8);
        PstRange pstRange2 = PstRange.withAbsoluteRange(null, 4, 18);
        pstRange2.setNetworkElement(pst1);*/

        NetworkElement pst2 = new NetworkElement("idPst2", "My Pst 2");
        //PstRange pstRange3 = PstRange.create(pst2, -5, 5, 10, 20);

        /*List<PstRange> pstRangeList = new ArrayList<>(Arrays.asList(pstRange2));
        PstAlignLever pstAlignLever1 = new PstAlignLever(Arrays.asList(pstRange1));
        PstAlignLever pstAlignLever2 = new PstAlignLever(null);
        pstAlignLever2.setPstRanges(pstRangeList);
        pstAlignLever2.addPstLever(pstRange3);*/

        // HvdcLever
        NetworkElement hvdc1 = new NetworkElement("idHvdc1", "My Hvdc 1");
        /*HvdcRange hvdcRange1 = HvdcRange.withRelativeRange(null, 200, 800);
        hvdcRange1.setNetworkElement(hvdc1);
        HvdcRange hvdcRange2 = HvdcRange.withAbsoluteRange(hvdc1, 400, 1800);*/

        NetworkElement hvdc2 = new NetworkElement("idHvdc2", "My Hvdc 2");
        //HvdcRange hvdcRange3 = HvdcRange.create(hvdc2, -500, 500, 100, 2000);

        /*List<HvdcRange> hvdcList = new ArrayList<>(Arrays.asList(hvdcRange2));
        HvdcAlignLever hvdcAlignLever1 = new HvdcAlignLever(Arrays.asList(hvdcRange1));
        HvdcAlignLever hvdcAlignLever2 = new HvdcAlignLever(null);
        hvdcAlignLever2.setHvdcRanges(hvdcList);
        hvdcAlignLever2.addHvdcLever(hvdcRange3);*/

        // Topological RA
        NetworkElement line1 = new NetworkElement("idLine1", "My Line 1");
        Topology topology1 = new Topology(line1, OPEN);

        NetworkElement switch1 = new NetworkElement("idSwitch1", "My Switch 1");
        Topology topology2 = new Topology(line1, OPEN);
        topology2.setNetworkElement(switch1);
        topology2.setActionType(CLOSE);

        List<Topology> topoList = new ArrayList<>(Arrays.asList(topology1));
        /*TopologyGroupLever topologyGroupLever = new TopologyGroupLever(null);
        topologyGroupLever.setTopologyModifications(topoList);
        topologyGroupLever.addTopologyModification(topology2);*/

        // Usage rules
        NetworkElement line2 = new NetworkElement("idLine2", "My Line 2");
        NetworkElement line3 = new NetworkElement("idLine3", "My Line 3");

        List<NetworkElement> elementsList = new ArrayList<>(Arrays.asList(line2, line3));
        Contingency contingency = new Contingency("idContingency", "My contingency", null);
        contingency.setNetworkElements(elementsList);
        contingency.addNetworkElement(networkElement1);

        //AbstractUsageRule abstractUsageRule1 = new FreeToUse(FORCED);
        //AbstractUsageRule abstractUsageRule2 = new FreeToUse(AVAILABLE);
        //AbstractUsageRule abstractUsageRule3 = new FreeToUse(AVAILABLE);
        //abstractUsageRule3.setUsageMethod(UNAVAILABLE);

        /*AbstractRemedialAction abstractRemedialAction1 = new AbstractRemedialAction("idRA1", "My Remedial Action 1", topologyGroupLever, null);
        abstractRemedialAction1.setAbstractUsageRules(new ArrayList<>(Arrays.asList(abstractUsageRule1)));
        abstractRemedialAction1.addUsageRule(abstractUsageRule2);
        AbstractRemedialAction abstractRemedialAction2 = new AbstractRemedialAction("idRA2", "My Remedial Action 2", hvdcAlignLever1, new ArrayList<>(Arrays.asList(abstractUsageRule3)));
        abstractRemedialAction2.setNetworkAction(hvdcAlignLever2);

        List<AbstractRemedialAction> abstractRemedialActions = new ArrayList<>(Arrays.asList(abstractRemedialAction1));*/

        Instant basecase = new Instant(0);
        Instant curative = new Instant(200);

        State stateBasecase = new State(Optional.empty(), basecase);
        State stateCurative = new State(Optional.empty(), null);
        stateCurative.setContingency(Optional.of(contingency));
        stateCurative.setInstant(curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        FlowThreshold threshold1 = new FlowThreshold(null, LEFT, IN, 1000);
        threshold1.setUnit(AMPERE);
        threshold1.setSide(RIGHT);
        threshold1.setDirection(OUT);
        threshold1.setMaxValue(999);
        VoltageThreshold threshold2 = new VoltageThreshold(KILOVOLT, 280, 300);
        Cnec cnec1 = new Cnec("idCnec", "Cnec", null, threshold1, stateCurative);
        cnec1.setCriticalNetworkElement(monitoredElement);
        Cnec cnec2 = new Cnec("idCnec2", "Cnec 2", monitoredElement, null, null);
        cnec2.setState(stateBasecase);
        cnec2.setThreshold(threshold2);

        List<Cnec> cnecs = new ArrayList<>();
        cnecs.add(cnec1);

        Crac crac = new Crac("idCrac", "name", new ArrayList<>(), new ArrayList<>());

        crac.setCnecs(cnecs);
        crac.addCnec(cnec2);

        /*crac.setRangeActions(abstractRemedialActions);
        crac.addRemedialAction(abstractRemedialAction2);*/

        crac.getCnecs().forEach(
            cnec -> {
                cnec.getState().getInstant();
                cnec.getState().getContingency();
            });

        crac.getRangeActions().forEach(
            abstractRemedialAction -> abstractRemedialAction.getUsageRules().forEach(
                abstractUsageRule -> {
                    abstractUsageRule.getUsageMethod();
                }));

        assertEquals(crac.getContingencies().size(), 1);
        assertEquals(crac.getCriticalNetworkElements().size(), 2);
        //assertEquals(crac.getRangeActions().size(), 2);

        /*assertFalse(hvdcRange1.hasAbsoluteRange());
        assertTrue(hvdcRange1.hasRelativeRange());
        assertTrue(hvdcRange2.hasAbsoluteRange());
        assertFalse(hvdcRange2.hasRelativeRange());

        assertFalse(pstRange1.hasAbsoluteRange());
        assertTrue(pstRange1.hasRelativeRange());
        assertTrue(pstRange2.hasAbsoluteRange());
        assertFalse(pstRange2.hasRelativeRange());*/

        assertTrue(crac.getId().equals("idCrac"));
        assertTrue(cnec2.isBasecase());
    }
}
