package com.farao_community.farao.util;

import com.farao_community.farao.commons.FaraoException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(NativeLibraryLoader.class)
public class NativeLibraryLoaderTest {

    @Test(expected = FaraoException.class)
    public void shouldFailWhenLibraryNotFound() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.doThrow(new UnsatisfiedLinkError()).when(System.class);
        System.loadLibrary("thisLibraryWillNeverExist");

        NativeLibraryLoader.loadNativeLibrary("thisLibraryWillNeverExist");
    }

    @Test
    public void shouldSucceedWhenLibraryFound() {
        PowerMockito.mockStatic(System.class);
        PowerMockito.doNothing().when(System.class);
        System.loadLibrary("thisLibraryExists");

        NativeLibraryLoader.loadNativeLibrary("thisLibraryExists");
    }
}
