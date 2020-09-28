/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.commons;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({UUID.class, RandomizedString.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class RandomizedStringTest {
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
        PowerMockito.mockStatic(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuid, otherUuid);

        String generatedString = RandomizedString.getRandomizedString("TEST_", Collections.singletonList("TEST_" + uuid));
        assertEquals("TEST_" + otherUuid, generatedString);
    }

    @Test(expected = FaraoException.class)
    public void generateStringFailsIfNotEnoughTries() {
        UUID uuid = UUID.fromString("2937ed60-9511-11ea-bb37-0242ac130002");
        UUID otherUuid = UUID.fromString("622fc1d6-41ba-43bc-9c54-c11073fc2ce7");
        PowerMockito.mockStatic(UUID.class);
        PowerMockito.when(UUID.randomUUID()).thenReturn(uuid, otherUuid);

        RandomizedString.getRandomizedString("RANDOMIZED_STRING_", Collections.singletonList("RANDOMIZED_STRING_" + uuid), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exceptionIfInvalidMaxTry() {
        RandomizedString.getRandomizedString("", Collections.emptyList(), 0);
    }
}
