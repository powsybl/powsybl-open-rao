/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class RandomizedStringTest {
    private static MockedStatic<UUID> uuidMock;

    @BeforeAll
    public static void setUpIndividual() {
        uuidMock = mockStatic(UUID.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterAll
    public static void cleanUp() {
        uuidMock.close();
    }

    @Test
    public void generateRandomString() {
        String generatedString = RandomizedString.getRandomizedString();
        assertNotNull(generatedString);
        assertFalse(generatedString.isEmpty());

        generatedString = RandomizedString.getRandomizedString("Custom prefix ");
        assertTrue(generatedString.startsWith("Custom prefix "));
        assertNotEquals("Custom prefix ", generatedString);

        generatedString = RandomizedString.getRandomizedString(Collections.emptyList());
        assertNotNull(generatedString);
        assertFalse(generatedString.isEmpty());
    }

    @Test
    public void generateStringDifferentToInvalidOnes() {
        UUID uuid = UUID.fromString("2937ed60-9511-11ea-bb37-0242ac130002");
        UUID otherUuid = UUID.fromString("622fc1d6-41ba-43bc-9c54-c11073fc2ce7");
        uuidMock.when(() -> UUID.randomUUID()).thenReturn(uuid, otherUuid);

        String generatedString = RandomizedString.getRandomizedString("TEST_", Collections.singletonList("TEST_" + uuid));
        assertEquals("TEST_" + otherUuid, generatedString);
    }

    @Test
    public void generateStringFailsIfNotEnoughTries() {
        UUID uuid = UUID.fromString("2937ed60-9511-11ea-bb37-0242ac130002");
        UUID otherUuid = UUID.fromString("622fc1d6-41ba-43bc-9c54-c11073fc2ce7");
        uuidMock.when(UUID::randomUUID).thenReturn(uuid, otherUuid);

        assertThrows(FaraoException.class, () -> RandomizedString.getRandomizedString("RANDOMIZED_STRING_", Collections.singletonList("RANDOMIZED_STRING_" + uuid), 1));
    }

    @Test
    public void exceptionIfInvalidMaxTry() {
        assertThrows(IllegalArgumentException.class, () -> RandomizedString.getRandomizedString("", Collections.emptyList(), 0));
    }
}
