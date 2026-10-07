/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.range.RangeType;
import com.powsybl.openrao.data.crac.api.range.StandardRange;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.VariationDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gabriel Plante {@literal <gabriel.plante_externe at rte-france.com>}
 */
class CounterTradeRangeActionAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Crac crac;
    private Network network;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        network = Network.read("TestCase12Nodes_no_pst.uct", getClass().getResourceAsStream("/TestCase12Nodes_no_pst.uct"));
    }

    @Test
    void testAdd() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withActivationCost(1000d)
                .withVariationCost(10000d, VariationDirection.UP)
                .withVariationCost(20000d, VariationDirection.DOWN)
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .withConnectedAreas(network)
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("id1", counterTradeRangeAction.getName());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertEquals(Optional.of(1000d), counterTradeRangeAction.getActivationCost());
        assertEquals(Optional.of(10000d), counterTradeRangeAction.getVariationCost(VariationDirection.UP));
        assertEquals(Optional.of(20000d), counterTradeRangeAction.getVariationCost(VariationDirection.DOWN));
        assertTrue(counterTradeRangeAction.getGroupId().isPresent());
        assertEquals("groupId1", counterTradeRangeAction.getGroupId().get());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals("BE", counterTradeRangeAction.getArea());
        assertEquals(1000d, counterTradeRangeAction.getInitialNetPosition());
        assertEquals(2, counterTradeRangeAction.getConnectedAreas().size());
        assertEquals("FR", counterTradeRangeAction.getConnectedAreas().get(0).getArea());
        assertEquals("NL", counterTradeRangeAction.getConnectedAreas().get(1).getArea());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutGroupId() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertTrue(counterTradeRangeAction.getGroupId().isEmpty());
        assertTrue(counterTradeRangeAction.getActivationCost().isEmpty());
        assertTrue(counterTradeRangeAction.getVariationCost(VariationDirection.UP).isEmpty());
        assertTrue(counterTradeRangeAction.getVariationCost(VariationDirection.DOWN).isEmpty());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals("BE", counterTradeRangeAction.getArea());
        assertEquals(1000d, counterTradeRangeAction.getInitialNetPosition());
        assertEquals(2, counterTradeRangeAction.getConnectedAreas().size());
        assertEquals("FR", counterTradeRangeAction.getConnectedAreas().get(0).getArea());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutUsageRule() {
        /*
        This behaviour is considered admissible:
            - without usage rule, the remedial action will never be available

        This test should however issue a warning
         */
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(0, counterTradeRangeAction.getUsageRules().size());
        assertEquals("BE", counterTradeRangeAction.getArea());
        assertEquals(1000d, counterTradeRangeAction.getInitialNetPosition());
        assertEquals(2, counterTradeRangeAction.getConnectedAreas().size());
        assertEquals("FR", counterTradeRangeAction.getConnectedAreas().get(0).getArea());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutOperator() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withGroupId("groupId1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .withConnectedAreas(network)
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertNull(counterTradeRangeAction.getOperator());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals("BE", counterTradeRangeAction.getArea());
        assertEquals(1000d, counterTradeRangeAction.getInitialNetPosition());
        assertEquals(2, counterTradeRangeAction.getConnectedAreas().size());
        assertEquals("FR", counterTradeRangeAction.getConnectedAreas().get(0).getArea());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutConnectedAreas() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals("BE", counterTradeRangeAction.getArea());
        assertEquals(1000d, counterTradeRangeAction.getInitialNetPosition());
        assertTrue(counterTradeRangeAction.getConnectedAreas().isEmpty());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testConnectedAreaWithoutNetworkFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newConnectedArea().withArea("FR").add()
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot check that the connected areas of CounterTradeRangeAction id1 share a border with area BE without a network. Please use withConnectedAreas()", e.getMessage());
    }

    @Test
    void testAddWithDefinedConnectedAreas() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newConnectedArea().withArea("NL")
                    .newBorderRange().withMin(-500).withMax(500).withRangeType(RangeType.ABSOLUTE).add()
                    .add()
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        // FR also shares a border with BE but is not added since connected areas were defined
        assertEquals(1, counterTradeRangeAction.getConnectedAreas().size());
        assertEquals("NL", counterTradeRangeAction.getConnectedAreas().get(0).getArea());
        assertEquals(1, counterTradeRangeAction.getConnectedAreas().get(0).getBorderRanges().size());
        StandardRange borderRange = counterTradeRangeAction.getConnectedAreas().get(0).getBorderRanges().get(0);
        assertEquals(-500., borderRange.getMin());
        assertEquals(500., borderRange.getMax());
        assertEquals(RangeType.ABSOLUTE, borderRange.getRangeType());
    }

    @Test
    void testConnectedAreaWithoutCommonBorderFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .newConnectedArea().withArea("DE").add()
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Connected area DE of CounterTradeRangeAction id1 does not share a border with area BE", e.getMessage());
    }

    @Test
    void testAddBorderRangesWithoutMinOrMax() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newConnectedArea().withArea("FR")
                    .newBorderRange().withMin(-500).withRangeType(RangeType.ABSOLUTE).add()
                    .newBorderRange().withMax(500).withRangeType(RangeType.ABSOLUTE).add()
                    .add()
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();

        List<StandardRange> borderRanges = counterTradeRangeAction.getConnectedAreas().get(0).getBorderRanges();
        assertEquals(2, borderRanges.size());
        assertEquals(-500., borderRanges.get(0).getMin());
        assertEquals(Double.MAX_VALUE, borderRanges.get(0).getMax());
        assertEquals(-Double.MAX_VALUE, borderRanges.get(1).getMin());
        assertEquals(500., borderRanges.get(1).getMax());
    }

    @Test
    void testNoIdFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withOperator("BE")
                .withGroupId("groupId1")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add a CounterTradeRangeAction object with no specified id. Please use withId()", e.getMessage());
    }

    @Test
    void testNoAreaFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a area. Please use withArea() with a non null value", e.getMessage());
    }

    @Test
    void testNoInitialNetPositionFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withArea("BE")
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a initialNetPosition. Please use withInitialNetPosition() with a non null value", e.getMessage());
    }

    @Test
    void testNoRangeFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a range. Please use newRange()", e.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add()
                .add();
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withArea("BE")
                .withInitialNetPosition(1000d)
                .withConnectedAreas(network)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", e.getMessage());
    }
}
