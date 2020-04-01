package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class NativeLibraryLoaderTest {
    @Test(expected = FaraoException.class)
    public void shouldFailToLoadInvalidLibrary() {
        NativeLibraryLoader.loadNativeLibrary("thisLibraryWillNeverExist");
    }
}
