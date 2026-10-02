/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.action.Action;
import com.powsybl.action.AreaInterchangeTargetAction;
import com.powsybl.action.GeneratorActionBuilder;
import com.powsybl.action.HvdcActionBuilder;
import com.powsybl.action.LoadActionBuilder;
import com.powsybl.action.PhaseTapChangerTapPositionAction;
import com.powsybl.action.RatioTapChangerTapPositionAction;
import com.powsybl.action.ShuntCompensatorPositionActionBuilder;
import com.powsybl.action.StaticVarCompensatorActionBuilder;
import com.powsybl.action.SwitchAction;
import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class OperatorStrategySupportTest {

    private static final OperatorStrategySupport.Mode DC = new OperatorStrategySupport.Mode(true, false);
    private static final OperatorStrategySupport.Mode DC_DISTRIBUTED_SLACK = new OperatorStrategySupport.Mode(true, true);
    private static final OperatorStrategySupport.Mode AC = new OperatorStrategySupport.Mode(false, true);

    @Test
    void testMode() {
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.getLoadFlowParameters().setDc(true).setDistributedSlack(true);
        assertEquals(DC_DISTRIBUTED_SLACK, OperatorStrategySupport.Mode.of(parameters));
        assertEquals("DC with distributed slack", DC_DISTRIBUTED_SLACK.toString());
        assertEquals("AC with distributed slack", AC.toString());
        assertEquals("DC", DC.toString());
    }

    @Test
    void testIsSupported() {
        Action pstTap = new PhaseTapChangerTapPositionAction("pst", "transformer", false, 3);
        Action terminals = new TerminalsConnectionAction("line", "line", true);
        Action switchAction = new SwitchAction("switch", "switch", true);
        Action generator = new GeneratorActionBuilder().withId("gen").withGeneratorId("gen").withActivePowerRelativeValue(false).withActivePowerValue(100.).build();
        Action load = new LoadActionBuilder().withId("load").withLoadId("load").withRelativeValue(false).withActivePowerValue(100.).build();
        Action ratioTap = new RatioTapChangerTapPositionAction("rtc", "transformer", false, 3);
        Action shunt = new ShuntCompensatorPositionActionBuilder().withId("shunt").withShuntCompensatorId("shunt").withSectionCount(2).build();
        Action area = new AreaInterchangeTargetAction("area", "area", 100.);
        Action hvdcSetpoint = new HvdcActionBuilder().withId("hvdc1").withHvdcId("hvdc").withActivePowerSetpoint(100.).build();
        Action hvdcDisableAcEmulation = new HvdcActionBuilder().withId("hvdc2").withHvdcId("hvdc").withAcEmulationEnabled(false).build();
        Action hvdcEnableAcEmulation = new HvdcActionBuilder().withId("hvdc3").withHvdcId("hvdc").withAcEmulationEnabled(true).build();
        Action hvdcNoEffect = new HvdcActionBuilder().withId("hvdc4").withHvdcId("hvdc").withDroop(100.).build();
        Action svc = new StaticVarCompensatorActionBuilder().withId("svc").withStaticVarCompensatorId("svc").withVoltageSetpoint(400.).build();

        // always supported
        for (Action action : List.of(pstTap, terminals, switchAction)) {
            for (OperatorStrategySupport.Mode mode : List.of(DC, DC_DISTRIBUTED_SLACK, AC)) {
                assertTrue(OperatorStrategySupport.isSupported(action, mode), action.getId());
            }
        }
        // injections: not in DC with distributed slack
        for (Action action : List.of(generator, load)) {
            assertTrue(OperatorStrategySupport.isSupported(action, DC), action.getId());
            assertFalse(OperatorStrategySupport.isSupported(action, DC_DISTRIBUTED_SLACK), action.getId());
            assertTrue(OperatorStrategySupport.isSupported(action, AC), action.getId());
        }
        // AC only
        for (Action action : List.of(ratioTap, shunt, area, hvdcSetpoint, hvdcDisableAcEmulation)) {
            assertFalse(OperatorStrategySupport.isSupported(action, DC), action.getId());
            assertTrue(OperatorStrategySupport.isSupported(action, AC), action.getId());
        }
        // never supported
        for (Action action : List.of(hvdcEnableAcEmulation, hvdcNoEffect, svc)) {
            assertFalse(OperatorStrategySupport.isSupported(action, DC), action.getId());
            assertFalse(OperatorStrategySupport.isSupported(action, AC), action.getId());
        }
    }

    @Test
    void testExistsInNetwork() {
        Network network = NetworkImportsUtil.import16NodesNetworkWithHvdc();
        String pst = "BBE2AA11 BBE3AA11 1";
        String line = "BBE1AA11 BBE2AA11 1";
        String generator = "BBE1AA11_generator";
        String load = "BBE1AA11_load";
        String hvdc = "BBE2AA11 FFR3AA11 1";

        assertTrue(OperatorStrategySupport.existsInNetwork(new TerminalsConnectionAction("a", line, true), network));
        assertTrue(OperatorStrategySupport.existsInNetwork(new PhaseTapChangerTapPositionAction("a", pst, false, 1), network));
        assertTrue(OperatorStrategySupport.existsInNetwork(new RatioTapChangerTapPositionAction("a", pst, false, 1), network));
        assertTrue(OperatorStrategySupport.existsInNetwork(
            new GeneratorActionBuilder().withId("a").withGeneratorId(generator).withActivePowerRelativeValue(false).withActivePowerValue(1.).build(), network));
        assertTrue(OperatorStrategySupport.existsInNetwork(new LoadActionBuilder().withId("a").withLoadId(load).withRelativeValue(false).withActivePowerValue(1.).build(), network));
        assertTrue(OperatorStrategySupport.existsInNetwork(new HvdcActionBuilder().withId("a").withHvdcId(hvdc).withActivePowerSetpoint(1.).build(), network));

        for (Action unknown : List.of(
            new TerminalsConnectionAction("a", "unknown", true),
            new SwitchAction("a", "unknown", true),
            new PhaseTapChangerTapPositionAction("a", "unknown", false, 1),
            new RatioTapChangerTapPositionAction("a", "unknown", false, 1),
            new GeneratorActionBuilder().withId("a").withGeneratorId("unknown").withActivePowerRelativeValue(false).withActivePowerValue(1.).build(),
            new LoadActionBuilder().withId("a").withLoadId("unknown").withRelativeValue(false).withActivePowerValue(1.).build(),
            new HvdcActionBuilder().withId("a").withHvdcId("unknown").withActivePowerSetpoint(1.).build(),
            new ShuntCompensatorPositionActionBuilder().withId("a").withShuntCompensatorId("unknown").withSectionCount(1).build(),
            new AreaInterchangeTargetAction("a", "unknown", 1.),
            new StaticVarCompensatorActionBuilder().withId("a").withStaticVarCompensatorId("unknown").withVoltageSetpoint(400.).build())) {
            assertFalse(OperatorStrategySupport.existsInNetwork(unknown, network), unknown.getId());
        }
        assertFalse(OperatorStrategySupport.isSupported(new TerminalsConnectionAction("a", "unknown", true), network, DC));
        assertTrue(OperatorStrategySupport.isSupported(new TerminalsConnectionAction("a", line, true), network, DC));
    }
}
