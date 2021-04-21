/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class XnodeGlskHandlerTest {

    @Test
    public void test() {
        String networkFileName = "network_with_virtual_hubs.xiidm";
        String glskFileName = "glsk_with_virtual_hubs.xml";

        Network network = Importers.loadNetwork(networkFileName, getClass().getResourceAsStream("/" + networkFileName));
        ZonalData<LinearGlsk> glskZonalData = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/" + glskFileName)).getZonalGlsks(network, java.time.Instant.parse("2016-07-28T22:30:00Z"));

        Crac crac = CracFactory.findDefault().create("cracId");

        crac.newContingency()
            .withId("internalBranch")
            .withNetworkElement("DDE1AA1  DDE3AA1  1")
            .add();

        crac.newContingency()
            .withId("danglingLine")
            .withNetworkElement("FFR1AA1  XLI_OB1B 1")
            .add();

        FlowCnec cnec1 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.PREVENTIVE)
            .newThreshold()
                .withRule(BranchThresholdRule.ON_RIGHT_SIDE)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        FlowCnec cnec2 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.OUTAGE)
            .withContingency("internalBranch")
                .newThreshold()
                .withRule(BranchThresholdRule.ON_RIGHT_SIDE)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        FlowCnec cnec3 = crac.newFlowCnec()
            .withId("cnec1")
            .withNetworkElement("anyNetworkElement")
            .withInstant(Instant.OUTAGE)
            .withContingency("danglingLine")
            .newThreshold()
                .withRule(BranchThresholdRule.ON_RIGHT_SIDE)
                .withUnit(Unit.MEGAWATT)
                .withMax(1000.0)
                .add()
            .add();

        XnodeGlskHandler xnodeGlskHandler = new XnodeGlskHandler(glskZonalData, crac.getBranchCnecs(), network);

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("10YNL----------L")));

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("22Y201903144---9")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("22Y201903144---9")));
        assertFalse(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("22Y201903144---9")));
    }

}
