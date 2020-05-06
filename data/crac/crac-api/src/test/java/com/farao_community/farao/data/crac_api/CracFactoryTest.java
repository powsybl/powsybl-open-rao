package com.farao_community.farao.data.crac_api;

import com.farao_community.farao.commons.FaraoException;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;

/**
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
public class CracFactoryTest {

    private FileSystem fileSystem;

    private InMemoryPlatformConfig platformConfig;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test(expected = FaraoException.class)
    public void mustThrowIfNoImplem1() {
        CracFactory.find("SimpleCracFactory");
    }

    @Test(expected = FaraoException.class)
    public void mustThrowIfNoImplem2() {
        CracFactory.findDefault();
    }
}
