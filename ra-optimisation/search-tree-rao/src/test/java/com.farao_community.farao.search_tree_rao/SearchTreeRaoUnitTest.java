/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_loopflow_extension.CnecLoopFlowExtension;
import com.farao_community.farao.data.crac_loopflow_extension.CracLoopFlowExtension;
import com.farao_community.farao.ra_optimisation.RaoComputationResult;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.search_tree_rao.config.LoopFlowExtensionParameters;
import com.farao_community.farao.search_tree_rao.config.SearchTreeConfigurationUtil;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.farao_community.farao.search_tree_rao.process.search_tree.Tree;
import com.farao_community.farao.util.LoadFlowService;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchTreeConfigurationUtil.class, Tree.class})
public class SearchTreeRaoUnitTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchTreeRaoUnitTest.class);

    private SearchTreeRao searchTreeRao;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;

    @Before
    public void setUp() {
        searchTreeRao = new SearchTreeRao();
        FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
        InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);

        computationManager = LocalComputationManager.getDefault();
        raoParameters = RaoParameters.load(platformConfig);

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
    }

    @Test
    public void testNoParam() {
        RaoParameters brokenParameters = Mockito.mock(RaoParameters.class);
        boolean errorCaught = false;
        try {
            searchTreeRao.run(Mockito.mock(Network.class), Mockito.mock(Crac.class), "", computationManager, brokenParameters);
        } catch (FaraoException e) {
            errorCaught = true;
            assertEquals("There are some issues in RAO parameters:" + System.lineSeparator() +
                    "Search Tree Rao parameters not available", e.getMessage());
        }
        assertTrue(errorCaught);
    }

    @Test
    public void testRunLoopFlowExtensionInCracNotAvailable() {
        RaoParameters parameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = Mockito.mock(SearchTreeRaoParameters.class);
        parameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        LoopFlowExtensionParameters loopFlowExtensionParameters = new LoopFlowExtensionParameters();
        loopFlowExtensionParameters.setRaoWithLoopFlow(true);
        assertTrue(loopFlowExtensionParameters.isRaoWithLoopFlow());
        parameters.addExtension(LoopFlowExtensionParameters.class, loopFlowExtensionParameters);
        List<String> emptyList = new ArrayList<>();
        PowerMockito.mockStatic(SearchTreeConfigurationUtil.class);
        Mockito.when(SearchTreeConfigurationUtil.checkSearchTreeRaoConfiguration(parameters)).thenReturn(emptyList);
        PowerMockito.mockStatic(Tree.class);
        RaoComputationResult result = Mockito.mock(RaoComputationResult.class);
        Mockito.when(Tree.search(any(), any(), any(), any())).thenReturn(CompletableFuture.completedFuture(result));
        searchTreeRao.run(Mockito.mock(Network.class), Mockito.mock(Crac.class), "", computationManager, parameters);
    }

    @Test
    public void testCalculateLoopFlowConstraintAndUpdateAllCnec() {
        Network network = ExampleGenerator.network();
        Crac crac = ExampleGenerator.crac();
        Map<String, Double> fzeroallmap = new HashMap<>();
        fzeroallmap.put("FR-BE", 0.0);
        fzeroallmap.put("FR-DE", 0.0);
        fzeroallmap.put("BE-NL", 0.0);
        fzeroallmap.put("DE-NL", 0.0);
        CracLoopFlowExtension cracLoopFlowExtension = new CracLoopFlowExtension();
        crac.addExtension(CracLoopFlowExtension.class, cracLoopFlowExtension);
        searchTreeRao.updateCnecsLoopFlowConstraint(crac, fzeroallmap);
        crac.getCnecs().forEach(cnec -> {
            assertEquals(100.0, cnec.getExtension(CnecLoopFlowExtension.class).getLoopFlowConstraint(), 1E-1);
        });
    }

    @Test
    public void getName() {
        assertEquals("SearchTreeRao", searchTreeRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", searchTreeRao.getVersion());
    }
}
