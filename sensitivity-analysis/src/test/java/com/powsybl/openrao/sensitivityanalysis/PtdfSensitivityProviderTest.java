/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
class PtdfSensitivityProviderTest {

    Network network;
    Crac crac;
    ZonalData<SensitivityVariableSet> glskMock;

    @BeforeEach
    public void setUp() {
        network = NetworkImportsUtil.import12NodesNetwork();
        crac = CommonCracCreation.create(Set.of(TwoSides.ONE, TwoSides.TWO));
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
    void getFactorsOnCommonCracInMegawatt() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));
        List<SensitivityFactor> sensitivityFactors = ptdfSensitivityProvider.getBasecaseFactors(network);
        assertEquals(16, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
                                                                          && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));
        assertTrue(sensitivityFactors.stream().allMatch(sensitivityFactor -> sensitivityFactor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1) || sensitivityFactor.getFunctionType().equals(SensitivityFunctionType.BRANCH_ACTIVE_POWER_2)));

        sensitivityFactors = ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency(crac.getContingencies().iterator().next().getId(), new ArrayList<>())));
        assertEquals(12, sensitivityFactors.size()); // 12 and not 16 because in contingency FR1 FR2, monitor just side one of network elemeent "FFR2AA1  DDE3AA1  1".
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
            && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));
    }

    @Test
    void getFactorsOnCommonCracInAmpere() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.AMPERE));
        List<SensitivityFactor> sensitivityFactors = ptdfSensitivityProvider.getBasecaseFactors(network);
        assertEquals(16, sensitivityFactors.size());
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
            && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));
        assertTrue(sensitivityFactors.stream().allMatch(sensitivityFactor -> sensitivityFactor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_1) || sensitivityFactor.getFunctionType().equals(SensitivityFunctionType.BRANCH_CURRENT_2)));

        sensitivityFactors = ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency(crac.getContingencies().iterator().next().getId(), new ArrayList<>())));
        assertEquals(12, sensitivityFactors.size()); // 12 and not 16 because in contingency FR1 FR2, monitor just side one of network elemeent "FFR2AA1  DDE3AA1  1".
        assertTrue(sensitivityFactors.stream().anyMatch(sensitivityFactor -> sensitivityFactor.getFunctionId().contains("FFR2AA1  DDE3AA1  1")
            && sensitivityFactor.getVariableId().contains("10YCB-GERMANY--8")));
    }

    @Test
    void testDisableFactorForBaseCase() {
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        // factors with basecase and contingency
        assertEquals(16, ptdfSensitivityProvider.getBasecaseFactors(network).size());
        assertEquals(16, ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());

        ptdfSensitivityProvider.disableFactorsForBaseCaseSituation();

        // factors after disabling basecase
        assertEquals(0, ptdfSensitivityProvider.getBasecaseFactors(network).size());
        assertEquals(16, ptdfSensitivityProvider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());
    }

    @Test
    void testDoHandleAmpere() {
        // Simple test to check that we can handle PDTF Sensitivity in both Ampere and MW.
        PtdfSensitivityProvider ptdfSensitivityProvider = new PtdfSensitivityProvider(glskMock, crac.getFlowCnecs(), Set.of(Unit.AMPERE, Unit.MEGAWATT));
        assertTrue(ptdfSensitivityProvider.factorsInAmpere);
        assertTrue(ptdfSensitivityProvider.factorsInMegawatt);
    }
}
