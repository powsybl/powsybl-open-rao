/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class NativeLibraryLoaderTest {

    @Test
    void shouldFailWhenLibraryNotFound() {
        FaraoException exception = assertThrows(FaraoException.class, () -> NativeLibraryLoader.loadNativeLibrary("thisLibraryWillNeverExist"));
        assertEquals("Failed to load library 'thisLibraryWillNeverExist'", exception.getMessage());
    }
}
