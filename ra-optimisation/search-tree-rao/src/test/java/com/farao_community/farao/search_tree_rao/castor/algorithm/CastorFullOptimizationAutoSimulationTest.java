/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.castor.algorithm;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_api.threshold.BranchThresholdRule;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.rao_result_api.ComputationStatus;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.rao_api.RaoInput;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.commons.SensitivityComputer;
import com.farao_community.farao.search_tree_rao.result.api.*;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.Leaf;
import com.farao_community.farao.search_tree_rao.search_tree.algorithms.SearchTree;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SensitivityComputer.class, SearchTree.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class CastorFullOptimizationAutoSimulationTest {
    private Crac crac;
    private Network network;
    private State state1;
    private RangeAction<?> ra2;
    private RangeAction<?> ra3;

    private CastorFullOptimization castorFullOptimization;
    private RaoInput inputs;
    private RaoParameters raoParameters;
    private java.time.Instant instant;

    @Before
    public void setup() {
        Network network = Importers.loadNetwork("network_with_alegro_hub.xiidm", getClass().getResourceAsStream("/network/network_with_alegro_hub.xiidm"));
        crac = CracImporters.importCrac("crac/small-crac-auto.json", getClass().getResourceAsStream("/crac/small-crac-auto.json"));
        inputs = Mockito.mock(RaoInput.class);
        when(inputs.getNetwork()).thenReturn(network);
        when(inputs.getNetworkVariantId()).thenReturn(network.getVariantManager().getWorkingVariantId());
        when(inputs.getCrac()).thenReturn(crac);
        raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        instant = Mockito.mock(java.time.Instant.class);
        castorFullOptimization = new CastorFullOptimization(inputs, raoParameters, instant);
    }

    private void prepareMocks() {
        SensitivityComputer.SensitivityComputerBuilder sensitivityComputerBuilder = Mockito.mock(SensitivityComputer.SensitivityComputerBuilder.class);
        when(sensitivityComputerBuilder.withToolProvider(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCnecs(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withRangeActions(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withPtdfsResults(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withPtdfsResults(Mockito.any(), Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCommercialFlowsResults(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withCommercialFlowsResults(Mockito.any(), Mockito.any())).thenReturn(sensitivityComputerBuilder);
        when(sensitivityComputerBuilder.withAppliedRemedialActions(Mockito.any())).thenReturn(sensitivityComputerBuilder);
        SensitivityComputer sensitivityComputer = Mockito.mock(SensitivityComputer.class);
        when(sensitivityComputerBuilder.build()).thenReturn(sensitivityComputer);

        SensitivityResult sensitivityResult = Mockito.mock(SensitivityResult.class);
        when(sensitivityResult.getSensitivityStatus()).thenReturn(ComputationStatus.DEFAULT);
        Mockito.when(sensitivityComputer.getSensitivityResult()).thenReturn(sensitivityResult);
        Mockito.when(sensitivityComputer.getBranchResult()).thenReturn(Mockito.mock(FlowResult.class));

        Leaf leaf = Mockito.mock(Leaf.class);
        when(leaf.getStatus()).thenReturn(Leaf.Status.EVALUATED);

        try {
            PowerMockito.whenNew(SensitivityComputer.SensitivityComputerBuilder.class).withNoArguments().thenReturn(sensitivityComputerBuilder);
            PowerMockito.whenNew(Leaf.class).withArguments(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()).thenReturn(leaf);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void run() throws ExecutionException, InterruptedException {
        prepareMocks();
        RaoResult raoResult = castorFullOptimization.run().get();
        assertNotNull(raoResult);
    }

    private void setUpCracWithAutoRAs() {
        crac = CracFactory.findDefault().create("test-crac");
        Contingency contingency1 = crac.newContingency()
                .withId("contingency1")
                .withNetworkElement("contingency1-ne")
                .add();
        crac.newFlowCnec()
                .withId("cnec")
                .withNetworkElement("cnec-ne")
                .withContingency("contingency1")
                .withInstant(Instant.AUTO)
                .withNominalVoltage(220.)
                .newThreshold().withRule(BranchThresholdRule.ON_RIGHT_SIDE).withMax(1000.).withUnit(Unit.AMPERE).add()
                .add();
        state1 = crac.getState(contingency1, Instant.AUTO);
        // ra2 : auto
        ra2 = crac.newPstRangeAction()
                .withId("ra2")
                .withNetworkElement("ra2-ne")
                .withSpeed(2)
                .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();
        ra3 = crac.newPstRangeAction()
                .withId("ra3")
                .withNetworkElement("ra2-ne")
                .withSpeed(3)
                .newFreeToUseUsageRule().withInstant(Instant.AUTO).withUsageMethod(UsageMethod.FORCED).add()
                .newOnFlowConstraintUsageRule().withInstant(Instant.AUTO).withFlowCnec("cnec").add()
                .withInitialTap(0).withTapToAngleConversionMap(Map.of(0, -100., 1, 100.))
                .add();

        state1 = crac.getState(contingency1, Instant.AUTO);
    }

    @Test
    public void testGatherCnecs() {
        setUpCracWithAutoRAs();
        assertEquals(1, CastorFullOptimization.gatherFlowCnecs(ra2, state1, crac, network).size());
        assertEquals(1, CastorFullOptimization.gatherFlowCnecs(ra3, state1, crac, network).size());
    }

}
