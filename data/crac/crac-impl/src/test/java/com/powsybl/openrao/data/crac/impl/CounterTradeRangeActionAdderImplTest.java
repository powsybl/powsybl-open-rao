/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.data.crac.impl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.InstantKind;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeAction;
import com.powsybl.openrao.data.crac.api.rangeaction.CounterTradeRangeActionAdder;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.api.usagerule.UsageMethod;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CounterTradeRangeActionAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
    }

    @Test
    void testAdd() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withActivationCost(1000d)
                .withVariationCost(10000d, RangeAction.VariationDirection.UP)
                .withVariationCost(20000d, RangeAction.VariationDirection.DOWN)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("id1", counterTradeRangeAction.getName());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertEquals(Optional.of(1000d), counterTradeRangeAction.getActivationCost());
        assertEquals(Optional.of(10000d), counterTradeRangeAction.getVariationCost(RangeAction.VariationDirection.UP));
        assertEquals(Optional.of(20000d), counterTradeRangeAction.getVariationCost(RangeAction.VariationDirection.DOWN));
        assertTrue(counterTradeRangeAction.getGroupId().isPresent());
        assertEquals("groupId1", counterTradeRangeAction.getGroupId().get());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals(Country.FR, counterTradeRangeAction.getExportingCountry());
        assertEquals(Country.DE, counterTradeRangeAction.getImportingCountry());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutGroupId() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertTrue(counterTradeRangeAction.getGroupId().isEmpty());
        assertTrue(counterTradeRangeAction.getActivationCost().isEmpty());
        assertTrue(counterTradeRangeAction.getVariationCost(RangeAction.VariationDirection.UP).isEmpty());
        assertTrue(counterTradeRangeAction.getVariationCost(RangeAction.VariationDirection.DOWN).isEmpty());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals(Country.FR, counterTradeRangeAction.getExportingCountry());
        assertEquals(Country.DE, counterTradeRangeAction.getImportingCountry());

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
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(0, counterTradeRangeAction.getUsageRules().size());
        assertEquals(Country.FR, counterTradeRangeAction.getExportingCountry());
        assertEquals(Country.DE, counterTradeRangeAction.getImportingCountry());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutOperator() {
        CounterTradeRangeAction counterTradeRangeAction = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withGroupId("groupId1")
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertNull(counterTradeRangeAction.getOperator());
        assertEquals(1, counterTradeRangeAction.getRanges().size());
        assertEquals(1, counterTradeRangeAction.getUsageRules().size());
        assertEquals(Country.FR, counterTradeRangeAction.getExportingCountry());
        assertEquals(Country.DE, counterTradeRangeAction.getImportingCountry());

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testNoIdFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withOperator("BE")
                .withGroupId("groupId1")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add a CounterTradeRangeAction object with no specified id. Please use withId()", e.getMessage());
    }

    @Test
    void testNoExportingCountryFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a exporting country. Please use withExportingCountry() with a non null value", e.getMessage());
    }

    @Test
    void testNoImportingCountryFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withExportingCountry(Country.FR)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a importing country. Please use withImportingCountry() with a non null value", e.getMessage());
    }

    @Test
    void testNoRangeFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a range. Please use newRange()", e.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(OpenRaoException.class, counterTradeRangeActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", e.getMessage());
    }
}
