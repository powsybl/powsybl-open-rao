/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracimpl;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeAction;
import com.powsybl.openrao.data.cracapi.rangeaction.InjectionRangeActionAdder;
import com.powsybl.openrao.data.cracapi.usagerule.UsageMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
class InjectionRangeActionAdderImplTest {

    private static final String PREVENTIVE_INSTANT_ID = "preventive";

    private CracImpl crac;
    private String injectionId1;
    private String injectionId2;
    private String injectionName2;

    @BeforeEach
    public void setUp() {
        crac = new CracImpl("test-crac")
            .newInstant(PREVENTIVE_INSTANT_ID, InstantKind.PREVENTIVE);

        injectionId1 = "BBE2AA11_Generator";
        injectionId2 = "FFR3AA11_Load";
        injectionName2 = "Load in FFR3AA11";
    }

    @Test
    void testAdd() {
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withNetworkElementAndKey(1., injectionId1)
                .withNetworkElementAndKey(-1., injectionId2, injectionName2)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertEquals("id1", injectionRangeAction.getName());
        assertEquals("BE", injectionRangeAction.getOperator());
        assertTrue(injectionRangeAction.getGroupId().isPresent());
        assertEquals("groupId1", injectionRangeAction.getGroupId().get());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(1, injectionRangeAction.getUsageRules().size());

        assertEquals(2, injectionRangeAction.getInjectionDistributionKeys().size());
        assertEquals(1., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId1)), 1e-6);
        assertEquals(-1., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId2)), 1e-6);

        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(injectionId1));
        assertNotNull(crac.getNetworkElement(injectionId2));

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithSumOnSameInjection() {
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withGroupId("groupId1")
                .withNetworkElementAndKey(1., injectionId1)
                .withNetworkElementAndKey(4., injectionId1)
                .withNetworkElementAndKey(-1., injectionId2, injectionName2)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertEquals("id1", injectionRangeAction.getName());
        assertEquals("BE", injectionRangeAction.getOperator());
        assertTrue(injectionRangeAction.getGroupId().isPresent());
        assertEquals("groupId1", injectionRangeAction.getGroupId().get());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(1, injectionRangeAction.getUsageRules().size());

        assertEquals(2, injectionRangeAction.getInjectionDistributionKeys().size());
        assertEquals(5., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId1)), 1e-6);
        assertEquals(-1., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId2)), 1e-6);

        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(injectionId1));
        assertNotNull(crac.getNetworkElement(injectionId2));

        assertEquals(1, crac.getRangeActions().size());

    }

    @Test
    void testAddWithoutGroupId() {
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElementAndKey(1., injectionId1)
                .withNetworkElementAndKey(-1., injectionId2, injectionName2)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertEquals("BE", injectionRangeAction.getOperator());
        assertTrue(injectionRangeAction.getGroupId().isEmpty());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(1, injectionRangeAction.getUsageRules().size());
        assertEquals(2, injectionRangeAction.getInjectionDistributionKeys().size());

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(2, crac.getNetworkElements().size());
    }

    @Test
    void testAddDefaultKey() {
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withGroupId("groupId1")
            .withNetworkElement(injectionId1)
            .withNetworkElement(injectionId2, injectionName2)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
            .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertEquals("id1", injectionRangeAction.getName());
        assertEquals("BE", injectionRangeAction.getOperator());
        assertTrue(injectionRangeAction.getGroupId().isPresent());
        assertEquals("groupId1", injectionRangeAction.getGroupId().get());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(1, injectionRangeAction.getUsageRules().size());

        assertEquals(2, injectionRangeAction.getInjectionDistributionKeys().size());
        assertEquals(1., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId1)), 1e-6);
        assertEquals(1., injectionRangeAction.getInjectionDistributionKeys().get(crac.getNetworkElement(injectionId2)), 1e-6);

        assertEquals(2, crac.getNetworkElements().size());
        assertNotNull(crac.getNetworkElement(injectionId1));
        assertNotNull(crac.getNetworkElement(injectionId2));

        assertEquals(1, crac.getRangeActions().size());
    }

    @Test
    void testAddWithoutUsageRule() {
        /*
        This behaviour is considered admissible:
            - without usage rule, the remedial action will never be available

        This test should however warnings
         */
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
                .withId("id1")
                .withOperator("BE")
                .withNetworkElementAndKey(1., injectionId1)
                .newRange().withMin(-5).withMax(10).add()
                .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertEquals("BE", injectionRangeAction.getOperator());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(0, injectionRangeAction.getUsageRules().size());
        assertEquals(1, injectionRangeAction.getInjectionDistributionKeys().size());

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(1, crac.getNetworkElements().size());
    }

    @Test
    void testAddWithoutOperator() {
        InjectionRangeAction injectionRangeAction = crac.newInjectionRangeAction()
                .withId("id1")
                .withGroupId("groupId1")
                .withNetworkElementAndKey(1., injectionId1)
                .withNetworkElementAndKey(-1., injectionId2, injectionName2)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();

        assertEquals("id1", injectionRangeAction.getId());
        assertNull(injectionRangeAction.getOperator());
        assertEquals(1, injectionRangeAction.getRanges().size());
        assertEquals(1, injectionRangeAction.getUsageRules().size());
        assertEquals(2, injectionRangeAction.getInjectionDistributionKeys().size());

        assertEquals(1, crac.getRangeActions().size());
        assertEquals(2, crac.getNetworkElements().size());
    }

    @Test
    void testNoIdFail() {
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withOperator("BE")
            .withGroupId("groupId1")
            .withNetworkElementAndKey(1., injectionId1)
            .withNetworkElementAndKey(-1., injectionId2, injectionName2)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, injectionRangeActionAdder::add);
        assertEquals("Cannot add a InjectionRangeAction object with no specified id. Please use withId()", exception.getMessage());
    }

    @Test
    void testNoNetworkElementFail() {
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withGroupId("groupId1")
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, injectionRangeActionAdder::add);
        assertEquals("Cannot add InjectionRangeAction without a injection distribution key. Please use withNetworkElementAndKey()", exception.getMessage());
    }

    @Test
    void testNoRangeFail() {
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId("id1")
            .withOperator("BE")
            .withNetworkElementAndKey(1., injectionId1)
            .withNetworkElementAndKey(-1., injectionId2, injectionName2)
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, injectionRangeActionAdder::add);
        assertEquals("Cannot add InjectionRangeAction without a range. Please use newRange()", exception.getMessage());
    }

    @Test
    void testIdNotUnique() {
        crac.newInjectionRangeAction()
                .withId("sameId")
                .withNetworkElementAndKey(1., injectionId1)
                .newRange().withMin(-5).withMax(10).add()
                .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add()
                .add();
        InjectionRangeActionAdder injectionRangeActionAdder = crac.newInjectionRangeAction()
            .withId("sameId")
            .withNetworkElementAndKey(1., injectionId1)
            .newRange().withMin(-5).withMax(10).add()
            .newOnInstantUsageRule().withInstant(PREVENTIVE_INSTANT_ID).withUsageMethod(UsageMethod.AVAILABLE).add();
        OpenRaoException exception = assertThrows(OpenRaoException.class, injectionRangeActionAdder::add);
        assertEquals("A remedial action with id sameId already exists", exception.getMessage());
    }
}
