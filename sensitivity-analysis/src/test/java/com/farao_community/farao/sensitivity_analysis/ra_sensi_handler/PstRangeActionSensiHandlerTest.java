/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis.ra_sensi_handler;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.range.RangeType;
import com.farao_community.farao.data.crac_api.range_action.PstRangeAction;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.powsybl.iidm.network.Network;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class PstRangeActionSensiHandlerTest {

    @Test
    public void checkConsistencyOKTest() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(crac.getPstRangeAction("pst"));

        sensiHandler.checkConsistency(network); // should not throw
    }

    @Test
    public void getSensitivityOnFlowTest() {
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        FlowCnec anyFLowCnec = crac.getFlowCnec("cnec1basecase");
        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(crac.getPstRangeAction("pst"));

        SystematicSensitivityResult sensiResult = Mockito.mock(SystematicSensitivityResult.class);
        Mockito.when(sensiResult.getSensitivityOnFlow("BBE2AA1  BBE3AA1  1", anyFLowCnec)).thenReturn(14.32);

        assertEquals(14.32, sensiHandler.getSensitivityOnFlow(anyFLowCnec, sensiResult), 1e-3);
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotAPst() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("pstOnBranch")
                .withNetworkElement("BBE1AA1  BB23AA1  1")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(crac.getPstRangeAction("pst").getTapToAngleConversionMap())
                .add();

        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(pstRangeAction);
        sensiHandler.checkConsistency(network); // should throw
    }

    @Test (expected = FaraoException.class)
    public void checkConsistencyNotANetworkElement() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        PstRangeAction pstRangeAction = crac.newPstRangeAction()
                .withId("pstOnNonExistingElement")
                .withNetworkElement("unknown")
                .newFreeToUseUsageRule().withInstant(Instant.PREVENTIVE).withUsageMethod(UsageMethod.AVAILABLE).add()
                .newTapRange().withRangeType(RangeType.ABSOLUTE).withMinTap(-16).withMaxTap(16).add()
                .withInitialTap(0)
                .withTapToAngleConversionMap(crac.getPstRangeAction("pst").getTapToAngleConversionMap())
                .add();

        PstRangeActionSensiHandler sensiHandler = new PstRangeActionSensiHandler(pstRangeAction);
        sensiHandler.checkConsistency(network); // should throw
    }
}
