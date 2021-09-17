/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util.ucte;

import com.powsybl.iidm.network.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Set;

import static com.farao_community.farao.data.crac_creation_util.ConnectableType.*;
import static org.junit.Assert.*;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteConnectableTest {

    @Test
    public void testInternalLineWithOrderCode() {
        Branch<?> branch = Mockito.mock(Branch.class);
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false);

        // found internal_line with order code
        assertTrue(ucteElement.doesMatch("ABC12345", "DEF12345", "1", INTERNAL_LINE));
        assertTrue(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "1", INTERNAL_LINE).hasMatched());
        assertEquals(UcteConnectable.Side.BOTH, ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "1", INTERNAL_LINE).getSide());

        // not found, wrong from id
        assertFalse(ucteElement.doesMatch("ABC123 ", "DEF12345", "1", INTERNAL_LINE));
        assertFalse(ucteElement.getUcteMatchingResult("ABC123 ", "DEF12345", "1", INTERNAL_LINE).hasMatched());

        // not found, wrong type
        assertFalse(ucteElement.doesMatch("ABC12345", "DEF12345", "1", PST));
        assertFalse(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "1", PST).hasMatched());
    }

    @Test
    public void testTieLineWithElementName() {
        TieLine tieLine = Mockito.mock(TieLine.class);
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), tieLine, false);

        // found tie_line with element name
        assertTrue(ucteElement.doesMatch("ABC12345", "DEF12345", "en1", TIE_LINE, INTERNAL_LINE));
        assertTrue(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "en1", TIE_LINE).hasMatched());

        // not found, wrong element name
        assertFalse(ucteElement.doesMatch("ABC12345", "DEF12345", "unknown1", TIE_LINE));
        assertFalse(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "unknown1", TIE_LINE).hasMatched());

        // not found, wrong type
        assertFalse(ucteElement.doesMatch("ABC12345", "DEF12345", "unknown1", VOLTAGE_TRANSFORMER, PST));
        assertFalse(ucteElement.getUcteMatchingResult("ABC12345", "DEF12345", "unknown1", VOLTAGE_TRANSFORMER, PST).hasMatched());
    }

    @Test
    public void testPstWithWildcards() {
        TwoWindingsTransformer pst = Mockito.mock(TwoWindingsTransformer.class);
        Mockito.when(pst.getPhaseTapChanger()).thenReturn(Mockito.mock(PhaseTapChanger.class));

        // found transformer with id with wildcard
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), pst, false);

        assertTrue(ucteElement.doesMatch("ABC1234*", "DEF12345", "en2", VOLTAGE_TRANSFORMER, PST));
        assertTrue(ucteElement.getUcteMatchingResult("ABC1234*", "DEF12345", "en2", PST).hasMatched());

        // not found, from/to inverted
        assertFalse(ucteElement.doesMatch("DEF12345", "ABC12345", "1", PST));
        assertFalse(ucteElement.getUcteMatchingResult("DEF12345", "ABC12345", "1", PST).hasMatched());
    }

    @Test
    public void testMatchWithSide() {

        TieLine tieLine = Mockito.mock(TieLine.class);
        UcteConnectable ucteElement = new UcteConnectable("X_NODE12", "R_NODE12", "1", Set.of("en1", "en2"), tieLine, false, UcteConnectable.Side.ONE);

        // found with element name
        assertTrue(ucteElement.doesMatch("X_NODE12", "R_NODE12", "en2", TIE_LINE));
        assertTrue(ucteElement.getUcteMatchingResult("X_NODE12", "R_NODE12", "en2", TIE_LINE).hasMatched());
        assertEquals(UcteConnectable.Side.ONE, ucteElement.getUcteMatchingResult("X_NODE12", "R_NODE12", "en2", TIE_LINE).getSide());

        ucteElement = new UcteConnectable("X_NODE12", "R_NODE12", "1", Set.of("en1", "en2"), tieLine, false, UcteConnectable.Side.TWO);

        // id with wildcard
        assertTrue(ucteElement.doesMatch("X_NODE1*", "R_NODE1*", "1", TIE_LINE));
        assertTrue(ucteElement.getUcteMatchingResult("X_NODE1*", "R_NODE1*", "1", TIE_LINE).hasMatched());
        assertEquals(UcteConnectable.Side.TWO, ucteElement.getUcteMatchingResult("X_NODE1*", "R_NODE1*", "1", TIE_LINE).getSide());
    }

    @Test
    public void testInversionInConvention() {

        TwoWindingsTransformer transformer = Mockito.mock(TwoWindingsTransformer.class);
        Mockito.when(transformer.getPhaseTapChanger()).thenReturn(null);

        UcteConnectable ucteTransfo = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), transformer, true);

        // found with element name, inverted direction
        assertTrue(ucteTransfo.getUcteMatchingResult("ABC12345", "DEF12345", "1", VOLTAGE_TRANSFORMER).hasMatched());
        assertTrue(ucteTransfo.getUcteMatchingResult("ABC12345", "DEF12345", "1", VOLTAGE_TRANSFORMER).isInverted());
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
        Branch<?> branch = Mockito.mock(Branch.class);
        Mockito.when(branch.getId()).thenReturn("iidmBranchId");

        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), branch, false, UcteConnectable.Side.ONE);
        assertEquals("ABC12345 DEF12345 1 - iidmBranchId - side ONE", ucteElement.toString());
    }

    @Test
    public void testConstructorException() {
        Branch<?> branch = Mockito.mock(Branch.class);
        Mockito.when(branch.getId()).thenReturn("iidmBranchId");

        Set<String> suffixes = Set.of("en1");
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC1234", "DEF12345", "1", suffixes, branch, false));
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC12345", "DEF1234", "1", suffixes, branch, false));
    }

}
