/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_cne;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CneGenerationTest {

    @Test
    public void testExport1() {

        Crac crac = CracImporters.importCrac("US2-3-crac1-standard.json", getClass().getResourceAsStream("/US2-3-crac1-standard.json"));
        Network network = Importers.loadNetwork("US2-3-case1-standard.uct", getClass().getResourceAsStream("/US2-3-case1-standard.uct"));

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(3, point.getConstraintSeries().size());
    }

    @Test
    public void testExport2() {

        Crac crac = CracImporters.importCrac("US3-2-pst-direct.json", getClass().getResourceAsStream("/US3-2-pst-direct.json"));
        Network network = NetworkImportsUtil.import12NodesNetwork();

        // build object
        Cne cne = new Cne(crac, network);
        cne.generate();
        CriticalNetworkElementMarketDocument marketDocument = cne.getMarketDocument();
        Point point = marketDocument.getTimeSeries().get(0).getPeriod().get(0).getPoint().get(0);

        assertEquals(6, point.getConstraintSeries().size());
    }
}
