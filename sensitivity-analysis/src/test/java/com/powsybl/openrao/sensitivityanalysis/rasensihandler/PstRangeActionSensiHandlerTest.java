/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.sensitivityanalysis.rasensihandler;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.cnec.Side;
import com.powsybl.openrao.data.cracapi.range.RangeType;
import com.powsybl.openrao.data.cracapi.rangeaction.PstRangeAction;
import com.powsybl.openrao.data.cracapi.triggercondition.UsageMethod;
import com.powsybl.openrao.data.cracimpl.utils.CommonCracCreation;
import com.powsybl.openrao.data.cracimpl.utils.NetworkImportsUtil;
import com.powsybl.openrao.sensitivityanalysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class PstRangeActionSensiHandlerTest {

    @Test
    void checkConsistencyOKTest() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(crac.getPstRangeAction("pst"));

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    void getSensitivityOnFlowTest() {
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        FlowCnec flowCnec = crac.getFlowCnec("cnec1basecase");
        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(crac.getPstRangeAction("pst"));

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA1  BBE3AA1  1", flowCnec, Side.LEFT)).thenReturn(14.32);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA1  BBE3AA1  1", flowCnec, Side.RIGHT)).thenReturn(104.32);

        assertEquals(14.32, sensiHandler.getSensitivityOnFlow(flowCnec, Side.LEFT, sensiResult), 1e-3);
        assertEquals(104.32, sensiHandler.getSensitivityOnFlow(flowCnec, Side.RIGHT, sensiResult), 1e-3);
    }

    @Test
    void checkConsistencyNotAPst() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("pstOnBranch")
                .withNetworkElement("BBE1AA1  BB23AA1  1")
                .newTriggerCondition().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(crac.getPstRangeAction("pst").getTapToAngleConversionMap())
                .add();

        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(pstRangeAction);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for PstRangeAction pstOnBranch, on element BBE1AA1  BB23AA1  1", exception.getMessage());
    }

    @Test
    void checkConsistencyNotANetworkElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("pstOnNonExistingElement")
                .withNetworkElement("unknown")
                .newTriggerCondition().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(crac.getPstRangeAction("pst").getTapToAngleConversionMap())
                .add();

        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(pstRangeAction);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for PstRangeAction pstOnNonExistingElement, on element unknown", exception.getMessage());
    }
}
