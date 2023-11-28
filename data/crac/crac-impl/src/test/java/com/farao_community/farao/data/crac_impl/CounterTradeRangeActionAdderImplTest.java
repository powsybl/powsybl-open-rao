/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.data.crac_impl;

import com.farao_community.farao.commons.FaraoException;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.InstantKind;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeAction;
import com.farao_community.farao.data.crac_api.range_action.CounterTradeRangeActionAdder;
import com.farao_community.farao.data.crac_api.usage_rule.UsageMethod;
import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CounterTradeRangeActionAdderImplTest {
    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private CracImpl crac;
    private Instant preventiveInstant;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);
        preventiveInstant = crac.getInstant(PREVENTIVE_INSTANT_ID);
    }

    @Test
    void testAdd() {
        CounterTradeRangeAction counterTradeRangeAction = (CounterTradeRangeAction) crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("id1", counterTradeRangeAction.getName());
        assertEquals("BE", counterTradeRangeAction.getOperator());
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
        CounterTradeRangeAction counterTradeRangeAction = (CounterTradeRangeAction) crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", counterTradeRangeAction.getId());
        assertEquals("BE", counterTradeRangeAction.getOperator());
        assertTrue(counterTradeRangeAction.getGroupId().isEmpty());
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
        CounterTradeRangeAction counterTradeRangeAction = (CounterTradeRangeAction) crac.newCounterTradeRangeAction()
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
        CounterTradeRangeAction counterTradeRangeAction = (CounterTradeRangeAction) crac.newCounterTradeRangeAction()
                .withId("id1")
                .withGroupId("groupId1")
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
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
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(FaraoException.class, counterTradeRangeActionAdder::add);
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
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(FaraoException.class, counterTradeRangeActionAdder::add);
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
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(FaraoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a importing country. Please use withImportingCountry() with a non null value", e.getMessage());
    }

    @Test
    void testNoRangeFail() {
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(FaraoException.class, counterTradeRangeActionAdder::add);
        assertEquals("Cannot add CounterTradeRangeAction without a range. Please use newRange()", e.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        CounterTradeRangeActionAdder counterTradeRangeActionAdder = crac.newCounterTradeRangeAction()
                .withId("sameId")
                .withExportingCountry(Country.FR)
                .withImportingCountry(Country.DE)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(preventiveInstant).withUsageMethod(UsageMethod.AVAILABLE).add();
        Exception e = assertThrows(FaraoException.class, counterTradeRangeActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", e.getMessage());
    }
}
