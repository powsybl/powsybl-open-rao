package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public final class NativeLibraryLoader {

    private static volatile boolean nativeLibrariesLoaded = false;

    private NativeLibraryLoader() {
    }

    public static synchronized void loadNativeLibraries() {
        if (nativeLibrariesLoaded) {
            return;
        }

        try {
            if (nativeLibrariesLoaded) {
                System.loadLibrary("jniortools");
                nativeLibrariesLoaded = true;
            }
        } catch (UnsatisfiedLinkError e) {
            throw new FaraoException("Coucou");
        }
    }
}
