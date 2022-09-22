/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.sensitivity_analysis;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.CracFactory;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_impl.utils.CommonCracCreation;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.contingency.BranchContingency;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadflowProviderTest {

    @Test
    public void inAmpereAndMegawatt() {
        Crac crac = CommonCracCreation.create();
        Network network = NetworkImportsUtil.import12NodesNetwork();
        LoadflowProvider provider = new LoadflowProvider(crac.getFlowCnecs(), Stream.of(Unit.MEGAWATT, Unit.AMPERE).collect(Collectors.toSet()));

        // Common Crac contains 6 CNEC (2 network element) and 1 range action
        List<SensitivityFactor> factorList = provider.getBasecaseFactors(network);
        assertEquals(6, factorList.size());
        assertEquals(2, factorList.stream().filter(factor ->
            factor.getFunctionType() == SensitivityFunctionType.BRANCH_ACTIVE_POWER_1
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

        LoadflowProvider provider = new LoadflowProvider(Set.of(crac.getFlowCnec("cnec1basecase")), Stream.of(Unit.AMPERE).collect(Collectors.toSet()));

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
    public void filterCnecsMonitoredOnTheirOwnOutage() {
        // Do not generate factor on a FlowCnec for a contingency on its own network element
        Crac crac = CracFactory.findDefault().create("crac");
        String contingencyId = "contingency";
        crac.newContingency().withId(contingencyId).withNetworkElement("BBE2AA1  FFR3AA1  1").withNetworkElement("FFR1AA1  FFR3AA1  1").add();
        FlowCnec flowCnec = crac.newFlowCnec().withId("cnec").withNetworkElement("BBE2AA1  FFR3AA1  1").withContingency(contingencyId).withInstant(Instant.OUTAGE)
            .newThreshold().withUnit(Unit.MEGAWATT).withMax(1000.).withRule(BranchThresholdRule.ON_LEFT_SIDE).add()
            .add();

        Network network = NetworkImportsUtil.import12NodesNetwork();
        LoadflowProvider provider = new LoadflowProvider(Set.of(flowCnec), Collections.singleton(Unit.MEGAWATT));

        List<SensitivityFactor> factorList = provider.getContingencyFactors(network, List.of(new Contingency(contingencyId, List.of(new BranchContingency("BBE2AA1  FFR3AA1  1"), new BranchContingency("FFR1AA1  FFR3AA1  1")))));
        assertEquals(0, factorList.size());
    }
}
