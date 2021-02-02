/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_io_api.CracImporters;
import com.farao_community.farao.data.crac_result_extensions.CracResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_commons.CracResultManager;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.RaoUtil;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityInterface;
import com.farao_community.farao.sensitivity_analysis.SystematicSensitivityResult;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.farao_community.farao.util.NativeLibraryLoader;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NativeLibraryLoader.class, SearchTreeRaoLogger.class, SystematicSensitivityInterface.class, Leaf.class, SearchTree.class, RaoUtil.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class SearchTreeTest {

    private SearchTree searchTree;
    private RaoParameters raoParameters;
    private SystematicSensitivityInterface systematicSensitivityInterface;
    private IteratingLinearOptimizer iteratingLinearOptimizer;
    private Network network;
    private RaoData raoData;

    @Before
    public void setUp() {
        searchTree = new SearchTree();
        network = NetworkImportsUtil.import12NodesNetwork();
        String variantId = network.getVariantManager().getWorkingVariantId();
        RaoUtil.initNetwork(network, variantId);
        Crac crac = CracImporters.importCrac("small-crac-with-network-actions.json", getClass().getResourceAsStream("/small-crac-with-network-actions.json"));
        RaoUtil.initCrac(crac, network);

        raoData = Mockito.spy(new RaoData(network, crac, crac.getPreventiveState(), Collections.singleton(crac.getPreventiveState()), null, null, null, new RaoParameters()));
        raoParameters = JsonRaoParameters.read(getClass().getResourceAsStream("/SearchTreeRaoParameters.json"));
        systematicSensitivityInterface = Mockito.mock(SystematicSensitivityInterface.class);
        iteratingLinearOptimizer = Mockito.mock(IteratingLinearOptimizer.class);
    }

    private void mockNativeLibraryLoader() {
        PowerMockito.mockStatic(NativeLibraryLoader.class);
        NativeLibraryLoader.loadNativeLibrary("jniortools");
    }

    @Test
    public void testRao() throws Exception {
        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));

        mockNativeLibraryLoader();
        PowerMockito.doReturn("successful").when(iteratingLinearOptimizer).optimize(any());
        PowerMockito.mockStatic(RaoUtil.class);
        PowerMockito.when(RaoUtil.createLinearOptimizer(Mockito.any(), Mockito.any())).thenAnswer(invocationOnMock -> iteratingLinearOptimizer);

        CracResultManager spiedCracResultManager = Mockito.spy(raoData.getCracResultManager());
        Mockito.when(raoData.getCracResultManager()).thenReturn(spiedCracResultManager);
        Mockito.when(raoData.getSystematicSensitivityResult()).thenReturn(Mockito.mock(SystematicSensitivityResult.class));
        Mockito.doNothing().when(spiedCracResultManager).fillCracResultWithCosts(anyDouble(), anyDouble());
        Mockito.doNothing().when(spiedCracResultManager).fillCnecResultWithFlows();

        PowerMockito.whenNew(SystematicSensitivityInterface.class).withAnyArguments().thenReturn(systematicSensitivityInterface);
        Mockito.doReturn(Mockito.mock(SystematicSensitivityResult.class)).when(systematicSensitivityInterface).run(any());

        CracResult cracResult = Mockito.mock(CracResult.class);
        Mockito.doReturn(cracResult).when(raoData).getCracResult();
        Mockito.doReturn(cracResult).when(raoData).getCracResult(anyString());
        Mockito.doReturn(0.0).when(cracResult).getCost();

        PowerMockito.mockStatic(SearchTreeRaoLogger.class);

        TreeParameters treeParameters = TreeParameters.buildForPreventivePerimeter(raoParameters.getExtension(SearchTreeRaoParameters.class));
        Leaf mockLeaf = Mockito.spy(new Leaf(raoData, raoParameters, treeParameters));
        PowerMockito.whenNew(Leaf.class).withAnyArguments().thenReturn(mockLeaf);
        when(mockLeaf.getBestCost()).thenReturn(0.);
        PowerMockito.doNothing().when(mockLeaf).evaluate();

        RaoResult result = searchTree.run(raoData, raoParameters, treeParameters).join();
        assertNotNull(result);
        assertEquals(RaoResult.Status.SUCCESS, result.getStatus());
    }

    @Test(expected = NotImplementedException.class)
    public void optimizeNextLeafAndUpdate() throws Exception {
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        FaraoNetworkPool faraoNetworkPool = Mockito.mock(FaraoNetworkPool.class);
        searchTree.initParameters(raoParameters);
        searchTree.initLeaves(raoData);
        Mockito.doThrow(new NotImplementedException("")).when(networkAction).apply(network);
        searchTree.optimizeNextLeafAndUpdate(networkAction, network, faraoNetworkPool);
    }
}
