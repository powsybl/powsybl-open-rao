/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.range_action.HvdcRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class HvdcRangeActionSensiHandlerTest {

    @Test
    void checkConsistencyOKTest() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE);
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    void getSensitivityOnFlowTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE2AA11 FFR3AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA11 FFR3AA11 1", flowCnec, Side.LEFT)).thenReturn(-12.56);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA11 FFR3AA11 1", flowCnec, Side.RIGHT)).thenReturn(-10.56);

        assertEquals(-12.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.LEFT, sensiResult), 1e-3);
        assertEquals(-10.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.RIGHT, sensiResult), 1e-3);
    }

    @Test
    void checkConsistencyNotAHvdc() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE);

        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("BBE1AA11 BBE2AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        FaraoException exception = assertThrows(FaraoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for HvdcRangeAction hvdcRangeId, on element BBE1AA11 BBE2AA11 1", exception.getMessage());
    }

    @Test
    void checkConsistencyNotANetworkElement() {
        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant("preventive", InstantKind.PREVENTIVE);

        HvdcRangeAction hvdcRangeAction = (HvdcRangeAction) crac.newHvdcRangeAction().withId("hvdcRangeId")
                .withNetworkElement("unknownNetworkElement")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        HvdcRangeActionSensiHandler sensiHandler = new HvdcRangeActionSensiHandler(hvdcRangeAction);

        FaraoException exception = assertThrows(FaraoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for HvdcRangeAction hvdcRangeId, on element unknownNetworkElement", exception.getMessage());
    }
}
