/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.searchtreerao.linearoptimisation.algorithms.linearproblem;

import com.google.ortools.Loader;

import static com.powsybl.openrao.commons.logs.OpenRaoLoggerProvider.TECHNICAL_LOGS;

/**
 * @author Daniel THIRION {@literal <daniel.thirion at rte-france.com>}
 */
public final class LibraryLoader {

    private LibraryLoader() {
        //Private constructor
    }

    /**
     * If currentThread has been interrupted, loading of libraries cannot be undertaken as VM will block
     * required operations, such as directory initialization. To avoid this it is encapsulated in a new thread.
     */
    static void loadLibrary() {
        final Thread loaderThread = new Thread(() -> {
            try {
                Loader.loadNativeLibraries();
            } catch (final Exception e) { //NOSONAR
                TECHNICAL_LOGS.error("Native library jniortools could not be loaded. You can ignore this message if it is not needed.");
            }
        });
        loaderThread.start();
        joinWithInterruptionHandling(loaderThread);
    }

    /**
     * If outer thread was interrupted, this new thread will exit with an InterruptedException,
     * this is a false positive since lib was correctly loaded.
     *
     * @param loaderThread
     */
    private static void joinWithInterruptionHandling(final Thread loaderThread) {
        try {
            loaderThread.join();
        } catch (final InterruptedException e) { //NOSONAR
            TECHNICAL_LOGS.info("Loading of jniortools has been interrupted but lib was correctly loaded.");
        }
    }
}
