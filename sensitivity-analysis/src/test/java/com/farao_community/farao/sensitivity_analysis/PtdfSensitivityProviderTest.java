/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.powsybl.contingency.Contingency;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class PtdfSensitivityProviderTest {

    Network network;
    Crac crac;
    ZonalData<SensitivityVariableSet> glskMock;

    @Before
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create();
        glskMock = glsk();
    }

    private static ZonalData<SensitivityVariableSet> glsk() {
        Map<String, SensitivityVariableSet> glsks = new HashMap<>();
        glsks.put("FR", new SensitivityVariableSet("10YFR-RTE------C", List.of(new WeightedSensitivityVariable("Generator FR", 1.f))));
        glsks.put("BE", new SensitivityVariableSet("10YBE----------2", List.of(new WeightedSensitivityVariable("Generator BE", 1.f))));
        glsks.put("DE", new SensitivityVariableSet("10YCB-GERMANY--8", List.of(new WeightedSensitivityVariable("Generator DE", 1.f))));
        glsks.put("NL", new SensitivityVariableSet("10YNL----------L", List.of(new WeightedSensitivityVariable("Generator NL", 1.f))));
        return new ZonalDataImpl<>(glsks);
    }

    @Test
    public void getFactorsOnCommonCrac() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));
        List<SensitivityFactor> sensitivityFactors = ptdfSensitivityProvider.getBasecaseFactors(network);
        assertEquals(8, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
                                                                          && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));

        sensitivityFactors = ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency(crac.getContingencies().iterator().next().getId(), new ArrayList<>())));
        assertEquals(8, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
            && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));
    }

    @Test
    public void testDisableFactorForBaseCase() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        // factors with basecase and contingency
        assertEquals(8, ptdfSensitivityProvider.getBasecaseFactors(network).size());
        assertEquals(8, ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());

        ptdfSensitivityProvider.disableFactorsForBaseCaseSituation();

        // factors after disabling basecase
        assertEquals(0, ptdfSensitivityProvider.getBasecaseFactors(network).size());
        assertEquals(8, ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());
    }

    @Test
    public void testDoNotHandleAmpere() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.AMPERE));
        assertFalse(ptdfSensitivityProvider.factorsInAmpere);
        assertTrue(ptdfSensitivityProvider.factorsInMegawatt);
    }
}
