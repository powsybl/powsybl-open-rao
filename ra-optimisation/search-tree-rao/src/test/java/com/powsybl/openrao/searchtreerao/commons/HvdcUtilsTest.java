/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openrao.data.crac.api.*;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.impl.HvdcRangeActionImpl;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class HvdcUtilsTest {

    @Test
    void checkAddNetworkActionAssociatedWithHvdcRangeAction() throws IOException {
        // Two hvdc range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in AC emulation mode, so one
        // AC emulation deactivation network action is created but with usage rule of both range actions.
        Network network = Network.read("TestCase16NodesWithHvdc_AC_emulation.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_AC_emulation.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        addNetworkActionAssociatedWithHvdcRangeAction(crac, network);
        assertEquals(1, crac.getNetworkActions().size());
        assertEquals(8, crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().size());
        assertTrue(crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().containsAll(crac.getHvdcRangeAction("HVDC_RA1").getUsageRules()));
        assertTrue(crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().containsAll(crac.getHvdcRangeAction("HVDC_RA2").getUsageRules()));
    }

    @Test
    void checkNoAcEmulationNetworkActions() throws IOException {
        // Two HVDC range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in fixed point mode
        // So no AC emulation deactivation network action is created
        Network network = Network.read("TestCase16NodesWithHvdc_fixed_setpoint.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_fixed_setpoint.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        addNetworkActionAssociatedWithHvdcRangeAction(crac, network);
        assertEquals(0, crac.getNetworkActions().size());
    }

    @Test
    void testUpdateHvdcRangeActionInitialSetpoint() throws IOException {
        // Two HVDC range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in AC emulation mode
        // the initial set point is set to the initial flow passing on the line.
        Network network = Network.read("TestCase16NodesWithHvdc_AC_emulation.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_AC_emulation.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        // Before
        assertEquals(0.0, crac.getHvdcRangeAction("HVDC_RA1").getInitialSetpoint());
        assertEquals(0.0, crac.getHvdcRangeAction("HVDC_RA2").getInitialSetpoint());

        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        updateHvdcRangeActionInitialSetpoint(crac, network, raoParameters, ReportNode.NO_OP);

        assertEquals(823.0, crac.getHvdcRangeAction("HVDC_RA1").getInitialSetpoint(), 1);
        assertEquals(823.0, crac.getHvdcRangeAction("HVDC_RA2").getInitialSetpoint(), 1);
    }

    @Test
    void testRunLoadFlowAndUpdateHvdcActiveSetpointAfterContingency() throws IOException {
        // Test that check if the runLoadFlowAndUpdateHvdcActiveSetpoint, is able to apply contingency and update the setpoint after applying the contingency.
        // Plus that the returned values correspond to what was applied in network.

        Network network = Network.read("TestCase16NodesWithHvdc_AC_emulation.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_AC_emulation.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
        loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);

        // Before running load flow
        // The setpoint is read directly from the network
        assertEquals(0.0, crac.getHvdcRangeAction("HVDC_RA1").getCurrentSetpoint(network));

        // Preventive state, no contingency is applied
        RaoParameters raoParameters = new RaoParameters(ReportNode.NO_OP);
        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters(ReportNode.NO_OP));
        Map<HvdcRangeAction, Double> hvdcRangeActionActivePowerSetpoint = runLoadFlowAndUpdateHvdcActivePowerSetpoint(
            network,
            crac.getPreventiveState(),
            "OpenLoadFlow",
            loadFlowParameters,
            crac.getHvdcRangeActions().stream().map(HvdcRangeActionImpl.class::cast).collect(Collectors.toSet()),
            ReportNode.NO_OP
        );

        assertEquals(hvdcRangeActionActivePowerSetpoint.get(crac.getHvdcRangeAction("HVDC_RA1")), crac.getHvdcRangeAction("HVDC_RA1").getCurrentSetpoint(network));
        assertEquals(823, hvdcRangeActionActivePowerSetpoint.get(crac.getHvdcRangeAction("HVDC_RA1")), 1);

        // Auto state, after applying contingency
        State autoState = crac.getState("co1_be1_fr5", crac.getInstant(InstantKind.AUTO));
        hvdcRangeActionActivePowerSetpoint = runLoadFlowAndUpdateHvdcActivePowerSetpoint(
            network,
            autoState,
            "OpenLoadFlow",
            loadFlowParameters,
            crac.getHvdcRangeActions().stream().map(HvdcRangeActionImpl.class::cast).collect(Collectors.toSet()),
            ReportNode.NO_OP
        );
        assertEquals(hvdcRangeActionActivePowerSetpoint.get(crac.getHvdcRangeAction("HVDC_RA1")), crac.getHvdcRangeAction("HVDC_RA1").getCurrentSetpoint(network));
        assertEquals(864, hvdcRangeActionActivePowerSetpoint.get(crac.getHvdcRangeAction("HVDC_RA1")), 1);
    }

    @Test
    void testRunLoadFlowAndUpdateHvdcActiveSetpointWithDisconnectedHvdc() throws IOException {
        // same test as testRunLoadFlowAndUpdateHvdcActiveSetpointAfterContingency but with a disconnected HVDC line
        // The setpoint is not valid, the HVDC active power set point is not updated.
        Network network = Network.read("TestCase16NodesWithHvdc_disconnected.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_disconnected.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        OpenLoadFlowParameters openLoadFlowParameters = new OpenLoadFlowParameters();
        loadFlowParameters.addExtension(OpenLoadFlowParameters.class, openLoadFlowParameters);

        Map<HvdcRangeAction, Double> hvdcRangeActionActivePowerSetpoint = runLoadFlowAndUpdateHvdcActivePowerSetpoint(
            network,
            crac.getPreventiveState(),
            "OpenLoadFlow",
            loadFlowParameters,
            crac.getHvdcRangeActions().stream().map(HvdcRangeActionImpl.class::cast).collect(Collectors.toSet()),
            ReportNode.NO_OP
        );
        assertTrue(hvdcRangeActionActivePowerSetpoint.isEmpty());
        assertEquals(0, crac.getHvdcRangeAction("HVDC_RA1").getCurrentSetpoint(network));
    }
}
