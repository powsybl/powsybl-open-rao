/*
 *
 *  * Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation.createCracWithRemedialActions;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class InjectionSetpointImplTest {

    @Test
    void basicMethods() {
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction injectionSetpoint = crac.newNetworkAction()
            .withId("injectionSetpoint")
            .newInjectionSetPoint()
                .withNetworkElement("element")
                .withSetpoint(10)
                .withUnit(Unit.MEGAWATT)
                .add()
            .add();
        assertEquals(1, injectionSetpoint.getNetworkElements().size());
        assertEquals("element", injectionSetpoint.getNetworkElements().iterator().next().getId());
        assertTrue(injectionSetpoint.canBeApplied(Mockito.mock(Network.class)));
    }

    @Test
    void hasImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("FFR1AA1 _generator")
            .withSetpoint(100)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertTrue(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction generatorSetpoint = crac.newNetworkAction()
            .withId("generatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("FFR1AA1 _generator")
            .withSetpoint(2000)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertFalse(generatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnGenerator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl generatorSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _generator"),
                100, Unit.MEGAWATT);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
        generatorSetpoint.apply(network);
        assertEquals(100., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction loadSetpoint = crac.newNetworkAction()
            .withId("loadSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("FFR1AA1 _load")
            .withSetpoint(100)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertTrue(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction loadSetpoint = crac.newNetworkAction()
            .withId("loadSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("FFR1AA1 _load")
            .withSetpoint(1000)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertFalse(loadSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnLoad() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        InjectionSetpointImpl loadSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("FFR1AA1 _load"),
                100, Unit.MEGAWATT);
        assertEquals(1000., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
        loadSetpoint.apply(network);
        assertEquals(100., network.getLoad("FFR1AA1 _load").getP0(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("DL1")
            .withSetpoint(100)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertTrue(danglingLineSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction danglingLineSetpoint = crac.newNetworkAction()
            .withId("danglingLineSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("DL1")
            .withSetpoint(0)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertFalse(danglingLineSetpoint.hasImpactOnNetwork(network));

    }

    @Test
    void applyOnDanglingLine() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        InjectionSetpointImpl danglingLineSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("DL1"),
                100, Unit.MEGAWATT);
        assertEquals(0., network.getDanglingLine("DL1").getP0(), 1e-3);
        danglingLineSetpoint.apply(network);
        assertEquals(100., network.getDanglingLine("DL1").getP0(), 1e-3);
    }

    @Test
    void hasImpactOnNetworkForShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("SC1")
            .withSetpoint(0)
            .withUnit(Unit.SECTION_COUNT)
            .add()
            .add();
        assertTrue(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void hasNoImpactOnNetworkForShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("SC1")
            .withSetpoint(1)
            .withUnit(Unit.SECTION_COUNT)
            .add()
            .add();
        assertFalse(shuntCompensatorSetpoint.hasImpactOnNetwork(network));
    }

    @Test
    void applyOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        InjectionSetpointImpl shuntCompensatorSetpoint = new InjectionSetpointImpl(
                new NetworkElementImpl("SC1"),
                2, Unit.SECTION_COUNT);
        assertEquals(1., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
        shuntCompensatorSetpoint.apply(network);
        assertEquals(2., network.getShuntCompensator("SC1").getSectionCount(), 1e-3);
    }

    @Test
    void canNotBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("SC1")
            .withSetpoint(3)
            .withUnit(Unit.SECTION_COUNT)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertFalse(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 3
    }

    @Test
    void canBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("SC1")
            .withSetpoint(1)
            .withUnit(Unit.SECTION_COUNT)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 1
    }

    @Test
    void canMaxBeAppliedOnShuntCompensator() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addShuntCompensator(network);
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction shuntCompensatorSetpoint = crac.newNetworkAction()
            .withId("shuntCompensatorSetpoint")
            .newInjectionSetPoint()
            .withNetworkElement("SC1")
            .withSetpoint(2)
            .withUnit(Unit.SECTION_COUNT)
            .add()
            .add();
        assertEquals(2, network.getShuntCompensator("SC1").getMaximumSectionCount());
        assertTrue(shuntCompensatorSetpoint.canBeApplied(network)); // max is 2 while setpoint is 2
    }

    @Test
    void getUnit() {
        InjectionSetpointImpl dummy = new InjectionSetpointImpl(
                new NetworkElementImpl("wrong_name"),
                100, Unit.MEGAWATT);
        assertEquals(Unit.MEGAWATT, dummy.getUnit());
    }

    @Test
    void hasImpactOnNetworkThrow() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = new CracImplFactory().create("cracId");
        NetworkAction dummy = crac.newNetworkAction()
            .withId("dummy")
            .newInjectionSetPoint()
            .withNetworkElement("wrong_name")
            .withSetpoint(100)
            .withUnit(Unit.MEGAWATT)
            .add()
            .add();
        assertThrows(NotImplementedException.class, () -> dummy.hasImpactOnNetwork(network));
    }

    @Test
    void equals() {
        InjectionSetpointImpl injectionSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            10., Unit.MEGAWATT);

        InjectionSetpointImpl sameInjectionSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            10., Unit.MEGAWATT);
        assertEquals(injectionSetpoint, sameInjectionSetpoint);

        InjectionSetpointImpl differentInjectionSetpointOnSetpoint = new InjectionSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            12., Unit.MEGAWATT);
        assertNotEquals(injectionSetpoint, differentInjectionSetpointOnSetpoint);

        InjectionSetpointImpl differentInjectionSetpointOnNetworkElement = new InjectionSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  2"),
            10., Unit.MEGAWATT);
        assertNotEquals(injectionSetpoint, differentInjectionSetpointOnNetworkElement);

        /*InjectionSetpointImpl differentInjectionSetpointOnUnit = new InjectionSetpointImpl(
            new NetworkElementImpl("BBE2AA1  BBE3AA1  1"),
            10., Unit.AMPERE);
        assertNotEquals(injectionSetpoint, differentInjectionSetpointOnUnit);*/ //do not work
    }

    @Test
    void compatibility() {
        Crac crac = createCracWithRemedialActions();
        NetworkAction injectionSetpoint = crac.getNetworkAction("generator-1-75-mw");

        assertTrue(injectionSetpoint.isCompatibleWith(injectionSetpoint));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-1")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("close-switch-2")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-75-mw")));
        assertFalse(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-1-100-mw")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-75-mw")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("generator-2-100-mw")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-1-tap-8")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("pst-2-tap-8")));

        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-2")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-2-close-switch-1")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-4")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-1-close-switch-3")));
        assertTrue(injectionSetpoint.isCompatibleWith(crac.getNetworkAction("open-switch-3-close-switch-2")));
    }
}
