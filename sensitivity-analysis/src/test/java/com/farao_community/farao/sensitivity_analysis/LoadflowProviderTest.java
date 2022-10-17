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
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadflowProviderTest {

    @Test
    public void inAmpereAndMegawattOnOneSide() {
        Crac crac = CommonCracCreation.create(Set.of(Side.LEFT));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        LoadflowProvider provider = new LoadflowProvider(crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
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
    public void inAmpereAndMegawattOnTwoSides() {
        Crac crac = CommonCracCreation.create(Set.of(Side.LEFT, Side.RIGHT));
        Network network = NetworkImportsUtil.import12NodesNetwork();
        LoadflowProvider provider = new LoadflowProvider(crac.getFlowCnecs(), Set.of(Unit.MEGAWATT, Unit.AMPERE));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
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
    public void inAmpereOnTransformer() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        network.getBranch("BBE2AA1  FFR3AA1  1").getTerminal2().getVoltageLevel().setNominalV(200);

        LoadflowProvider provider = new LoadflowProvider(Set.of(crac.getFlowCnec("cnec1basecase")), Set.of(Unit.AMPERE));

        // When nominalV on side 1 & side 2 are not the same, only use side 1 factor
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(1, factorList.size());
        assertEquals(SensitivityFunctionType.BRANCH_CURRENT_1, factorList.get(0).getFunctionType());
        assertEquals(SensitivityVariableType.TRANSFORMER_PHASE, factorList.get(0).getVariableType());
    }

    @Test
    public void inMegawattOnly() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        LoadflowProvider provider = new LoadflowProvider(crac.getFlowCnecs(), Collections.singleton(Unit.MEGAWATT));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(2, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        String contingencyId = crac.getContingencies().iterator().next().getId();
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertEquals(2, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
                && factor.getVariableType() == SensitivityVariableType.TRANSFORMER_PHASE).count());
    }

    @Test
    public void filterDisconnectedFlowCnecs() {
        // Do not generate factor on a FlowCnec that is disconnected in the network
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        String contingencyId = "Contingency FR1 FR3";

        LoadflowProvider provider = new LoadflowProvider(Set.of(crac.getFlowCnec("cnec1basecase"), crac.getFlowCnec("cnec1stateCurativeContingency1")), Collections.singleton(Unit.MEGAWATT));

        // Line is still connected
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(1, factorList.size());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertEquals(1, factorList.size());
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

        LoadflowProvider provider = new LoadflowProvider(Set.of(crac.getFlowCnec("cnecOnDlBasecase"), crac.getFlowCnec("cnecOnDlCurative")), Collections.singleton(Unit.MEGAWATT));

        // Line is still connected
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(1, factorList.size());
        factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, new ArrayList<>())));
        assertEquals(1, factorList.size());
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
