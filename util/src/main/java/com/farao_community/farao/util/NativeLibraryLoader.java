/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NativeLibraryLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeLibraryLoader.class);
    private static volatile Set<String> nativeLibrariesLoaded = Collections.synchronizedSet(new HashSet<>());

    private NativeLibraryLoader() {
    }

    private static synchronized boolean alreadyLoaded(String libraryName) {
        return nativeLibrariesLoaded.contains(libraryName);
    }

    public static synchronized void loadNativeLibrary(String libraryName) {
        if (!alreadyLoaded(libraryName)) {
            try {
                LOGGER.info("Loading library '{}'", libraryName);
                System.loadLibrary(libraryName);
                nativeLibrariesLoaded.add(libraryName);
            } catch (UnsatisfiedLinkError e) {
                LOGGER.error("Failed to load library '{}'", libraryName);
                throw new FaraoException(String.format("Failed to load library '%s'", libraryName));
            }
        }
    }
}
