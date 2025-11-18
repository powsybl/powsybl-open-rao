/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.openrao.raoapi.RaoInput;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.addNetworkActionAssociatedWithHvdcRangeAction;
import static com.powsybl.openrao.searchtreerao.commons.HvdcUtils.updateHvdcRangeActionInitialSetpoint;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roxane Chen {@literal <roxane.chen at rte-france.com>}
 */
public class HvdcUtilsTest {

    private RaoParameters raoParameters;
    private Network network;
    private Crac crac;
    private String variantId;
    private RaoInput raoInput;

    @BeforeEach
    void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.createWithPreventivePstRange();
        variantId = network.getVariantManager().getWorkingVariantId();
        raoInput = RaoInput.buildWithPreventiveState(network, crac)
            .withNetworkVariantId(variantId)
            .build();
        raoParameters = new RaoParameters();
    }

    @Test
    void checkAddNetworkActionAssociatedWithHvdcRangeAction() throws IOException {
        // Two hvdc range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in ac emulation mode, so one
        // ac emulation deactivation network action is created but with usage rule of both range actions.
        Network network = Network.read("TestCase16NodesWithHvdc_AC_emulation.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_AC_emulation.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        addNetworkActionAssociatedWithHvdcRangeAction(crac, network);
        assert 1 == crac.getNetworkActions().size();
        assert 8 == crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().size();
        assert crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().containsAll(crac.getHvdcRangeAction("ARA_HVDC").getUsageRules());
        assert crac.getNetworkAction("acEmulationDeactivation_BBE2AA11 FFR3AA11 1").getUsageRules().containsAll(crac.getHvdcRangeAction("CRA_HVDC").getUsageRules());
    }

    @Test
    void checkNoAcEmulationNetworkActions() throws IOException {
        // Two hvdc range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in fixed point mode
        // So no ac emulation deaction network action is created
        Network network = Network.read("TestCase16NodesWithHvdc_fixed_setpoint.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_fixed_setpoint.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        addNetworkActionAssociatedWithHvdcRangeAction(crac, network);
        assert 0 == crac.getNetworkActions().size();
    }

    @Test
    void testUpdateHvdcRangeActionInitialSetpoint() throws IOException {
        // Two hvdc range actions both using HVDC line "BBE2AA11 FFR3AA11 1" that is initially in ac emulation mode
        // the initial set point is set to the initial flow passing on the line.

        Network network = Network.read("TestCase16NodesWithHvdc_AC_emulation.xiidm", getClass().getResourceAsStream("/network/TestCase16NodesWithHvdc_AC_emulation.xiidm"));
        Crac crac = Crac.read("crac_hvdc_allinstants_allusagerules.json", getClass().getResourceAsStream("/crac/crac_hvdc_allinstants_allusagerules.json"), network);
        // Before
        assertEquals(0.0, crac.getHvdcRangeAction("ARA_HVDC").getInitialSetpoint());
        assertEquals(0.0, crac.getHvdcRangeAction("CRA_HVDC").getInitialSetpoint());

        raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
        updateHvdcRangeActionInitialSetpoint(crac, network, raoParameters);

        assertEquals(823.0, crac.getHvdcRangeAction("ARA_HVDC").getInitialSetpoint(), 1);
        assertEquals(823.0, crac.getHvdcRangeAction("CRA_HVDC").getInitialSetpoint(), 1);
    }


}