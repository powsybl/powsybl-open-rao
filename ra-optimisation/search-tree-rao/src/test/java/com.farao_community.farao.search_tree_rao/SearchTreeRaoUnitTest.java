/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.search_tree_rao.config.SearchTreeConfigurationUtil;
import com.farao_community.farao.search_tree_rao.process.search_tree.Tree;
import com.farao_community.farao.util.LoadFlowService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchTreeConfigurationUtil.class, Tree.class})
public class SearchTreeRaoUnitTest {

    private SearchTreeRao searchTreeRao;
    private ComputationManager computationManager;

    @Before
    public void setUp() {
        searchTreeRao = new SearchTreeRao();

        computationManager = LocalComputationManager.getDefault();

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
    public void getName() {
        assertEquals("SearchTreeRao", searchTreeRao.getName());
    }

    @Test
    public void getVersion() {
        assertEquals("1.0.0", searchTreeRao.getVersion());
    }
}
