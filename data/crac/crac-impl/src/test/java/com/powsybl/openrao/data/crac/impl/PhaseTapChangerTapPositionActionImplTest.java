/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pauline JEAN-MARIE {@literal <pauline.jean-marie at artelys.com>}
 */
class PhaseTapChangerTapPositionActionImplTest {

    private Network network;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
    }

    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
                .withNetworkElement("BBE2AA1  BBE3AA1  1")
                .withTapPosition(12)
                .add()
            .add();
        assertEquals(1, pstSetpoint.getNetworkElements().size());
        assertEquals("BBE2AA1  BBE3AA1  1", pstSetpoint.getNetworkElements().iterator().next().getId());
        assertTrue(pstSetpoint.canBeApplied(network));
    }

    @Test
    void hasImpactOnNetwork() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(-9)
            .add()
            .add();
        assertTrue(pstSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetwork() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(0)
            .add()
            .add();
        assertFalse(pstSetpoint.hasImpactOnNetwork(network));

    }

    @Test
    void applyCenteredOnZero() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(-9)
            .add()
            .add();
        assertEquals(0, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        pstSetpoint.apply(network);
        assertEquals(-9, network.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
    }

    @Test
    void applyOutOfBoundStartsAtOne() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(17)
            .add()
            .add();
        assertFalse(pstSetpoint.apply(network));
    }

    @Test
    void applyOutOfBoundCenteredOnZero() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction pstSetpoint = crac.newNetworkAction()
            .withId("pstSetpoint")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(50)
            .add()
            .add();
        assertFalse(pstSetpoint.apply(network));
    }

    @Test
    void equals() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  1")
            .withTapPosition(-9)
            .add()
            .add();
        assertEquals(1, dummy.getElementaryActions().size());

        NetworkAction dummy2 = crac.newNetworkAction()
            .withId("dummy2")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withTapPosition(-9)
            .add()
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  2")
            .withTapPosition(-9)
            .add()
            .add();
        assertEquals(1, dummy2.getElementaryActions().size());

        NetworkAction dummy3 = crac.newNetworkAction()
            .withId("dummy3")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withTapPosition(-9)
            .add()
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  3")
            .withTapPosition(-10)
            .add()
            .add();
        assertEquals(2, dummy3.getElementaryActions().size());

        NetworkAction dummy4 = crac.newNetworkAction()
            .withId("dummy4")
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  4")
            .withTapPosition(-9)
            .add()
            .newPhaseTapChangerTapPositionAction()
            .withNetworkElement("BBE2AA1  BBE3AA1  5")
            .withTapPosition(-9)
            .add()
            .add();
        assertEquals(2, dummy4.getElementaryActions().size());

        PhaseTapChangerTapPositionAction phaseTapChangerTapPositionAction = new PhaseTapChangerTapPositionActionBuilder().withId("id").withNetworkElementId("T1").withTapPosition(-9).withRelativeValue(false).build();
        PhaseTapChangerTapPositionAction samePhaseTapChangerTapPositionAction = new PhaseTapChangerTapPositionActionBuilder().withId("id").withNetworkElementId("T1").withTapPosition(-9).withRelativeValue(false).build();
        assertEquals(phaseTapChangerTapPositionAction, samePhaseTapChangerTapPositionAction);
        NetworkAction dummy5 = new NetworkActionImpl("id", "name", "operator", null,
            new HashSet<>(List.of(phaseTapChangerTapPositionAction, samePhaseTapChangerTapPositionAction)), 0, null, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }

    @Test
    void compatibility() {
        Crac crac = CommonCracCreation.createCracWithRemedialActions();
        NetworkAction pstSetpoint = crac.getNetworkAction("pst-1-tap-3");

        assertTrue(pstSetpoint.isCompatibleWith(pstSetpoint));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-1")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-2")));

        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw")));

        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3")));
        assertFalse(pstSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8")));

        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3")));
        assertTrue(pstSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2")));
    }
}
