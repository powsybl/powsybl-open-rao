/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.open_rao.sensitivity_analysis.ra_sensi_handler;

import com.powsybl.open_rao.commons.OpenRaoException;
import com.powsybl.open_rao.data.crac_api.Crac;
import com.powsybl.open_rao.data.crac_api.cnec.FlowCnec;
import com.powsybl.open_rao.data.crac_api.cnec.Side;
import com.powsybl.open_rao.data.crac_api.range.RangeType;
import com.powsybl.open_rao.data.crac_api.range_action.PstRangeAction;
import com.powsybl.open_rao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.open_rao.data.crac_impl.utils.CommonCracCreation;
import com.powsybl.open_rao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.open_rao.sensitivity_analysis.SystematicSensitivityResult;
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
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
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
                .newOnInstantUsageRule().withInstant("preventive").withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(crac.getPstRangeAction("pst").getTapToAngleConversionMap())
                .add();

        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(pstRangeAction);
        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> sensiHandler.checkConsistency(network));
        assertEquals("Unable to create sensitivity variable for PstRangeAction pstOnNonExistingElement, on element unknown", exception.getMessage());
    }
}
