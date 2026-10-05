/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.searchtree.algorithms;

import com.powsybl.action.TerminalsConnectionAction;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.PstRangeAction;
import com.powsybl.openrao.searchtreerao.commons.NetworkActionCombination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.NETWORK;
import static com.powsybl.openrao.searchtreerao.searchtree.algorithms.NetworkActionCombinationsUtils.P_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
class MaximumNumberOfElementaryActionsFilterTest {
    @Test
    void testRemoveCombinationsWhichExceedMaxElementaryActionsPerTso() {
        PstRangeAction pstRangeAction = NetworkActionCombinationsUtils.addPstRangeActionToCrac();
        pstRangeAction.apply(NETWORK, pstRangeAction.getTapToAngleConversionMap().get(3));

        TerminalsConnectionAction topoFr1 = Mockito.mock(TerminalsConnectionAction.class);
        TerminalsConnectionAction topoFr2 = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naFr = Mockito.mock(NetworkAction.class);
        Mockito.when(naFr.getOperator()).thenReturn("FR");
        Mockito.when(naFr.getElementaryActions()).thenReturn(Set.of(topoFr1, topoFr2));

        TerminalsConnectionAction topoNl = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naNl = Mockito.mock(NetworkAction.class);
        Mockito.when(naNl.getOperator()).thenReturn("NL");
        Mockito.when(naNl.getElementaryActions()).thenReturn(Set.of(topoNl));

        TerminalsConnectionAction topoBe = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naBe = Mockito.mock(NetworkAction.class);
        Mockito.when(naBe.getOperator()).thenReturn("BE");
        Mockito.when(naBe.getElementaryActions()).thenReturn(Set.of(topoBe));

        TerminalsConnectionAction topoDe = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naDe = Mockito.mock(NetworkAction.class);
        Mockito.when(naDe.getOperator()).thenReturn("DE");
        Mockito.when(naDe.getElementaryActions()).thenReturn(Set.of(topoDe));

        NetworkActionCombination networkActionCombinationFrNl = new NetworkActionCombination(Set.of(naFr, naNl));
        NetworkActionCombination networkActionCombinationBe = new NetworkActionCombination(Set.of(naBe));
        NetworkActionCombination networkActionCombinationDe = new NetworkActionCombination(Set.of(naDe));

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(leaf.getActivatedRangeActions(P_STATE)).thenReturn(Set.of(pstRangeAction));

        MaximumNumberOfElementaryActionsFilter naFilter = new MaximumNumberOfElementaryActionsFilter(Map.of("BE", 3, "DE", 2, "FR", 1, "NL", 2));
        Set<NetworkActionCombination> result = naFilter.filter(Set.of(networkActionCombinationFrNl, networkActionCombinationBe, networkActionCombinationDe), leaf, ReportNode.NO_OP);

        assertEquals(result, Set.of(networkActionCombinationBe, networkActionCombinationDe));
    }

    @Test
    void testCommonElementaryActionsInNetworkActionCombination() {
        PstRangeAction pstRangeAction = NetworkActionCombinationsUtils.addPstRangeActionToCrac();
        pstRangeAction.apply(NETWORK, pstRangeAction.getTapToAngleConversionMap().get(3));

        TerminalsConnectionAction topoFr1 = Mockito.mock(TerminalsConnectionAction.class);
        TerminalsConnectionAction topoFr2 = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naFr = Mockito.mock(NetworkAction.class);
        Mockito.when(naFr.getOperator()).thenReturn("FR");
        Mockito.when(naFr.getElementaryActions()).thenReturn(Set.of(topoFr1, topoFr2));

        TerminalsConnectionAction topoNl = Mockito.mock(TerminalsConnectionAction.class);
        NetworkAction naNlFr = Mockito.mock(NetworkAction.class);
        Mockito.when(naNlFr.getOperator()).thenReturn("FR");
        Mockito.when(naNlFr.getElementaryActions()).thenReturn(Set.of(topoNl, topoFr1));

        NetworkActionCombination networkActionCombinationFrNl = new NetworkActionCombination(Set.of(naFr, naNlFr));

        Leaf leaf = Mockito.mock(Leaf.class);
        Mockito.when(leaf.getActivatedNetworkActions()).thenReturn(Collections.emptySet());
        Mockito.when(leaf.getActivatedRangeActions(P_STATE)).thenReturn(Set.of(pstRangeAction));

        MaximumNumberOfElementaryActionsFilter naFilter = new MaximumNumberOfElementaryActionsFilter(Map.of("FR", 3));
        assertEquals(Set.of(networkActionCombinationFrNl), naFilter.filter(Set.of(networkActionCombinationFrNl), leaf, ReportNode.NO_OP));
    }
}
