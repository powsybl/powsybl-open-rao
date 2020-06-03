/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.util.LoadFlowService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Pengbo Wang {@literal <pengbo.wang at rte-international.com>}
 */
public class SearchTreeRaoUnitTest {

    private SearchTreeRao searchTreeRao;

    @Before
    public void setUp() {
        searchTreeRao = new SearchTreeRao();

        ComputationManager computationManager = LocalComputationManager.getDefault();

        LoadFlow.Runner loadFlowRunner = Mockito.mock(LoadFlow.Runner.class);
        when(loadFlowRunner.run(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new LoadFlowResultImpl(true, Collections.emptyMap(), ""));
        LoadFlowService.init(loadFlowRunner, computationManager);
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
