package com.farao_community.farao.util;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FaraoNetworkPoolTest {
    @Test
    public void networkPoolUsageTest() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        String initialVariant = network.getVariantManager().getWorkingVariantId();
        String otherVariant = "otherVariant";
        network.getVariantManager().cloneVariant(initialVariant, otherVariant);

        try (FaraoNetworkPool pool = new FaraoNetworkPool(network, otherVariant, 10)) {
            Network networkCopy = pool.getAvailableNetwork();

            assertNotNull(networkCopy);
            assertNotEquals(network, networkCopy);
            assertTrue(networkCopy.getVariantManager().getWorkingVariantId().startsWith("FaraoNetworkPool working variant"));

            pool.releaseUsedNetwork(networkCopy);
        } catch (InterruptedException e) {
            fail();
            Thread.currentThread().interrupt();
        }
    }
}
