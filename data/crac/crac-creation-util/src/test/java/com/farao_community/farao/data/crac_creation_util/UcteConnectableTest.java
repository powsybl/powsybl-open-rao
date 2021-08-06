/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_creation_util;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class UcteConnectableTest {

    /*
    @Test
    public void testBranchMatchResultConstructor() {
        assertEquals(MATCHED_ON_SIDE_ONE, UcteConnectable.MatchResult.getMatchedResult(false, Branch.Side.ONE));
        assertEquals(MATCHED_ON_SIDE_TWO, UcteConnectable.MatchResult.getMatchedResult(false, Branch.Side.TWO));
        assertEquals(INVERTED_ON_SIDE_ONE, UcteConnectable.MatchResult.getMatchedResult(true, Branch.Side.ONE));
        assertEquals(INVERTED_ON_SIDE_TWO, UcteConnectable.MatchResult.getMatchedResult(true, Branch.Side.TWO));
    }

    @Test
    public void testBranchMatchResultGetters() {
        UcteConnectable.MatchResult matchResult = NOT_MATCHED;
        assertFalse(matchResult.matched());
        assertFalse(matchResult.isInverted());
        assertNull(matchResult.getSide());

        matchResult = MATCHED_ON_SIDE_ONE;
        assertTrue(matchResult.matched());
        assertFalse(matchResult.isInverted());
        assertEquals(Branch.Side.ONE, matchResult.getSide());

        matchResult = MATCHED_ON_SIDE_TWO;
        assertTrue(matchResult.matched());
        assertFalse(matchResult.isInverted());
        assertEquals(Branch.Side.TWO, matchResult.getSide());

        matchResult = INVERTED_ON_SIDE_ONE;
        assertTrue(matchResult.matched());
        assertTrue(matchResult.isInverted());
        assertEquals(Branch.Side.ONE, matchResult.getSide());

        matchResult = INVERTED_ON_SIDE_TWO;
        assertTrue(matchResult.matched());
        assertTrue(matchResult.isInverted());
        assertEquals(Branch.Side.TWO, matchResult.getSide());
    }

    @Test
    public void testMatch() {
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class);
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC12345", "DEF12345", "1", false));
        assertEquals(INVERTED_ON_SIDE_ONE, ucteElement.tryMatching("DEF12345", "ABC12345", "en1", false));
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC1234*", "DEF12345", "en2", false));
        assertEquals(INVERTED_ON_SIDE_ONE, ucteElement.tryMatching("DEF1234*", "ABC12345", "1", false));
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC123", "DEF12345", "en2", true));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC123", "DEF12345", "1", false));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC12345", "DEF12345", "en11", false));
    }

    @Test
    public void testMatchWithSide() {
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC12345", "DEF12345", "1", false));
        assertEquals(INVERTED_ON_SIDE_ONE, ucteElement.tryMatching("DEF12345", "ABC12345", "en1", false));
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC1234*", "DEF12345", "en2", false));
        assertEquals(INVERTED_ON_SIDE_ONE, ucteElement.tryMatching("DEF1234*", "ABC12345", "1", false));
        assertEquals(MATCHED_ON_SIDE_ONE, ucteElement.tryMatching("ABC123", "DEF12345", "en2", true));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC123", "DEF12345", "1", false));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC12345", "DEF12345", "en11", false));

        ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.TWO);
        assertEquals(MATCHED_ON_SIDE_TWO, ucteElement.tryMatching("ABC12345", "DEF12345", "1", false));
        assertEquals(INVERTED_ON_SIDE_TWO, ucteElement.tryMatching("DEF12345", "ABC12345", "en1", false));
        assertEquals(MATCHED_ON_SIDE_TWO, ucteElement.tryMatching("ABC1234*", "DEF12345", "en2", false));
        assertEquals(INVERTED_ON_SIDE_TWO, ucteElement.tryMatching("DEF1234*", "ABC12345", "1", false));
        assertEquals(MATCHED_ON_SIDE_TWO, ucteElement.tryMatching("ABC123", "DEF12345", "en2", true));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC123", "DEF12345", "1", false));
        assertEquals(NOT_MATCHED, ucteElement.tryMatching("ABC12345", "DEF12345", "en11", false));
    }

    @Test
    public void testEquals() {
        UcteConnectable ucteElement1 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        UcteConnectable ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        assertEquals(ucteElement1, ucteElement1);
        assertEquals(ucteElement1, ucteElement2);
        assertNotEquals(ucteElement1, null);

        // different from
        ucteElement2 = new UcteConnectable("ABC1234_", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        assertNotEquals(ucteElement1, ucteElement2);

        // different to
        ucteElement2 = new UcteConnectable("ABC12345", "DEF1234_", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        assertNotEquals(ucteElement1, ucteElement2);

        // different order code
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "2", Set.of("en1", "en2"), Branch.class, Branch.Side.ONE);
        assertNotEquals(ucteElement1, ucteElement2);

        // different suffixes
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en11", "en2"), Branch.class, Branch.Side.ONE);
        assertEquals(ucteElement1, ucteElement2);

        // different type
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Switch.class, Branch.Side.ONE);
        assertNotEquals(ucteElement1, ucteElement2);

        // different side
        ucteElement2 = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Branch.class, Branch.Side.TWO);
        assertNotEquals(ucteElement1, ucteElement2);
    }

    @Test
    public void testToString() {
        UcteConnectable ucteElement = new UcteConnectable("ABC12345", "DEF12345", "1", Set.of("en1", "en2"), Switch.class, Branch.Side.ONE);
        assertEquals("ABC12345 DEF12345 1 - Switch - side ONE", ucteElement.toString());
    }

    @Test
    public void testConstructorException() {
        Set<String> suffixes = Set.of("en1");
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC1234", "DEF12345", "1", suffixes, Branch.class, Branch.Side.ONE));
        assertThrows(IllegalArgumentException.class, () -> new UcteConnectable("ABC12345", "DEF1234", "1", suffixes, Branch.class, Branch.Side.ONE));
    }

     */
}
