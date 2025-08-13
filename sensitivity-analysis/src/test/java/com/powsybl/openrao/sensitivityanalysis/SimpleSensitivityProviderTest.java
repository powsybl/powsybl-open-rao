/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.impl.CracImplFactory;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import org.junit.jupiter.api.Test;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class SimpleSensitivityProviderTest {
    @Test
    void testIsConnected() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        Crac crac = new CracImplFactory().create("cracId")
            .newInstant("preventive", InstantKind.PREVENTIVE);

        // Branch
        FlowCnec cnec1 = crac.newFlowCnec().withId("cnec-1-id").withNetworkElement("BBE1AA1  BBE2AA1  1").withInstant("preventive").newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(AbstractSimpleSensitivityProvider.isConnected(cnec1, network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().disconnect();
        assertFalse(AbstractSimpleSensitivityProvider.isConnected(cnec1, network));

        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal1().connect();
        network.getBranch("BBE1AA1  BBE2AA1  1").getTerminal2().disconnect();
        assertFalse(AbstractSimpleSensitivityProvider.isConnected(cnec1, network));

        // DanglingLine
        FlowCnec cnec2 = crac.newFlowCnec().withId("cnec-2-id").withNetworkElement("DL1").withInstant("preventive").newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(AbstractSimpleSensitivityProvider.isConnected(cnec2, network));

        network.getDanglingLine("DL1").getTerminal().disconnect();
        assertFalse(AbstractSimpleSensitivityProvider.isConnected(cnec2, network));

        // Generator
        FlowCnec cnec3 = crac.newFlowCnec().withId("cnec-3-id").withNetworkElement("BBE2AA1 _generator").withInstant("preventive").newThreshold().withUnit(MEGAWATT).withMax(1000.).withSide(ONE).add().add();
        assertTrue(AbstractSimpleSensitivityProvider.isConnected(cnec3, network));

        network.getGenerator("BBE2AA1 _generator").getTerminal().disconnect();
        assertFalse(AbstractSimpleSensitivityProvider.isConnected(cnec3, network));
    }
}
