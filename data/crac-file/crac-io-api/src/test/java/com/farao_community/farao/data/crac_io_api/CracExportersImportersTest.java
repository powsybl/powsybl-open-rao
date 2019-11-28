package com.farao_community.farao.data.crac_io_api;

import com.farao_community.farao.data.crac_api.Crac;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracExportersImportersTest {

    @Test
    public void testExport() {
        Crac crac = Mockito.mock(Crac.class);
        CracExporters.exportCrac(crac, Mockito.anyString(), Paths.get(getClass().getResource("/empty.txt").getFile()));
    }

    @Test
    public void testImport() {
        CracImporters.importCrac(Paths.get(getClass().getResource("/empty.txt").getFile()));
    }
}
