/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.remedial_action.range_action.PstWithRange;
import com.farao_community.farao.data.crac_impl.usage_rule.OnState;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class PerimetersTest {

    private SimpleCrac crac;
    private Network network;
    private StateTree stateTree;

    @Before
    public void setUp() {
        network = Mockito.mock(Network.class);

        crac = new SimpleCrac("crac-id");
        crac.newInstant().setId("N").setSeconds(0).add();
        crac.newInstant().setId("Outage").setSeconds(60).add();
        crac.newInstant().setId("Curative").setSeconds(500).add();
        crac.newContingency().setId("contingency-1").add();
        crac.newContingency().setId("contingency-2").add();
        crac.newContingency().setId("contingency-3").add();
        crac.newCnec()
            .setInstant(crac.getInstant("N"))
            .setId("cnec1-preventive")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(200.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Outage"))
            .setContingency(crac.getContingency("contingency-1"))
            .setId("cnec1-outage1")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(400.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Curative"))
            .setContingency(crac.getContingency("contingency-1"))
            .setId("cnec1-curative1")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(200.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Outage")).
            setContingency(crac.getContingency("contingency-2"))
            .setId("cnec1-outage2")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(500.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Curative"))
            .setContingency(crac.getContingency("contingency-2"))
            .setId("cnec1-curative2")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(200.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Outage"))
            .setContingency(crac.getContingency("contingency-3"))
            .setId("cnec1-outage3")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(200.).setDirection(Direction.BOTH).add()
            .add();
        crac.newCnec()
            .setInstant(crac.getInstant("Curative"))
            .setContingency(crac.getContingency("contingency-3"))
            .setId("cnec1-curative3")
            .newNetworkElement().setId("ne1").add()
            .newThreshold().setSide(Side.LEFT).setUnit(Unit.AMPERE).setMaxValue(200.).setDirection(Direction.BOTH).add()
            .add();

        stateTree = new StateTree();
    }

    @Test
    public void testCreatePerimetersWithNoRemedialActions() {
        stateTree.createPerimeters(crac, network, crac.getPreventiveState());
        assertEquals(1, stateTree.getOptimizedStates().size());
        assertEquals(7, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnOutage() {
        PstRange pstRange = new PstWithRange("pst-ra", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-1", "Outage")));
        crac.addRangeAction(pstRange);
        stateTree.createPerimeters(crac, network, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(5, stateTree.getPerimeter(crac.getPreventiveState()).size());
        assertEquals(2, stateTree.getPerimeter(crac.getState("contingency-1", "Outage")).size());
    }

    @Test
    public void testCreatePerimetersWithOneRemedialActionOnCurative() {
        PstRange pstRange = new PstWithRange("pst-ra", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-1", "Curative")));
        crac.addRangeAction(pstRange);
        stateTree.createPerimeters(crac, network, crac.getPreventiveState());
        assertEquals(2, stateTree.getOptimizedStates().size());
        assertEquals(6, stateTree.getPerimeter(crac.getPreventiveState()).size());
        assertEquals(1, stateTree.getPerimeter(crac.getState("contingency-1", "Curative")).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActions() {
        PstRange pstRange1 = new PstWithRange("pst-ra1", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange1.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-1", "Curative")));
        crac.addRangeAction(pstRange1);
        PstRange pstRange2 = new PstWithRange("pst-ra2", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange2.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-2", "Outage")));
        crac.addRangeAction(pstRange2);
        stateTree.createPerimeters(crac, network, crac.getPreventiveState());
        assertEquals(3, stateTree.getOptimizedStates().size());
        assertEquals(4, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }

    @Test
    public void testCreatePerimetersWithTwoRemedialActionsOnSameContingency() {
        PstRange pstRange1 = new PstWithRange("pst-ra1", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange1.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-2", "Curative")));
        crac.addRangeAction(pstRange1);
        PstRange pstRange2 = new PstWithRange("pst-ra2", crac.addNetworkElement(new NetworkElement("pst1")));
        pstRange2.addUsageRule(new OnState(UsageMethod.AVAILABLE, crac.getState("contingency-2", "Outage")));
        crac.addRangeAction(pstRange2);
        stateTree.createPerimeters(crac, network, crac.getPreventiveState());
        assertEquals(3, stateTree.getOptimizedStates().size());
        assertEquals(5, stateTree.getPerimeter(crac.getPreventiveState()).size());
    }
}
