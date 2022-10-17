/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
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

    @Test
    public void filterDisconnectedFlowCnecs() {
        // Do not generate factor on a FlowCnec that is disconnected in the network
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String contingencyId = "Contingency FR1 FR3";

        PtdfSensitivityProvider provider = new PtdfSensitivityProvider(glskMock, Set.of(crac.getFlowCnec("cnec1basecase"), crac.getFlowCnec("cnec1stateCurativeContingency1")), Set.of(Unit.MEGAWATT));

        // Line is still connected
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(4, factorList.size());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertEquals(4, factorList.size());
        assertEquals(1, provider.getContingencies(network).size());

        // Disconnect Terminal1
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().disconnect();
        factorList = provider.getBasecaseFactors(network);
        assertTrue(factorList.isEmpty());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertTrue(factorList.isEmpty());
        assertTrue(provider.getContingencies(network).isEmpty());

        // Reconnect Terminal1 and disconnect Terminal2
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal1().connect();
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal2().disconnect();
        factorList = provider.getBasecaseFactors(network);
        assertTrue(factorList.isEmpty());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertTrue(factorList.isEmpty());
        assertTrue(provider.getContingencies(network).isEmpty());
    }

    @Test
    public void filterDisconnectedFlowCnecOnDanglingLine() {
        // Do not generate factor on a FlowCnec that is disconnected in the network
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addDanglingLine(network);
        String contingencyId = "Contingency FR1 FR3";

        crac.newFlowCnec().withId("cnecOnDlBasecase").withInstant(Instant.PREVENTIVE).withNetworkElement("DL1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();
        crac.newFlowCnec().withId("cnecOnDlCurative").withInstant(Instant.CURATIVE).withContingency(contingencyId).withNetworkElement("DL1")
            .newThreshold().withSide(Side.LEFT).withUnit(Unit.MEGAWATT).withMax(1000.).add()
            .add();

        PtdfSensitivityProvider provider = new PtdfSensitivityProvider(glskMock, Set.of(crac.getFlowCnec("cnecOnDlBasecase"), crac.getFlowCnec("cnecOnDlCurative")), Set.of(Unit.MEGAWATT));

        // Line is still connected
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(4, factorList.size());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertEquals(4, factorList.size());
        assertEquals(1, provider.getContingencies(network).size());

        // Disconnect dangling line
        network.getDanglingLine("DL1").getTerminal().disconnect();
        factorList = provider.getBasecaseFactors(network);
        assertTrue(factorList.isEmpty());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertTrue(factorList.isEmpty());
        assertTrue(provider.getContingencies(network).isEmpty());
    }
}
