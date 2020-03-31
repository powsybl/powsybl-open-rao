/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.data.crac_api.*;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.remedial_action.network_action.Topology;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.data.crac_result_extensions.NetworkActionResultExtension;
import com.farao_community.farao.rao_api.Rao;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_api.RaoResult;
import com.farao_community.farao.search_tree_rao.mock.LinearRaoMock;
import com.farao_community.farao.search_tree_rao.mock.RaoRunnerMock;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.iidm.network.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.file.FileSystem;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Rao.class)
public class TreeTest {

    private static final double DOUBLE_TOLERANCE = 0.01;

    @Test
    public void buildOutputTest() {

        // Get RaoComputationResults
        RaoResult rootRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        rootRaoResult.setPreOptimVariantId("rootPreOptim");
        rootRaoResult.setPostOptimVariantId("rootPostOptim");
        RaoResult optimalRaoResult = new RaoResult(RaoResult.Status.SUCCESS);
        optimalRaoResult.setPreOptimVariantId("leafPreOptim");
        optimalRaoResult.setPostOptimVariantId("leafPostOptim");

        // Mock root leaf
        Leaf rootLeaf = Mockito.mock(Leaf.class);
        Mockito.when(rootLeaf.getRaoResult()).thenReturn(rootRaoResult);

        // Mock optimal Leaf
        Leaf optimalLeaf = Mockito.mock(Leaf.class);
        Mockito.when(optimalLeaf.getRaoResult()).thenReturn(optimalRaoResult);

        // build output
        RaoResult result = Tree.buildOutput(rootLeaf, optimalLeaf);

        assertTrue(result.isSuccessful());
        assertEquals("rootPreOptim", result.getPreOptimVariantId());
        assertEquals("leafPostOptim", result.getPostOptimVariantId());
    }

    @Test
    public void brokenRootSearchTest() {
        Network network = Mockito.mock(Network.class);
        VariantManager variantManager = Mockito.mock(VariantManager.class);
        Mockito.when(network.getVariantManager()).thenReturn(variantManager);
        RaoResult result;
        try {
            result = Tree.search(network, Mockito.mock(Crac.class), "", Mockito.mock(RaoParameters.class)).get();
            assertEquals(RaoResult.Status.FAILURE, result.getStatus());
        } catch (Exception e) {
            throw new AssertionError();
        }
    }

    @Test
    public void searchTest() {
        SimpleCrac crac = new SimpleCrac("id");
        crac.addState(new SimpleState(Optional.empty(), new Instant("inst", 0)));
        NetworkElement networkElement = new NetworkElement("BBE1AA1  BBE2AA1  1");
        crac.addNetworkElement(networkElement);
        Topology topo = new Topology("topo", networkElement, ActionType.OPEN);
        crac.addNetworkAction(topo);
        Network network = NetworkImportsUtil.import12NodesNetwork();
        crac.synchronize(network);
        String referenceNetworkVariant = network.getVariantManager().getWorkingVariantId();

        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
        RaoParameters raoParameters = RaoParameters.load(platformConfig);

        PowerMockito.mockStatic(Rao.class);
        when(Rao.find(Mockito.anyString())).thenReturn(new RaoRunnerMock(new LinearRaoMock()));

        try {
            RaoResult raoResult = Tree.search(network, crac, referenceNetworkVariant, raoParameters).get();
            String postOptId = raoResult.getPostOptimVariantId();
            String prevStateId = crac.getPreventiveState().getId();
            assertFalse(topo.getExtension(NetworkActionResultExtension.class).getVariant(postOptId).isActivated(prevStateId));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
