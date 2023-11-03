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
import com.farao_community.farao.data.crac_api.range_action.InjectionRangeAction;
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
class InjectionRangeActionSensiHandlerTest {
    @Test
    void checkConsistencyOKTest() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        InjectionRangeAction injectionRangeAction = (InjectionRangeAction) crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    void getSensitivityOnFlowSimpleTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = (InjectionRangeAction) crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE2AA12_generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, Side.LEFT)).thenReturn(-1.56);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, Side.RIGHT)).thenReturn(-0.56);

        assertEquals(-1.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.LEFT, sensiResult), 1e-3);
        assertEquals(-0.56, sensiHandler.getSensitivityOnFlow(flowCnec, Side.RIGHT, sensiResult), 1e-3);
    }

    @Test
    void getSensitivityOnFlowComplexTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = (InjectionRangeAction) crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, Side.LEFT)).thenReturn(4.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-negativeInjections", flowCnec, Side.LEFT)).thenReturn(7.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, Side.RIGHT)).thenReturn(10.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-negativeInjections", flowCnec, Side.RIGHT)).thenReturn(30.);

        assertEquals(4 * 0.8 - 7 * 0.5, sensiHandler.getSensitivityOnFlow(flowCnec, Side.LEFT, sensiResult), 1e-3);
        assertEquals(10 * 0.8 - 30 * 0.5, sensiHandler.getSensitivityOnFlow(flowCnec, Side.RIGHT, sensiResult), 1e-3);
    }

    @Test
    void checkConsistencyNotAnInjection() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        InjectionRangeAction injectionRangeAction = (InjectionRangeAction) crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE1AA11 BBE2AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        FaraoException exception = assertThrows(FaraoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for InjectionRangeAction injectionRangeId, on element BBE1AA11 BBE2AA11 1", exception.getMessage());
    }

    @Test
    void checkConsistencyNotANetworkElement() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac");
        crac.newInstant("preventive", InstantKind.PREVENTIVE, null);
        InjectionRangeAction injectionRangeAction = (InjectionRangeAction) crac.newInjectionRangeAction().withId("injectionRangeId")
            .withNetworkElementAndKey(1, "unknown")
            .newRange().withMin(-1000).withMax(1000).add()
            .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        FaraoException exception = assertThrows(FaraoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for InjectionRangeAction injectionRangeId, on element unknown", exception.getMessage());
    }
}
