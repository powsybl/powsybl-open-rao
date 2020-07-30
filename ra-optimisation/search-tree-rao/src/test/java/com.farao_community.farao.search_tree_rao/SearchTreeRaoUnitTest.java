/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.search_tree_rao;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.util.FaraoNetworkPool;
import com.farao_community.farao.util.LoadFlowService;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResultImpl;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Optional;

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

    @Test(expected = NotImplementedException.class)
    public void optimizeNextLeafAndUpdate() throws Exception {
        String variantId = "variantId";
        SearchTreeRao searchTreeRao = new SearchTreeRao();
        NetworkAction networkAction = Mockito.mock(NetworkAction.class);
        Network network = NetworkImportsUtil.import12NodesNetwork();
        VariantManager variantManager = network.getVariantManager();
        variantManager.cloneVariant(variantManager.getWorkingVariantId(), variantId);
        Crac crac = new SimpleCrac("id");
        crac.addState(new SimpleState(Optional.empty(), new Instant("instantId", 1)));
        RaoParameters raoParameters = new RaoParameters();
        FaraoNetworkPool faraoNetworkPool = Mockito.mock(FaraoNetworkPool.class);
        searchTreeRao.init(network, crac, variantId, raoParameters);
        Mockito.doThrow(new NotImplementedException("")).when(networkAction).apply(network);
        searchTreeRao.optimizeNextLeafAndUpdate(networkAction, network, faraoNetworkPool);
    }
}
