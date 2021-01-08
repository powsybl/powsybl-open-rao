/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.rao_commons;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.RaoParameters;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.util.HashSet;

import static junit.framework.TestCase.assertNotNull;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class InitialSensitivityAnalysisTest {

    @Test
    public void testConstructor() {

        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        RaoData raoData = new RaoData(network, crac, crac.getPreventiveState(), crac.getStates(), null, null, new HashSet<>(), false);
        RaoParameters raoParameters = new RaoParameters();

        InitialSensitivityAnalysis initialSensitivityAnalysis = new InitialSensitivityAnalysis(raoData, raoParameters);
        assertNotNull(initialSensitivityAnalysis);
    }
}
