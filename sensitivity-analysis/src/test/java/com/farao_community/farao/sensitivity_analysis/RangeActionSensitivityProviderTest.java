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
import com.farao_community.farao.data.crac_impl.ComplexContingency;
import com.farao_community.farao.data.crac_impl.SimpleCnec;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.threshold.AbsoluteFlowThreshold;
import com.farao_community.farao.data.crac_impl.threshold.AbstractThreshold;
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
import java.util.Optional;
import java.util.Set;

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

        Crac crac = CommonCracCreation.createWithPstRange();
        ComplexContingency generatorContingency = new ComplexContingency("contingency-generator");
        generatorContingency.addNetworkElement(new NetworkElement("BBE1AA1 _generator"));
        ComplexContingency hvdcContingency = new ComplexContingency("contingency-hvdc");
        hvdcContingency.addNetworkElement(new NetworkElement("HVDC1"));
        ComplexContingency busbarSectionContingency = new ComplexContingency("contingency-busbar-section");
        hvdcContingency.addNetworkElement(new NetworkElement("BB1"));
        crac.addContingency(generatorContingency);
        crac.addContingency(hvdcContingency);
        crac.addContingency(busbarSectionContingency);

        NetworkElement networkElement = crac.getCnecs().iterator().next().getNetworkElement();
        Instant instant = crac.getInstant("curative");
        Set<AbstractThreshold> thresholdSet = new HashSet<>();
        thresholdSet.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.BOTH, 10));

        crac.addCnec(new SimpleCnec("generatorContingencyCnec",
            networkElement,
            thresholdSet,
            new SimpleState(Optional.of(generatorContingency), instant)));
        crac.addCnec(new SimpleCnec("hvdcContingencyCnec",
            networkElement,
            thresholdSet,
            new SimpleState(Optional.of(hvdcContingency), instant)));
        crac.addCnec(new SimpleCnec("busbarContingencyCnec",
            networkElement,
            thresholdSet,
            new SimpleState(Optional.of(busbarSectionContingency), instant)));

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider();
        provider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Common Crac contains 6 CNEC and 1 range action
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
        Crac crac = CommonCracCreation.createWithPstRange();

        ComplexContingency busBreakerContingency = new ComplexContingency("contingency-fail");
        busBreakerContingency.addNetworkElement(new NetworkElement("FFR3AA1"));
        crac.addContingency(busBreakerContingency);

        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider();

        Set<AbstractThreshold> thresholdSet = new HashSet<>();
        thresholdSet.add(new AbsoluteFlowThreshold(Unit.AMPERE, Side.LEFT, Direction.BOTH, 10));
        Instant instant = crac.getInstant("curative");
        Cnec cnec = new SimpleCnec("failureCnec",
            crac.getNetworkElements().iterator().next(),
            thresholdSet,
            new SimpleState(Optional.of(busBreakerContingency), instant));
        Set<Cnec> cnecs = new HashSet<>();
        cnecs.add(cnec);

        provider.addSensitivityFactors(new HashSet<>(), cnecs);

        List<Contingency> contingencyList = provider.getContingencies(network);
    }

    @Test
    public void factorsCracPstWithRange() {
        Crac crac = CommonCracCreation.createWithPstRange();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider();
        provider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerPSTAngle).count());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerPSTAngle).count());
    }

    @Test
    public void cracWithoutRangeActionButWithPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider();
        provider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerPSTAngle).count());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerPSTAngle).count());
    }

    @Test (expected = FaraoException.class)
    public void cracWithoutRangeActionNorPst() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNoPstNetwork();
        RangeActionSensitivityProvider provider = new RangeActionSensitivityProvider();
        provider.addSensitivityFactors(crac.getRangeActions(), crac.getCnecs());

        // Common Crac contains 6 CNEC and 1 range action
        List<SensitivityFactor> factorList = provider.getFactors(network);
        assertEquals(4, factorList.size());
        assertEquals(2, factorList.stream().filter(factor -> factor instanceof BranchFlowPerInjectionIncrease).count());
        //assertEquals(6, factorList.stream().filter(factor -> factor instanceof BranchIntensityPerInjectionIncrease).count());
    }
}
