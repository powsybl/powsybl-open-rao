/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.crac.impl;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.crac.api.ContingencyAdder;
import com.powsybl.openrao.data.crac.api.Crac;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class ContingencyAdderImplTest {

    private Crac crac;

    @BeforeEach
    public void setUp() {
        crac = new CracImplFactory().create("test-crac");
    }

    @Test
    void testAddContingencies() {
        Contingency con1 = crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withContingencyElement("neId1", ContingencyElementType.SWITCH)
                .add();
        Contingency con2 = crac.newContingency()
                .withContingencyElement("neId2-1", ContingencyElementType.THREE_WINDINGS_TRANSFORMER)
                .withContingencyElement("neId2-2", ContingencyElementType.TWO_WINDINGS_TRANSFORMER)
                .withId("conId2")
                .add();
        assertEquals(2, crac.getContingencies().size());

        // Verify 1st contingency content
        assertEquals("conName1", crac.getContingency("conId1").getName().get());
        assertEquals(1, crac.getContingency("conId1").getElements().size());
        assertEquals("neId1", crac.getContingency("conId1").getElements().iterator().next().getId());
        assertEquals(con1.getId(), crac.getContingency("conId1").getId());

        // Verify 2nd contingency content
        assertEquals("conId2", crac.getContingency("conId2").getName().get());
        assertEquals(2, crac.getContingency("conId2").getElements().size());
        assertEquals(con2.getId(), crac.getContingency("conId2").getId());
        Iterator<ContingencyElement> iter = crac.getContingency("conId2").getElements().iterator();
        ContingencyElement ne1 = iter.next();
        ContingencyElement ne2 = iter.next();
        // Order the network elements from the Set
        if (ne2.getId().compareTo(ne1.getId()) < 0) {
            ContingencyElement tmp = ne2;
            ne2 = ne1;
            ne1 = tmp;
        }
        assertEquals("neId2-1", ne1.getId());
        assertEquals("neId2-2", ne2.getId());
    }

    @Test
    void testAddWithNoIdFail() {
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BUS);
        assertThrows(OpenRaoException.class, contingencyAdder::add);
    }

    @Test
    void testNullParentFail() {
        assertThrows(NullPointerException.class, () -> new ContingencyAdderImpl(null));
    }

    @Test
    void testAddEmptyContingency() {
        crac.newContingency().withId("cont").add();
        assertEquals(1, crac.getContingencies().size());
        assertNotNull(crac.getContingency("cont"));
        assertEquals(0, crac.getContingency("cont").getElements().size());
    }

    @Test
    void testAddExistingSameContingency() {
        Contingency contingency1 = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .add();
        assertSame(contingency1, contingency2);
    }

    @Test
    void testAddExistingSameContingencyOrderOfAddChanged() {
        Contingency contingency1 = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withName("conName1")
            .withId("conId1")
            .add();
        assertSame(contingency1, contingency2);
    }

    @Test
    void testAddExistingSameContingencyDuplicatedElements() {
        Contingency contingency1 = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .add();
        Contingency contingency2 = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .withContingencyElement("neId1", ContingencyElementType.BATTERY)
            .withContingencyElement("neId2", ContingencyElementType.DANGLING_LINE)
            .add();
        assertSame(contingency1, contingency2);
    }

    @Test
    void testAddExistingContingencyDifferentElementId() {
        crac.newContingency()
                .withId("conId1")
                .withName("conName1")
                .withContingencyElement("neId1", ContingencyElementType.LOAD)
                .add();
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId2", ContingencyElementType.LOAD);
        assertThrows(OpenRaoException.class, contingencyAdder::add);
    }

    @Test
    void testAddExistingContingencyDifferentElementType() {
        crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.LOAD)
            .add();
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.SHUNT_COMPENSATOR);
        assertThrows(OpenRaoException.class, contingencyAdder::add);
    }

    @Test
    void testAddExistingContingencyDifferentName() {
        crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.LOAD)
            .add();
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId("conId1")
            .withName("conName2")
            .withContingencyElement("neId1", ContingencyElementType.LOAD);
        assertThrows(OpenRaoException.class, contingencyAdder::add);
    }

    @Test
    void testAddExistingContingencyDifferentName2() {
        crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.LOAD)
            .add();
        ContingencyAdder contingencyAdder = crac.newContingency()
            .withId("conId1")
            .withContingencyElement("neId1", ContingencyElementType.LOAD);
        assertThrows(OpenRaoException.class, contingencyAdder::add);
    }

    @Test
    void testDifferentTypeOfContingencyElement() {
        crac.newContingency()
            .withId("conId1")
            .withName("conName1")
            .withContingencyElement("neId1", ContingencyElementType.BRANCH)
            .withContingencyElement("neId2", ContingencyElementType.GENERATOR)
            .withContingencyElement("neId3", ContingencyElementType.STATIC_VAR_COMPENSATOR)
            .withContingencyElement("neId4", ContingencyElementType.SHUNT_COMPENSATOR)
            .withContingencyElement("neId5", ContingencyElementType.HVDC_LINE)
            .withContingencyElement("neId6", ContingencyElementType.BUSBAR_SECTION)
            .withContingencyElement("neId7", ContingencyElementType.DANGLING_LINE)
            .withContingencyElement("neId8", ContingencyElementType.LINE)
            .withContingencyElement("neId10", ContingencyElementType.TWO_WINDINGS_TRANSFORMER)
            .withContingencyElement("neId11", ContingencyElementType.THREE_WINDINGS_TRANSFORMER)
            .withContingencyElement("neId12", ContingencyElementType.LOAD)
            .withContingencyElement("neId13", ContingencyElementType.SWITCH)
            .withContingencyElement("neId14", ContingencyElementType.BATTERY)
            .withContingencyElement("neId15", ContingencyElementType.BUS)
            .withContingencyElement("neId16", ContingencyElementType.TIE_LINE)
            .add();
        assertEquals(15, crac.getContingency("conId1").getElements().size());
    }
}
