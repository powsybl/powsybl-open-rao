/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_impl.SimpleState;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.rao_commons.RaoData;
import com.farao_community.farao.rao_commons.linear_optimisation.iterating_linear_optimizer.IteratingLinearOptimizer;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.farao_community.farao.data.crac_impl.utils.NetworkImportsUtil;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static com.farao_community.farao.search_tree_rao.mock.LinearRaoMock.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({IteratingLinearOptimizer.class})
public class LeafTest {

    private NetworkAction na1;
    private NetworkAction na2;
    private NetworkAction na3;

    private Network network;
    private Crac crac;
    private RaoData raoData;
    private RaoParameters raoParameters;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(IteratingLinearOptimizer.class);
        // network
        network = NetworkImportsUtil.import12NodesNetwork();

        // network action mocks
        na1 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getId()).thenReturn("topological_RA");

        na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na2.getId()).thenReturn("PSTsetpoint_RA");

        na3 = Mockito.mock(NetworkAction.class);
        Mockito.when(na3.getId()).thenReturn("anotherNA_RA");

        // other mocks
        crac = Mockito.mock(Crac.class);

        raoData = Mockito.mock(RaoData.class);
        Mockito.when(raoData.getCrac()).thenReturn(crac);
        Mockito.when(raoData.getNetwork()).thenReturn(network);

        // rao parameters
        raoParameters = new RaoParameters();
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        searchTreeRaoParameters.setRangeActionRao("Linear Range Action Rao Mock");
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
    }

    @Test
    public void bloomTest() {
        /*
        creation of leaves with :

        rootLeaf                    ROOT
                                  /      \
        firstGeneration         NA1      NA2
                                 |      /   \
        secondGeneration        NA3    NA1  NA3
         */

        // first generation
        Set<NetworkAction> twoNetworkActions = new HashSet<>();
        twoNetworkActions.add(na1);
        twoNetworkActions.add(na2);

        Leaf rootLeaf = new Leaf();
        List<Leaf> firstGeneration = rootLeaf.bloom(twoNetworkActions);

        assertTrue(rootLeaf.isRoot());
        assertEquals(2, firstGeneration.size());
        assertFalse(firstGeneration.get(0).isRoot());

        assertNull(rootLeaf.getParent());
        assertTrue(rootLeaf.getNetworkActions().isEmpty());
        assertNull(rootLeaf.getRaoResult());
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());

        assertEquals(rootLeaf, firstGeneration.get(0).getParent());
        assertEquals(1, firstGeneration.get(0).getNetworkActions().size());
        assertEquals(1, firstGeneration.stream().filter(l -> l.getNetworkActions().get(0).getId().equals("topological_RA")).count());
        assertEquals(1, firstGeneration.stream().filter(l -> l.getNetworkActions().get(0).getId().equals("PSTsetpoint_RA")).count());

        assertTrue(rootLeaf.bloom(Collections.emptySet()).isEmpty());

        // second generation - left
        Set<NetworkAction> oneNetworkAction = new HashSet<>();
        oneNetworkAction.add(na3);
        List<Leaf> secondGenerationL = firstGeneration.get(0).bloom(oneNetworkAction);

        assertEquals(2, secondGenerationL.get(0).getNetworkActions().size());

        // second generation - right
        Set<NetworkAction> threeNetworkActions = new HashSet<>();
        threeNetworkActions.add(na1);
        threeNetworkActions.add(na2); // filtered because already present in the leaf legacy
        threeNetworkActions.add(na3);
        List<Leaf> secondGenerationR = firstGeneration.get(1).bloom(threeNetworkActions);

        assertEquals(2, secondGenerationR.size());
        assertEquals(2, secondGenerationR.get(0).getNetworkActions().size());
        assertTrue(secondGenerationR.get(0).getNetworkActions().containsAll(firstGeneration.get(1).getNetworkActions()));
    }

    @Ignore
    @Test
    public void evaluateOkTest() {
        BDDMockito.given(IteratingLinearOptimizer.optimize(any(), any())).willReturn("successful");
        crac.addState(new SimpleState(Optional.empty(), new Instant("preventiveInstant", 0)));

        String initialVariant = network.getVariantManager().getWorkingVariantId();
        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(raoData, initialVariant, raoParameters);

        assertEquals(1, network.getVariantManager().getVariantIds().size());
        assertEquals(Leaf.Status.EVALUATION_SUCCESS, rootLeaf.getStatus());

        List<Leaf> childrenLeaf = rootLeaf.bloom(Collections.singleton(na1));
        childrenLeaf.get(0).evaluate(raoData, initialVariant, raoParameters);

        assertEquals(1, network.getVariantManager().getVariantIds().size());
        assertEquals(Leaf.Status.EVALUATION_SUCCESS, rootLeaf.getStatus());
    }

    @Test
    public void evaluateWithRaoExceptionTest() {
        BDDMockito.given(IteratingLinearOptimizer.optimize(any(), any())).willThrow(new FaraoException("error with optim"));
        String initialVariant = network.getVariantManager().getWorkingVariantId();

        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(raoData, initialVariant, raoParameters);

        assertEquals(Leaf.Status.EVALUATION_ERROR, rootLeaf.getStatus());
    }

    @Ignore
    @Test
    public void evaluateWithRaoFailureTest() {
        crac = new SimpleCrac(CRAC_NAME_RAO_RETURNS_FAILURE);
        crac.addState(new SimpleState(Optional.empty(), new Instant("preventiveInstant", 0)));

        String initialVariant = network.getVariantManager().getWorkingVariantId();

        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(raoData, initialVariant, raoParameters);

        assertEquals(Leaf.Status.EVALUATION_ERROR, rootLeaf.getStatus());
    }

    @Test
    public void evaluateWithUnknownVariantTest() {
        Mockito.when(crac.getName()).thenReturn("CracOK");
        String initialVariant = network.getVariantManager().getWorkingVariantId();

        Leaf rootLeaf = new Leaf();
        rootLeaf.evaluate(raoData, "unknown variant", raoParameters);

        assertEquals(Leaf.Status.EVALUATION_ERROR, rootLeaf.getStatus());
    }
}
