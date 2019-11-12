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
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.data.crac_api.ActionType.*;
import static com.farao_community.farao.data.crac_api.Direction.*;
import static com.farao_community.farao.data.crac_api.Side.*;
import static com.farao_community.farao.data.crac_api.Unit.*;
import static com.farao_community.farao.data.crac_api.UsageMethod.*;
import static org.junit.Assert.*;

public class CracTest {

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
        PstLever pstLever1 = PstLever.withRelativeRange(pst1, 2, 8);
        PstLever pstLever2 = PstLever.withAbsoluteRange(null, 4, 18);
        pstLever2.setNetworkElement(pst1);

        NetworkElement pst2 = new NetworkElement("idPst2", "My Pst 2");
        PstLever pstLever3 = PstLever.create(pst2, -5, 5, 10, 20);

        List<PstLever> pstLeverList = new ArrayList<>(Arrays.asList(pstLever2));
        PstAlignLever pstAlignLever1 = new PstAlignLever(Arrays.asList(pstLever1));
        PstAlignLever pstAlignLever2 = new PstAlignLever(null);
        pstAlignLever2.setPstLevers(pstLeverList);
        pstAlignLever2.addPstLever(pstLever3);

        // HvdcLever
        NetworkElement hvdc1 = new NetworkElement("idHvdc1", "My Hvdc 1");
        HvdcLever hvdcLever1 = HvdcLever.withRelativeRange(null, 200, 800);
        hvdcLever1.setNetworkElement(hvdc1);
        HvdcLever hvdcLever2 = HvdcLever.withAbsoluteRange(hvdc1, 400, 1800);

        NetworkElement hvdc2 = new NetworkElement("idHvdc2", "My Hvdc 2");
        HvdcLever hvdcLever3 = HvdcLever.create(hvdc2, -500, 500, 100, 2000);

        List<HvdcLever> hvdcList = new ArrayList<>(Arrays.asList(hvdcLever2));
        HvdcAlignLever hvdcAlignLever1 = new HvdcAlignLever(Arrays.asList(hvdcLever1));
        HvdcAlignLever hvdcAlignLever2 = new HvdcAlignLever(null);
        hvdcAlignLever2.setHvdcLevers(hvdcList);
        hvdcAlignLever2.addHvdcLever(hvdcLever3);

        // Topological RA
        NetworkElement line1 = new NetworkElement("idLine1", "My Line 1");
        TopologyModification topologyModification1 = new TopologyModification(line1, OPEN);

        NetworkElement switch1 = new NetworkElement("idSwitch1", "My Switch 1");
        TopologyModification topologyModification2 = new TopologyModification(line1, OPEN);
        topologyModification2.setNetworkElement(switch1);
        topologyModification2.setActionType(CLOSE);

        List<TopologyModification> topoList = new ArrayList<>(Arrays.asList(topologyModification1));
        TopologyGroupLever topologyGroupLever = new TopologyGroupLever(null);
        topologyGroupLever.setTopologyModifications(topoList);
        topologyGroupLever.addTopologyModification(topologyModification2);

        // Usage rules
        NetworkElement line2 = new NetworkElement("idLine2", "My Line 2");
        NetworkElement line3 = new NetworkElement("idLine3", "My Line 3");

        List<NetworkElement> elementsList = new ArrayList<>(Arrays.asList(line2, line3));
        Contingency contingency = new Contingency("idContingency", "My contingency", null);
        contingency.setNetworkElements(elementsList);
        contingency.addNetworkElement(networkElement1);

        FreeToUse contextFreeToUse = new FreeToUse();

        UsageRule usageRule1 = new UsageRule(FORCED, contextFreeToUse);
        UsageRule usageRule2 = new UsageRule(AVAILABLE, contextFreeToUse);
        UsageRule usageRule3 = new UsageRule(AVAILABLE, null);
        usageRule3.setUsageMethod(UNAVAILABLE);
        usageRule3.setUsageContext(contextFreeToUse);

        RemedialAction remedialAction1 = new RemedialAction("idRA1", "My Remedial Action 1", topologyGroupLever, null);
        remedialAction1.setUsageRules(new ArrayList<>(Arrays.asList(usageRule1)));
        remedialAction1.addUsageRule(usageRule2);
        RemedialAction remedialAction2 = new RemedialAction("idRA2", "My Remedial Action 2", hvdcAlignLever1, new ArrayList<>(Arrays.asList(usageRule3)));
        remedialAction2.setRemedialActionLever(hvdcAlignLever2);

        List<RemedialAction> remedialActions = new ArrayList<>(Arrays.asList(remedialAction1));

        Instant basecase = new Instant(0);
        Instant curative = new Instant(200);

        State stateBasecase = new State(Optional.empty(), basecase);
        State stateCurative = new State(Optional.empty(), null);
        stateCurative.setContingency(Optional.of(contingency));
        stateCurative.setInstant(curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        FlowViolation threshold1 = new FlowViolation(null, LEFT, IN, 1000);
        threshold1.setUnit(AMPERE);
        threshold1.setSide(RIGHT);
        threshold1.setDirection(OUT);
        threshold1.setMaxValue(999);
        SafetyInterval threshold2 = new SafetyInterval(KILOVOLT, 280, 300);
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

        crac.setRemedialActions(remedialActions);
        crac.addRemedialAction(remedialAction2);

        crac.getCnecs().forEach(
            cnec -> {
                cnec.getState().getInstant();
                cnec.getState().getContingency();
            });

        crac.getRemedialActions().forEach(
            remedialAction -> remedialAction.getUsageRules().forEach(
                usageRule -> {
                    usageRule.getUsageContext();
                    usageRule.getUsageMethod();
                }));

        assertEquals(crac.getContingencies().size(), 1);
        assertEquals(crac.getCriticalNetworkElements().size(), 2);
        assertEquals(crac.getRemedialActions().size(), 2);

        assertFalse(hvdcLever1.hasAbsoluteRange());
        assertTrue(hvdcLever1.hasRelativeRange());
        assertTrue(hvdcLever2.hasAbsoluteRange());
        assertFalse(hvdcLever2.hasRelativeRange());

        assertFalse(pstLever1.hasAbsoluteRange());
        assertTrue(pstLever1.hasRelativeRange());
        assertTrue(pstLever2.hasAbsoluteRange());
        assertFalse(pstLever2.hasRelativeRange());

        assertTrue(crac.getId().equals("idCrac"));
        assertTrue(cnec2.isBasecase());
    }
}
