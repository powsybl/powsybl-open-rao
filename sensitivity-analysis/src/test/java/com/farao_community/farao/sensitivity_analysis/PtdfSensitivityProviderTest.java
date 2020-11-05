/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.import_.glsk_provider.Glsk;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class PtdfSensitivityProviderTest {
    private static final double EPSILON = 1e-3;
    private Network network;
    private Crac crac;
    private Glsk glskMock;
    private PtdfSensitivityProvider ptdfSensitivityProvider;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();

        glskMock = glskProvider();
        ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock);
    }

    @Test
    public void testGetFactors() {
        ptdfSensitivityProvider.addCnecs(crac.getCnecs());
        List<SensitivityFactor> sensitivityFactors = ptdfSensitivityProvider.getFactors(network);

        assertEquals(8, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunction().getId().contains("FFR2AA1  DDE3AA1  1")
                                                                          && sensitivityFactor.getVariable().getId().contains("10YCB-GERMANY--8")));
    }

    static Glsk glskProvider() {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR", new LinearGlsk("10YFR-RTE------C", "FR", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("BE", new LinearGlsk("10YBE----------2", "BE", Collections.singletonMap("Generator BE", 1.f)));
        glsks.put("DE", new LinearGlsk("10YCB-GERMANY--8", "DE", Collections.singletonMap("Generator DE", 1.f)));
        glsks.put("NL", new LinearGlsk("10YNL----------L", "NL", Collections.singletonMap("Generator NL", 1.f)));
        return new Glsk() {
            @Override
            public Map<String, LinearGlsk> getAllGlsk(Network network) {
                return glsks;
            }

            @Override
            public LinearGlsk getGlsk(Network network, String area) {
                return glsks.get(area);
            }
        };
    }
}
