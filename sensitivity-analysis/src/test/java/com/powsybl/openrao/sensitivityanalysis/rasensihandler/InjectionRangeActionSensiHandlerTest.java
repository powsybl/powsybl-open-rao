/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class InjectionRangeActionSensiHandlerTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    @Test
    void checkConsistencyOKTest() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    void getSensitivityOnFlowSimpleTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE2AA12_generator")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, TwoSides.ONE)).thenReturn(-1.56);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, TwoSides.TWO)).thenReturn(-0.56);

        assertEquals(-1.56, sensiHandler.getSensitivityOnFlow(flowCnec, TwoSides.ONE, sensiResult), 1e-3);
        assertEquals(-0.56, sensiHandler.getSensitivityOnFlow(flowCnec, TwoSides.TWO, sensiResult), 1e-3);
    }

    @Test
    void getSensitivityOnFlowComplexTest() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(0.4, "BBE2AA12_generator")
                .withNetworkElementAndKey(0.4, "BBE2AA12_load")
                .withNetworkElementAndKey(-0.2, "FFR3AA12_generator")
                .withNetworkElementAndKey(-0.3, "FFR3AA12_load")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, TwoSides.ONE)).thenReturn(4.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-negativeInjections", flowCnec, TwoSides.ONE)).thenReturn(7.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-positiveInjections", flowCnec, TwoSides.TWO)).thenReturn(10.);
        Mockito.when(sensiResult.getSensitivityOnFlow("injectionRangeId-negativeInjections", flowCnec, TwoSides.TWO)).thenReturn(30.);

        assertEquals(4 * 0.8 - 7 * 0.5, sensiHandler.getSensitivityOnFlow(flowCnec, TwoSides.ONE, sensiResult), 1e-3);
        assertEquals(10 * 0.8 - 30 * 0.5, sensiHandler.getSensitivityOnFlow(flowCnec, TwoSides.TWO, sensiResult), 1e-3);
    }

    @Test
    void checkConsistencyNotAnInjection() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "BBE1AA11 BBE2AA11 1")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for InjectionRangeAction injectionRangeId, on element BBE1AA11 BBE2AA11 1", exception.getMessage());
    }

    @Test
    void checkConsistencyNotANetworkElement() {
        Network network = Network.read("TestCase16NodesWithUcteHvdc.uct", getClass().getResourceAsStream("/TestCase16NodesWithUcteHvdc.uct"));
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction().withId("injectionRangeId")
                .withNetworkElementAndKey(1, "unknown")
                .newRange().withMin(-1000).withMax(1000).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        InjectionRangeActionSensiHandler sensiHandler = new InjectionRangeActionSensiHandler(injectionRangeAction);

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for InjectionRangeAction injectionRangeId, on element unknown", exception.getMessage());
    }
}
