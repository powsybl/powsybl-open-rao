/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class NativeLibraryLoaderTest {

    @Test(expected = FaraoException.class)
    public void shouldFailWhenLibraryNotFound() {
        NativeLibraryLoader.loadNativeLibrary("thisLibraryWillNeverExist");
    }
}
