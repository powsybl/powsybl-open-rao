/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.NetworkElement;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class RangeActionSensitivityProviderTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";
    private static final String CURATIVE_INSTANT_ID = "curative";

    @Test
    void contingenciesCracPstWithRange() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addHvdcLine(network);

        network.getSubstation("BBE1AA").newVoltageLevel().setId("BBE1AA2").setNominalV(225).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        network.getVoltageLevel("BBE1AA2").getNodeBreakerView().newBusbarSection().setId("BB1").setNode(1).add();

        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        crac.newContingency()
            .withId("contingency-generator")
            .withContingencyElement("BBE1AA1 _generator", ContingencyElementType.GENERATOR)
            .add();

        crac.newContingency()
            .withId("contingency-hvdc")
            .withContingencyElement("HVDC1", ContingencyElementType.HVDC_LINE)
            .add();

        crac.newContingency()
            .withId("contingency-busbar-section")
            .withContingencyElement("BB1", ContingencyElementType.BUSBAR_SECTION)
            .add();

        crac.newFlowCnec()
            .withId("generatorContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(TwoSides.ONE)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-generator")
            .add();

        crac.newFlowCnec()
            .withId("hvdcContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(TwoSides.ONE)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-hvdc")
            .add();

        crac.newFlowCnec()
            .withId("busbarContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
            .withUnit(Unit.AMPERE)
            .withSide(TwoSides.ONE)
            .withMin(-10.)
            .withMax(10.)
            .add()
            .withNominalVoltage(380.)
            .withInstant(CURATIVE_INSTANT_ID)
            .withContingency("contingency-busbar-section")
            .add();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        List<Contingency> contingencyList = provider.getContingencies(network);
        assertEquals(5, contingencyList.size());
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR3")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR2")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-generator")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-hvdc")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-busbar-section")));
    }

    @Test
    void testDisableFactorForBaseCase() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // factors with basecase and contingency
        assertEquals(4, provider.getBasecaseFactors(network).size());
        assertEquals(4, provider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());

        provider.disableFactorsForBaseCaseSituation();

        // factors after disabling basecase
        assertEquals(0, provider.getBasecaseFactors(network).size());
        assertEquals(4, provider.getContingencyFactors(network, List.of(new Contingency("Contingency FR1 FR3", new ArrayList<>()))).size());

        provider.enableFactorsForBaseCaseSituation();
        // factors are enabled back
        assertEquals(4, provider.getBasecaseFactors(network).size());

    }

    @Test
    void factorsCracPstWithRange() {
        Crac crac = CommonCracCreation.createWithPreventivePstRange(Set.of(TwoSides.ONE, TwoSides.TWO));
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC (2 network elements) and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(8, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
    }

    @Test
    void cracWithoutRangeActionButWithPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(),
            crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
    }

    @Test
    void cracWithoutRangeActionNorPst() {
        Crac crac = CommonCracCreation.create(Set.of(TwoSides.ONE, TwoSides.TWO));
        Network network = NetworkImportsUtil.import12NodesNoPstNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(8, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.INJECTION_ACTIVE_POWER).count());
    }

    @Test
    void testHvdcSensi() {
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.TWO).add()
            .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        NetworkElement hvdc = Mockito.mock(NetworkElement.class);
        Mockito.when(hvdc.getId()).thenReturn("BBE2AA11 FFR3AA11 1");
        HvdcRangeAction mockHvdcRangeAction = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElement()).thenReturn(hvdc);

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(mockHvdcRangeAction), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);

        assertEquals(4, factorList.size());

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_2
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_1
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertTrue(factorList.stream().anyMatch(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_CURRENT_2
                && factor.getVariableType() == SensitivityVariableType.HVDC_LINE_ACTIVE_POWER
        ));

        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(0).getVariableId());
        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(1).getVariableId());
        assertEquals("BBE2AA11 FFR3AA11 1", factorList.get(2).getVariableId());
    }

    @Test
    void testUnhandledElement() {
        Crac crac = CracFactory.findDefault().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        NetworkElement line = Mockito.mock(NetworkElement.class);
        Mockito.when(line.getId()).thenReturn("BBE1AA11 BBE2AA11 1");
        RangeAction<?> mockHvdcRangeAction = Mockito.mock(RangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElements()).thenReturn(Set.of(line));

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(mockHvdcRangeAction), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> provider.getBasecaseFactors(network));
        assertEquals("Range action type of null not implemented yet", exception.getMessage());
    }

    @Test
    void testCTDoesNotThrow() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.newFlowCnec()
            .withId("cnec")
            .withNetworkElement("BBE1AA11 FFR5AA11 1")
            .withInstant(PREVENTIVE_INSTANT_ID)
            .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
            .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        RangeAction<?> ctRa = Mockito.mock(CounterTradeRangeAction.class);
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(ctRa), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        List<SensitivityFactor> factors = provider.getBasecaseFactors(network);
        assertEquals(2, factors.size());
        for (SensitivityFactor factor : factors) {
            // By default if no RA, then use a transformer for computing at least flows.
            assertEquals(SensitivityVariableType.TRANSFORMER_PHASE, factor.getVariableType());
        }

    }
}
