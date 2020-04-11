/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.linear_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
public class SystematicAnalysisEngineTest {
    private static final double FLOW_TOLERANCE = 1.;
    InitialSituation initialSituation;

    @Before
    public void setUp() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        crac.synchronize(network);
        initialSituation = new InitialSituation(network, network.getVariantManager().getWorkingVariantId(), crac);
    }

    @Test
    public void testRun() {
        assertNotNull(1);
    }
}
