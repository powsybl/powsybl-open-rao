/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.loopflow_computation;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.BranchCnec;
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.cnec.FlowCnecImpl;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;

import java.util.*;

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

        Set<NetworkElement> internalBranch = Collections.singleton(new NetworkElement("DDE1AA1  DDE3AA1  1"));
        Set<NetworkElement> danglingLine = Collections.singleton(new NetworkElement("FFR1AA1  XLI_OB1B 1"));

        State baseCase = new SimpleState(Optional.empty(), new Instant("preventive", 0));
        State contingencyClassic = new SimpleState(Optional.of(new ComplexContingency("internalBranch", internalBranch)), new Instant("curative", 60));
        State contingencyDl = new SimpleState(Optional.of(new ComplexContingency("danglingLine", danglingLine)), new Instant("curative", 60));

        BranchCnec cnec1 = new FlowCnecImpl("cnec1", new NetworkElement("ne"), baseCase, true, true, new HashSet<>(), 0.0);
        BranchCnec cnec2 = new FlowCnecImpl("cnec2", new NetworkElement("ne"), contingencyClassic, true, true, new HashSet<>(), 0.0);
        BranchCnec cnec3 = new FlowCnecImpl("cnec3", new NetworkElement("ne"), contingencyDl, true, true, new HashSet<>(), 0.0);

        Set<BranchCnec> cnecs = new HashSet<>(Arrays.asList(cnec1, cnec2, cnec3));

        XnodeGlskHandler xnodeGlskHandler = new XnodeGlskHandler(glskZonalData, cnecs, network);

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("10YNL----------L")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("10YNL----------L")));

        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec1, glskZonalData.getData("22Y201903144---9")));
        assertTrue(xnodeGlskHandler.isLinearGlskValidForCnec(cnec2, glskZonalData.getData("22Y201903144---9")));
        assertFalse(xnodeGlskHandler.isLinearGlskValidForCnec(cnec3, glskZonalData.getData("22Y201903144---9")));
    }

}
