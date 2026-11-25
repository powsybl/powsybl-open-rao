/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.sensitivityanalysis;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.*;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.HvdcRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.impl.utils.CommonCracCreation;
import com.powsybl.openrao.data.crac.impl.utils.NetworkImportsUtil;
import com.powsybl.sensitivity.*;
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

        HvdcRangeAction mockHvdcRangeAction = Mockito.mock(HvdcRangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElementId()).thenReturn("BBE2AA11 FFR3AA11 1");

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

        RangeAction<?> mockHvdcRangeAction = Mockito.mock(RangeAction.class);
        Mockito.when(mockHvdcRangeAction.getNetworkElementsIds()).thenReturn(Set.of("BBE1AA11 BBE2AA11 1"));

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(mockHvdcRangeAction), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        OpenRaoException exception = assertThrows(OpenRaoException.class, () -> provider.getBasecaseFactors(network));
        assertEquals("Range action type of null not implemented yet", exception.getMessage());
    }

    @Test
    void testCTWithoutGlskThrows() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("BBE1AA11 FFR5AA11 1")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        RangeAction<?> ctRa = Mockito.mock(CounterTradeRangeAction.class);
        Mockito.when(ctRa.getId()).thenReturn("ctRa");
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(ctRa), Set.of(flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        OpenRaoException e = assertThrows(OpenRaoException.class, () -> provider.getBasecaseFactors(network));
        assertEquals("Generator ordering of ctRa has not been computed", e.getMessage());
    }

    private record CtTestSetup(FlowCnec flowCnec, Network network, List<String> beGeneratorIds, Map<String, Double> weights) { }

    private CtTestSetup createSetup() {
        Crac crac = CommonCracCreation.create();
        FlowCnec flowCnec = crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("BBE1AA11 FFR5AA11 1")
                .withInstant(PREVENTIVE_INSTANT_ID)
                .newThreshold().withMax(1000.).withUnit(Unit.MEGAWATT).withSide(TwoSides.ONE).add()
                .add();

        Network network = Network.read("TestCase16NodesWithHvdc.xiidm", getClass().getResourceAsStream("/TestCase16NodesWithHvdc.xiidm"));

        List<String> beGeneratorIds = network.getGeneratorStream()
                .filter(g -> g.getTerminal().getVoltageLevel().getSubstation()
                        .flatMap(Substation::getCountry).orElse(null) == Country.BE)
                .map(Generator::getId)
                .sorted()
                .toList();
        List<Double> weightValues = List.of(0.1, 0.2, 0.3, 0.4);
        assertEquals(weightValues.size(), beGeneratorIds.size());

        Map<String, Double> weights = new HashMap<>();
        for (int i = 0; i < beGeneratorIds.size(); i++) {
            weights.put(beGeneratorIds.get(i), weightValues.get(i));
        }
        return new CtTestSetup(flowCnec, network, beGeneratorIds, weights);
    }

    @Test
    void testCTFactorsWithGlskNoSaturation() {
        CtTestSetup s = createSetup();
        List<Double> weightValues = List.of(0.1, 0.2, 0.3, 0.4);
        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
                s.weights(), s.network(), 1e-3, -100, 100);
        CounterTradeRangeAction ctRa = Mockito.mock(CounterTradeRangeAction.class);
        Mockito.when(ctRa.getId()).thenReturn("ctRa");
        Mockito.when(ctRa.getGeneratorOrdering()).thenReturn(ordering);

        assertEquals(0, ordering.upwardLastStage());
        assertEquals(0, ordering.downwardLastStage());
        assertEquals(4, ordering.upwardOrder().size());
        assertEquals(4, ordering.downwardOrder().size());

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(Set.of(ctRa), Set.of(s.flowCnec), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        List<SensitivityFactor> factors = provider.getBasecaseFactors(s.network);

        // 4a. factors: 4 functions (flow in MW and in A on side ONE) x 2 variables (up+down) = 4 factors
        assertEquals(4, factors.size());
        for (SensitivityFactor factor : factors) {
            assertEquals(SensitivityVariableType.INJECTION_ACTIVE_POWER, factor.getVariableType());
            assertTrue(factor.isVariableSet());
        }
        Set<String> variableIds = factors.stream().map(SensitivityFactor::getVariableId).collect(Collectors.toSet());
        assertEquals(2, variableIds.size());

        for (String variableSetId : variableIds) {
            SensitivityVariableSet variableSet = provider.getVariableSets().stream()
                    .filter(vs -> vs.getId().equals(variableSetId))
                    .findFirst().orElseThrow();
            Map<String, Double> varWeights = variableSet.getVariables().stream()
                    .collect(Collectors.toMap(WeightedSensitivityVariable::getId, WeightedSensitivityVariable::getWeight));
            assertEquals(4, varWeights.size());
            for (int i = 0; i < s.beGeneratorIds().size(); i++) {
                assertEquals(weightValues.get(i), varWeights.get(s.beGeneratorIds().get(i)), 1e-6);
            }
        }
    }

    @Test
    void testCTFactorsWithGlskUpwardSaturation() {
        CtTestSetup s = createSetup();
        String saturatingId = s.beGeneratorIds().get(3);   // generator 4, weight 0.4

        CounterTradeGeneratorOrdering ordering = GeneratorOrderingComputer.compute(
                s.weights(), s.network(), 1e-3, -100, 20000);

        // preconditions
        GlskGeneratorAllocation first = ordering.upwardOrder().get(0);
        assertEquals(saturatingId, first.generatorId());
        assertEquals(16250., first.cumulativeAllocation(), 1e-3);
        int k = ordering.upwardLastStage();
        assertTrue(k >= 1);
        assertEquals(0, ordering.downwardLastStage());

        CounterTradeRangeAction ctRa = Mockito.mock(CounterTradeRangeAction.class);
        Mockito.when(ctRa.getId()).thenReturn("ctRa");
        Mockito.when(ctRa.getGeneratorOrdering()).thenReturn(ordering);

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(
                Set.of(ctRa), Set.of(s.flowCnec()), Set.of(Unit.MEGAWATT, Unit.AMPERE));
        List<SensitivityFactor> factors = provider.getBasecaseFactors(s.network());

        // number of factors: functions x (k single generators + tail up + tail down)
        long functions = factors.stream().map(SensitivityFactor::getFunctionType).distinct().count();
        assertEquals(functions * (k + 2), factors.size());

        Map<String, List<SensitivityFactor>> byVariable = factors.stream()
                .collect(Collectors.groupingBy(SensitivityFactor::getVariableId));
        assertEquals(k + 2, byVariable.size());
        byVariable.values().forEach(list -> assertEquals(functions, list.size()));
        factors.forEach(f -> assertEquals(SensitivityVariableType.INJECTION_ACTIVE_POWER, f.getVariableType()));

        // single generators: generator id and not variableSets
        Set<String> headIds = ordering.upwardOrder().subList(0, k).stream()
                .map(GlskGeneratorAllocation::generatorId).collect(Collectors.toSet());
        assertTrue(headIds.contains(saturatingId));
        for (String id : headIds) {
            List<SensitivityFactor> list = byVariable.get(id);
            assertNotNull(list, "Non registered generator: " + id);
            list.forEach(f -> assertFalse(f.isVariableSet()));
        }

        // tails: variable sets with the expected generators
        Set<String> upTailIds = ordering.upwardOrder().subList(k, ordering.upwardOrder().size()).stream()
                .map(GlskGeneratorAllocation::generatorId).collect(Collectors.toSet());
        Set<String> downTailIds = ordering.downwardOrder().stream()
                .map(GlskGeneratorAllocation::generatorId).collect(Collectors.toSet());
        assertFalse(upTailIds.contains(saturatingId));
        assertEquals(s.beGeneratorIds().size() - k, upTailIds.size());
        assertEquals(s.beGeneratorIds().size(), downTailIds.size());

        assertTailSet(provider, byVariable, upTailIds, s.weights());
        assertTailSet(provider, byVariable, downTailIds, s.weights());
    }

    @Test
    void testLastStageAroundSaturationPoint() {
        CtTestSetup s = createSetup();
        assertEquals(0, GeneratorOrderingComputer.compute(s.weights(), s.network(), 1e-3, -100, 16249).upwardLastStage());
        assertTrue(GeneratorOrderingComputer.compute(s.weights(), s.network(), 1e-3, -100, 16251).upwardLastStage() >= 1);
    }

    private void assertTailSet(RangeActionSensitivityProvider provider,
                               Map<String, List<SensitivityFactor>> byVariable,
                               Set<String> expectedIds, Map<String, Double> rawWeights) {
        SensitivityVariableSet set = provider.getVariableSets().stream()
                .filter(vs -> vs.getVariables().stream()
                        .map(WeightedSensitivityVariable::getId).collect(Collectors.toSet()).equals(expectedIds))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No set with generators " + expectedIds));

        byVariable.get(set.getId()).forEach(f -> assertTrue(f.isVariableSet()));

        // weights are proportional to the initial ones, with or without tail normalization
        double setTotal = set.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        double rawTotal = expectedIds.stream().mapToDouble(rawWeights::get).sum();
        for (WeightedSensitivityVariable v : set.getVariables()) {
            assertEquals(rawWeights.get(v.getId()) / rawTotal, v.getWeight() / setTotal, 1e-6);
        }
    }
}
