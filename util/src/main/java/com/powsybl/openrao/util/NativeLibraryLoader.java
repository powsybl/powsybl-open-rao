/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.util;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NativeLibraryLoader {
    private static final Set<String> NATIVE_LIBRARIES_LOADED = Collections.synchronizedSet(new HashSet<>());

    private NativeLibraryLoader() {
    }

    private static synchronized boolean alreadyLoaded(String libraryName) {
        return NATIVE_LIBRARIES_LOADED.contains(libraryName);
    }

    public static synchronized void loadNativeLibrary(String libraryName) {
        if (!alreadyLoaded(libraryName)) {
            try {
                OpenRaoLoggerProvider.TECHNICAL_LOGS.info("Loading library '{}'", libraryName);
                System.loadLibrary(libraryName);
                NATIVE_LIBRARIES_LOADED.add(libraryName);
            } catch (UnsatisfiedLinkError e) {
                throw new OpenRaoException(String.format("Failed to load library '%s'", libraryName), e);
            }
        }
    }
}
