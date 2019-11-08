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

        Instant basecase = new Instant(0);
        Instant curative = new Instant(200);

        NetworkElement networkElementCo1 = new NetworkElement("idElementCo1", "Contingency Element 1");
        NetworkElement networkElementCo2 = new NetworkElement("idElementCo2", "Contingency Element 2");

        List<NetworkElement> contingenciesElements = new ArrayList<>();
        contingenciesElements.add(networkElementCo1);

        Contingency contingency = new Contingency("idContingency", "Contingency", contingenciesElements);

        State stateBasecase = new State(Optional.empty(), basecase);
        State stateCurative = new State(Optional.of(contingency), curative);

        NetworkElement monitoredElement = new NetworkElement("idMR", "Monitored Element");

        FlowViolation threshold1 = new FlowViolation(AMPERE, LEFT, IN, 1000);
        SafetyInterval threshold2 = new SafetyInterval(KILOVOLT, 280, 300);

        Cnec cnec = new Cnec("idCnec", "Cnec", monitoredElement, threshold1, stateCurative);

        List<Cnec> cnecs = new ArrayList<>();
        cnecs.add(cnec);

        FreeToUse usageContext = new FreeToUse();
        UsageRule usageRule = new UsageRule(FORCED, usageContext);

        NetworkElement networkElementRa = new NetworkElement("idElementRa", "Element RA");

        PstLever pstLever = PstLever.withAbsoluteRange(networkElementRa, 3, 14);
        PstAlignLever pstAlignLever = new PstAlignLever(Arrays.asList(pstLever));
        HvdcLever hvdcLever = HvdcLever.withAbsoluteRange(networkElementRa, 0, 1000);
        HvdcAlignLever hvdcAlignLever = new HvdcAlignLever(Arrays.asList(hvdcLever));

        //RemedialAction remedialAction = new RemedialAction("idRA", "Remedial Action", Arrays.asList(pstAlignLever), Arrays.asList(usageRule));
        //remedialAction.addRemedialActionLever(hvdcAlignLever);

        List<RemedialAction> remedialActions = new ArrayList<>();
        //remedialActions.add(remedialAction);

        Crac crac = new Crac("idCrac", "name", new ArrayList<>(), new ArrayList<>());

        crac.setCnecs(cnecs);
        crac.setRemedialActions(remedialActions);

        assertEquals(true, crac.getId().equals("idCrac"));
    }

    @Test
    public void testRemedialActionCreation() {

        // Redispatching
        NetworkElement generator = new NetworkElement("idGenerator", "My Generator");
        Redispatching rd = new Redispatching(10, 20, 18, 1000, 12, generator);
        rd.setMinimumPower(11);

        // PstLever
        NetworkElement pst1 = new NetworkElement("idPst1", "My Pst 1");
        PstLever pstLever1 = PstLever.withRelativeRange(pst1, 2, 8);
        PstLever pstLever2 = PstLever.withAbsoluteRange(pst1, 4, 18);

        NetworkElement pst2 = new NetworkElement("idPst2", "My Pst 2");
        PstLever pstLever3 = PstLever.create(pst2, -5, 5, 10, 20);

        List<PstLever> pstList = new ArrayList<>(Arrays.asList(pstLever2));
        PstAlignLever pstAlignLever1 = new PstAlignLever(Arrays.asList(pstLever1));
        PstAlignLever pstAlignLever2 = new PstAlignLever(pstList);
        pstAlignLever2.addPstLever(pstLever3);

        // HvdcLever
        NetworkElement hvdc1 = new NetworkElement("idHvdc1", "My Hvdc 1");
        HvdcLever hvdcLever1 = HvdcLever.withRelativeRange(hvdc1, 200, 800);
        HvdcLever hvdcLever2 = HvdcLever.withAbsoluteRange(hvdc1, 400, 1800);

        NetworkElement hvdc2 = new NetworkElement("idHvdc2", "My Hvdc 2");
        HvdcLever hvdcLever3 = HvdcLever.create(hvdc2, -500, 500, 100, 2000);

        List<HvdcLever> hvdcList = new ArrayList<>(Arrays.asList(hvdcLever2));
        HvdcAlignLever hvdcAlignLever1 = new HvdcAlignLever(Arrays.asList(hvdcLever1));
        HvdcAlignLever hvdcAlignLever2 = new HvdcAlignLever(hvdcList);
        hvdcAlignLever2.addHvdcLever(hvdcLever3);

        // Topological RA
        NetworkElement line1 = new NetworkElement("idLine1", "My Line 1");
        TopologyModification topologyModification1 = new TopologyModification(line1, OPEN);

        NetworkElement switch1 = new NetworkElement("idSwitch1", "My Switch 1");
        TopologyModification topologyModification2 = new TopologyModification(switch1, CLOSE);

        List<TopologyModification> topoList = new ArrayList<>(Arrays.asList(topologyModification1));
        TopologyGroupLever topologyGroupLever = new TopologyGroupLever(topoList);
        topologyGroupLever.addTopologyModification(topologyModification2);

        // Usage rules
        NetworkElement line2 = new NetworkElement("idLine2", "My Line 2");
        NetworkElement line3 = new NetworkElement("idLine3", "My Line 3");

        List<NetworkElement> elementsList = new ArrayList<>(Arrays.asList(line2, line3));
        Contingency contingency = new Contingency("idContingency", "My contingency", elementsList);
        OnContingency contextOnContingency = new OnContingency(contingency);

        // TODO : OnConstraint

        FreeToUse contextFreeToUse = new FreeToUse();

        UsageRule usageRule1 = new UsageRule(FORCED, contextOnContingency);
        UsageRule usageRule2 = new UsageRule(AVAILABLE, contextOnContingency);
        UsageRule usageRule3 = new UsageRule(UNAVAILABLE, contextOnContingency);
    }

}
