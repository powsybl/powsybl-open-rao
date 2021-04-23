/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.BranchIntensityPerPSTAngle;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class RangeActionSensitivityProviderTest {

    @Test
    public void contingenciesCracPstWithRange() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        NetworkImportsUtil.addHvdcLine(network);

        network.getSubstation("BBE1AA").newVoltageLevel().setId("BBE1AA2").setNominalV(225).setTopologyKind(TopologyKind.NODE_BREAKER).add();
        network.getVoltageLevel("BBE1AA2").getNodeBreakerView().newBusbarSection().setId("BB1").setNode(1).add();

        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        crac.newContingency()
            .withId("contingency-generator")
            .withNetworkElement("BBE1AA1 _generator")
            .add();

        crac.newContingency()
            .withId("contingency-hvdc")
            .withNetworkElement("HVDC1")
            .add();

        crac.newContingency()
            .withId("contingency-busbar-section")
            .withNetworkElement("BB1")
            .add();

        crac.newFlowCnec()
            .withId("generatorContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-10.)
                .withMax(10.)
                .add()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-generator")
            .add();

        crac.newFlowCnec()
            .withId("hvdcContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-10.)
                .withMax(10.)
                .add()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-hvdc")
            .add();

        crac.newFlowCnec()
            .withId("busbarContingencyCnec")
            .withNetworkElement("BBE2AA1  FFR3AA1  1")
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-10.)
                .withMax(10.)
                .add()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-busbar-section")
            .add();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(), crac.getBranchCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        List<Contingency> contingencyList = provider.getContingencies(network);
        assertEquals(5, contingencyList.size());
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR3")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("Contingency FR1 FR2")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-generator")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-hvdc")));
        assertTrue(contingencyList.stream().anyMatch(contingency -> contingency.getId().equals("contingency-busbar-section")));
    }

    @Test(expected = FaraoException.class)
    public void testFailureOnContingency() {
        Network network = NetworkImportsUtil.import12NodesNetwork();
        Crac crac = CommonCracCreation.createWithPreventivePstRange();

        crac.newContingency()
            .withId("contingency-fail")
            .withNetworkElement("FFR3AA1")
            .add();

        crac.newFlowCnec()
            .withId("failureCnec")
            .withNetworkElement("BBE1AA1  BBE3AA1  1")
            .newThreshold()
                .withUnit(Unit.AMPERE)
                .withRule(BranchThresholdRule.ON_LEFT_SIDE)
                .withMin(-10.)
                .withMax(10.)
                .add()
            .withInstant(Instant.CURATIVE)
            .withContingency("contingency-fail")
            .add();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(new HashSet<>(),
                Set.of(crac.getBranchCnec("failureCnec")), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));
        provider.getContingencies(network);
    }

    @Test
    public void factorsCracPstWithRange() {
        Crac crac = CommonCracCreation.createWithPreventivePstRange();
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(),
                crac.getBranchCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC (2 network elements) and 1 range action
        List<SensitivityFactor> factorList = provider.getAdditionalFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerPSTAngle).count());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerPSTAngle).count());
    }

    @Test
    public void cracWithoutRangeActionButWithPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(),
                crac.getBranchCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getAdditionalFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerPSTAngle).count());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerPSTAngle).count());
    }

    @Test (expected = FaraoException.class)
    public void cracWithoutRangeActionNorPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNoPstNetwork();

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider(crac.getRangeActions(),
                crac.getBranchCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getAdditionalFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerInjectionIncrease).count());
        //assertEquals(6, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerInjectionIncrease).count());
    }
}
