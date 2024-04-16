/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.PhaseTapChangerTapPositionActionBuilder;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
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
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (PowsyblException e) {
            assertEquals("2 windings transformer 'BBE2AA1  BBE3AA1  1': incorrect tap position 17 [-16, 16]", e.getMessage());
        }
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
        try {
            pstSetpoint.apply(network);
            fail();
        } catch (PowsyblException e) {
            assertEquals("2 windings transformer 'BBE2AA1  BBE3AA1  1': incorrect tap position 50 [-16, 16]", e.getMessage());
        }
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
            new HashSet<>(List.of(phaseTapChangerTapPositionAction, samePhaseTapChangerTapPositionAction)), 0, Set.of());
        assertEquals(1, dummy5.getElementaryActions().size());
    }
}
