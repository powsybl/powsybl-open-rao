/*
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.flow_decomposition.full_line_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.farao_community.farao.data.crac_file.CracFile;
import com.farao_community.farao.data.crac_file.json.JsonCracFile;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PtdfSensitivityConverterTest {
    private Network testNetwork;
    private CracFile testCracFile;

    @Before
    public void setUp() throws IOException {
        testNetwork = Importers.loadNetwork("testCase.xiidm", NetworkUtilTest.class.getResourceAsStream("/testCase.xiidm"));
        testCracFile = JsonCracFile.read(NetworkUtilTest.class.getResourceAsStream("/simpleInputs.json"));
    }

    @Test
    public void getFactors() {
        PtdfSensitivityConverter ptdfSensitivityConverter = new PtdfSensitivityConverter(testCracFile);
        List<SensitivityFactor> factors = ptdfSensitivityConverter.getFactors(testNetwork);

        long numberOfInjections = NetworkUtil.getInjectionStream(testNetwork).count();
        long numberOfMonitoredBranches = testCracFile.getPreContingency().getMonitoredBranches().size();
        assertEquals(numberOfInjections * numberOfMonitoredBranches, factors.size());
    }
}
