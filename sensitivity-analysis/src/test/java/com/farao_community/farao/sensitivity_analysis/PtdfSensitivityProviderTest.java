/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.glsk.import_.glsk_provider.GlskProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class PtdfSensitivityProviderTest {

    @Test
    public void getFactorsOnCommonCrac() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.create();
        GlskProvider glskProviderMock = glskProvider();

        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskProviderMock, crac.getCnecs(), Collections.singleton(Unit.MEGAWATT));

        List<SensitivityFactor> sensitivityFactors = ptdfSensitivityProvider.getFactors(network);

        assertEquals(8, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunction().getId().contains("FFR2AA1  DDE3AA1  1")
                                                                          && sensitivityFactor.getVariable().getId().contains("10YCB-GERMANY--8")));
    }

    private static GlskProvider glskProvider() {
        Map<String, LinearGlsk> glsks = new HashMap<>();
        glsks.put("FR", new LinearGlsk("10YFR-RTE------C", "FR", Collections.singletonMap("Generator FR", 1.f)));
        glsks.put("BE", new LinearGlsk("10YBE----------2", "BE", Collections.singletonMap("Generator BE", 1.f)));
        glsks.put("DE", new LinearGlsk("10YCB-GERMANY--8", "DE", Collections.singletonMap("Generator DE", 1.f)));
        glsks.put("NL", new LinearGlsk("10YNL----------L", "NL", Collections.singletonMap("Generator NL", 1.f)));
        return new GlskProvider() {
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
