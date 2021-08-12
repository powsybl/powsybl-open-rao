/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TieLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteConnectableTest {

    private Identifiable<?> branch;

    @Before
    public void setUp() {
        branch = Mockito.mock(Branch.class);
        Mockito.when(branch.getId()).thenReturn("iidmBranchId");
    }

    @Test
    public void testMatch() {
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);

        // found with order code, good direction
        assertTrue(ucteElement.doesMatch("ABC12345", "DEF12345", "1"));
        assertTrue(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "1").hasMatched());
        assertEquals(UcteConnectable.Side.BOTH, ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "1").getSide());

        // found with element name, inverted direction
        assertTrue(ucteElement.doesMatch("ABC12345", "DEF12345", "en1"));
        assertTrue(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "en1").hasMatched());

        // id with wildcard
        assertTrue(ucteElement.doesMatch("ABC1234*", "DEF12345", "en2"));
        assertTrue(ucteElement.getUcteMatchingResult("ABC1234*", "DEF12345", "en2").hasMatched());

        // not found
        assertFalse(ucteElement.doesMatch("ABC123 ", "DEF12345", "1"));
        assertFalse(ucteElement.getUcteMatchingResult("ABC123 ", "DEF12345", "1").hasMatched());

        assertFalse(ucteElement.doesMatch("ABC12345", "DEF12345", "unknown1"));
        assertFalse(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "unknown1").hasMatched());

        assertFalse(ucteElement.doesMatch("DEF12345", "ABC12345", "1"));
        assertFalse(ucteElement.getUcteMatchingResult("DEF12345", "ABC12345", "1").hasMatched());
    }

    @Test
    public void testMatchWithSide() {

        Identifiable<?> tieLine = Mockito.mock(TieLine.class);

        UcteConnectable ucteElement = new UcteConnectable("X_NODE12", "R_NODE12", "1", Set.of("en1", "en2"), tieLine, false, UcteConnectable.Side.ONE);

        // found with element name, inverted direction
        assertTrue(ucteElement.doesMatch("X_NODE12", "R_NODE12", "en2"));
        assertTrue(ucteElement.getUcteMatchingResult("X_NODE12", "R_NODE12", "en2").hasMatched());
        assertEquals(UcteConnectable.Side.ONE, ucteElement.getUcteMatchingResult("X_NODE12", "R_NODE12", "en2").getSide());

        ucteElement = new UcteConnectable("X_NODE12", "R_NODE12", "1", Set.of("en1", "en2"), tieLine, false, UcteConnectable.Side.TWO);

        // id with wildcard
        assertTrue(ucteElement.doesMatch("X_NODE1*", "R_NODE1*", "1"));
        assertTrue(ucteElement.getUcteMatchingResult("X_NODE1*", "R_NODE1*", "1").hasMatched());
        assertEquals(UcteConnectable.Side.TWO, ucteElement.getUcteMatchingResult("X_NODE1*", "R_NODE1*", "1").getSide());
    }

    @Test
    public void testInversionInConvention() {

        UcteConnectable ucteBranch = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);

        // found with element name, inverted direction
        assertTrue(ucteBranch.getUcteMatchingResult("ABC12345", "DEF12345", "1").hasMatched());
        assertFalse(ucteBranch.getUcteMatchingResult("ABC12345", "DEF12345", "1").isInverted());

        UcteConnectable ucteTransfo = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, true);

        // found with element name, inverted direction
        assertTrue(ucteTransfo.getUcteMatchingResult("ABC12345", "DEF12345", "1").hasMatched());
        assertTrue(ucteTransfo.getUcteMatchingResult("ABC12345", "DEF12345", "1").isInverted());
    }

    @Test
    public void testEquals() {

        Identifiable<?> branch = Mockito.mock(Branch.class);
        Mockito.when(branch.getId()).thenReturn("iidmBranchId");

        UcteConnectable ucteElement1 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);
        UcteConnectable ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);
        assertEquals(ucteElement1, ucteElement1);
        assertEquals(ucteElement1, ucteElement2);
        assertNotEquals(ucteElement1, null);

        // different from
        ucteElement2 = new UcteConnectable("DIF12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);
        assertNotEquals(ucteElement1, ucteElement2);

        // different to
        ucteElement2 = new UcteConnectable("ABC12345", "XXX12345", "1", Set.of("en1", "en2"), branch, false);
        assertNotEquals(ucteElement1, ucteElement2);

        // different order code
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "3", Set.of("en1", "en2"), branch, false);
        assertNotEquals(ucteElement1, ucteElement2);

        // different suffixes
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en5"), branch, false);
        assertNotEquals(ucteElement1, ucteElement2);

        // different iidm identifiable
        Identifiable<?> branch2 = Mockito.mock(Branch.class);
        Mockito.when(branch2.getId()).thenReturn("anotherIidmBranchId");
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch2, false);
        assertNotEquals(ucteElement1, ucteElement2);

        // different side
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false, UcteConnectable.Side.TWO);
        assertNotEquals(ucteElement1, ucteElement2);
    }

    @Test
    public void testToString() {
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false, UcteConnectable.Side.ONE);
        assertEquals("ABC12345 DEF12345 1 - iidmBranchId - side ONE", ucteElement.toString());
    }

    @Test
    public void testConstructorException() {
        Set<String> suffixes = Set.of("en1");
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC1234", "DEF12345", "1", suffixes, branch, false));
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC12345", "DEF1234", "1", suffixes, branch, false));
    }
}
