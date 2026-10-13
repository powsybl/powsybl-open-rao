/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.io.commons.iidm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ConnectedArea;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * @author Pedro Tobarra {@literal <pedro.tobarra at artelys.com>}
 */
class IidmCounterTradeHelperTest {

    private Network network;

    @BeforeEach
    public void setUp() {
        // borders in the network: BE-FR, BE-NL, BE-DE, FR-DE and NL-DE
        network = Network.read("TestCase12Nodes_forCSE.uct", getClass().getResourceAsStream("/TestCase12Nodes_forCSE.uct"));
    }

    @Test
    void testGetNeighbors() {
        assertEquals(List.of("BE", "DE"), new IidmCounterTradeHelper("FR", network).getNeighbors());
        assertEquals(List.of("DE", "FR", "NL"), new IidmCounterTradeHelper("BE", network).getNeighbors());
    }

    @Test
    void testAreaNotCountryFail() {
        Exception e = assertThrows(OpenRaoException.class, () -> new IidmCounterTradeHelper("10YFR-RTE------C", network));
        assertEquals("Area 10YFR-RTE------C is not a country, its neighbors cannot be computed from the network", e.getMessage());
    }

    @Test
    void testValidateConnectedAreas() {
        IidmCounterTradeHelper counterTradeHelper = new IidmCounterTradeHelper("FR", network);
        CounterTradeRangeAction counterTradeRangeAction = mockCounterTradeRangeAction("BE", "DE");
        assertDoesNotThrow(() -> counterTradeHelper.validateConnectedAreas(counterTradeRangeAction));
        CounterTradeRangeAction counterTradeRangeActionWithoutConnectedAreas = mockCounterTradeRangeAction();
        assertDoesNotThrow(() -> counterTradeHelper.validateConnectedAreas(counterTradeRangeActionWithoutConnectedAreas));
    }

    @Test
    void testValidateConnectedAreasWithoutCommonBorderFail() {
        IidmCounterTradeHelper counterTradeHelper = new IidmCounterTradeHelper("FR", network);
        CounterTradeRangeAction counterTradeRangeAction = mockCounterTradeRangeAction("BE", "NL");
        Exception e = assertThrows(OpenRaoException.class, () -> counterTradeHelper.validateConnectedAreas(counterTradeRangeAction));
        assertEquals("Connected area NL of CounterTradeRangeAction ctId does not share a border with area FR in the network", e.getMessage());
    }

    private static CounterTradeRangeAction mockCounterTradeRangeAction(String... connectedAreaIds) {
        List<ConnectedArea> connectedAreas = Arrays.stream(connectedAreaIds).map(connectedAreaId -> {
            ConnectedArea connectedArea = Mockito.mock(ConnectedArea.class);
            when(connectedArea.getAreaId()).thenReturn(connectedAreaId);
            return connectedArea;
        }).toList();
        CounterTradeRangeAction counterTradeRangeAction = Mockito.mock(CounterTradeRangeAction.class);
        when(counterTradeRangeAction.getId()).thenReturn("ctId");
        when(counterTradeRangeAction.getConnectedAreas()).thenReturn(connectedAreas);
        return counterTradeRangeAction;
    }
}
