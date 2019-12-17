/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.search_tree_rao.process.search_tree;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.NetworkAction;
import com.farao_community.farao.rao_api.RaoParameters;
import com.farao_community.farao.search_tree_rao.config.SearchTreeRaoParameters;
import com.google.common.collect.ImmutableList;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.search_tree_rao.mock.LinearRangeRaoMock.*;
import static org.junit.Assert.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-international.com>}
 */
public class LeafTest {

    private NetworkAction na1;
    private NetworkAction na2;
    private NetworkAction na3;

    private Network network;
    private Crac crac;
    private ComputationManager computationManager;
    private RaoParameters raoParameters;

    @Before
    public void setUp() {
        // network
        network = Importers.loadNetwork("TestCase12Nodes.uct", getClass().getResourceAsStream("/TestCase12Nodes.uct"));

        // network action mocks
        na1 = Mockito.mock(NetworkAction.class);
        Mockito.when(na1.getId()).thenReturn("topological_RA");

        na2 = Mockito.mock(NetworkAction.class);
        Mockito.when(na2.getId()).thenReturn("PSTsetpoint_RA");

        na3 = Mockito.mock(NetworkAction.class);
        Mockito.when(na3.getId()).thenReturn("anotherNA_RA");

        // other mocks
        crac = Mockito.mock(Crac.class);

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
        List<NetworkAction> twoNetworkActions = new ArrayList<>();
        twoNetworkActions.add(na1);
        twoNetworkActions.add(na2);

        Leaf rootLeaf = new Leaf("referenceVariant");
        List<Leaf> firstGeneration = rootLeaf.bloom(twoNetworkActions);

        assertTrue(rootLeaf.isRoot());
        assertEquals(2, firstGeneration.size());
        assertFalse(firstGeneration.get(0).isRoot());

        assertNull(rootLeaf.getParent());
        assertNull(rootLeaf.getNetworkAction());
        assertTrue(rootLeaf.getNetworkActionLegacy().isEmpty());
        assertNull(rootLeaf.getRaoResult());
        assertEquals("referenceVariant", rootLeaf.getNetworkVariant());
        assertEquals(Leaf.Status.CREATED, rootLeaf.getStatus());

        assertEquals(rootLeaf, firstGeneration.get(0).getParent());
        assertEquals(1, firstGeneration.stream().filter(l -> l.getNetworkAction().getId().equals("topological_RA")).count());
        assertEquals(1, firstGeneration.stream().filter(l -> l.getNetworkAction().getId().equals("PSTsetpoint_RA")).count());
        assertEquals(1, firstGeneration.get(0).getNetworkActionLegacy().size());
        assertEquals(firstGeneration.get(0).getNetworkAction(), firstGeneration.get(0).getNetworkActionLegacy().get(0));

        assertNull(firstGeneration.get(0).getNetworkVariant());

        assertTrue(rootLeaf.bloom(new ArrayList<>()).isEmpty());

        // second generation - left
        List<NetworkAction> oneNetworkAction = new ArrayList<>();
        oneNetworkAction.add(na3);
        List<Leaf> secondGenerationL = firstGeneration.get(0).bloom(oneNetworkAction);

        assertEquals(2, secondGenerationL.get(0).getNetworkActionLegacy().size());

        // second generation - right
        List<NetworkAction> threeNetworkActions = new ArrayList<>();
        threeNetworkActions.add(na1);
        threeNetworkActions.add(na2);
        threeNetworkActions.add(na3);
        List<Leaf> secondGenerationR = firstGeneration.get(1).bloom(threeNetworkActions);

        assertEquals(2, secondGenerationR.size());
        assertEquals(2, secondGenerationR.get(0).getNetworkActionLegacy().size());
        assertTrue(secondGenerationR.get(0).getNetworkActionLegacy().contains(firstGeneration.get(1).getNetworkAction()));
    }

    @Test
    public void evaluateOkTest() {
        Mockito.when(crac.getName()).thenReturn("CracOk");

        String initialVariant = network.getVariantManager().getWorkingVariantId();
        Leaf rootLeaf = new Leaf(initialVariant);
        rootLeaf.evaluate(network, crac, raoParameters);

        assertEquals(initialVariant, network.getVariantManager().getWorkingVariantId());
        assertEquals(Leaf.Status.EVALUATION_SUCCESS, rootLeaf.getStatus());

        List<Leaf> childrenLeaf = rootLeaf.bloom(ImmutableList.of(na1));
        childrenLeaf.get(0).evaluate(network, crac, raoParameters);

        assertNotEquals(initialVariant, network.getVariantManager().getWorkingVariantId());
        assertEquals(childrenLeaf.get(0).getNetworkVariant(), network.getVariantManager().getWorkingVariantId());
    }

    @Test
    public void evaluateWithRaoExceptionTest() {
        Mockito.when(crac.getName()).thenReturn(CRAC_NAME_RAO_THROWS_EXCEPTION);

        Leaf rootLeaf = new Leaf(network.getVariantManager().getWorkingVariantId());
        rootLeaf.evaluate(network, crac, raoParameters);

        assertEquals(Leaf.Status.EVALUATION_ERROR, rootLeaf.getStatus());
    }

    @Test
    public void evaluateWithRaoFailureTest() {
        Mockito.when(crac.getName()).thenReturn(CRAC_NAME_RAO_RETURNS_FAILURE);

        Leaf rootLeaf = new Leaf(network.getVariantManager().getWorkingVariantId());
        rootLeaf.evaluate(network, crac, raoParameters);

        assertEquals(Leaf.Status.EVALUATION_ERROR, rootLeaf.getStatus());
    }

}
